import { useCallback, useEffect, useState } from 'react'
import { useSearchParams } from 'react-router'
import { pedir } from '../../api/cliente'
import { obtenerAdaptaciones, type Adaptacion } from '../recetario/adaptaciones'
import { listarRecetas, obtenerReceta } from '../recetario/apiRecetario'
import { analizarReceta, cargarCocina, type Cocina, type RecetaVista } from '../recetario/cocina'
import { costoPlan, obtenerPlan } from './apiPlanificador'
import { esSinConexion, sincronizar } from './sinConexion'
import type { Receta, RecetaDetalle } from '../recetario/tipos'
import { esProxima, lunesDeSemana } from './semana'
import type { CostoPlan, PlanSemanal } from './tipos'

type Periodicidad = 'SEMANAL' | 'QUINCENAL' | 'MENSUAL'

/** Todo lo que necesitan las pantallas del plan para la semana actual. */
export interface Semana {
  /** Lunes de la semana, AAAA-MM-DD. */
  lunes: string
  /** `null` si la usuaria todavía no ha armado esta semana. */
  plan: PlanSemanal | null
  costo?: CostoPlan
  /** Nombre de cada receta del recetario. */
  nombres: Map<number, string>
  /** Las recetas que están en el plan, con costo y faltantes. */
  recetas: Map<number, RecetaVista>
  cocina: Cocina
  /** Cómo adaptar cada receta del plan a las alergias de cada persona (ADR-24). */
  adaptaciones: Map<number, Adaptacion>
  /** Días sin tiempo para cocinar del perfil (1 = lunes). */
  diasSinTiempo: number[]
  periodicidad?: Periodicidad
}

export type EstadoSemana = { tipo: 'cargando' } | { tipo: 'listo'; semana: Semana } | { tipo: 'error'; error: unknown }

export async function cargarSemana(lunes: string, senal?: AbortSignal): Promise<Semana> {
  // Primero se envía lo que quedó guardado sin conexión (RNF-17); sin señal no tarda nada.
  await sincronizar()
  const plan = await obtenerPlan(lunes, senal)
  const idsEnPlan = [...new Set((plan?.comidas ?? []).map((c) => c.recetaId))]
  // Sin conexión se muestra lo que haya guardado; lo que no, simplemente no aparece (RNF-17).
  const sinSenal = <T,>(valor: T) => (error: unknown): T => {
    if (esSinConexion(error)) return valor
    throw error
  }
  const [resumen, cocina, perfil, costo, detalles, adaptaciones] = await Promise.all([
    listarRecetas(senal).catch(sinSenal<Receta[]>([])),
    cargarCocina(senal).catch(sinSenal<Cocina>({ productos: new Map(), existencias: new Map(), personas: [] })),
    pedir<{ diasSinTiempo?: number[]; periodicidadIngreso?: Periodicidad }>('/perfil/hogar', { senal }).catch(() => undefined),
    plan ? costoPlan(plan.id, senal).catch(sinSenal<CostoPlan | undefined>(undefined)) : Promise.resolve(undefined),
    Promise.all(idsEnPlan.map((id) => obtenerReceta(id, senal).catch(sinSenal<RecetaDetalle | undefined>(undefined)))),
    obtenerAdaptaciones(idsEnPlan, senal),
  ])
  const conDetalle = detalles.filter((d): d is RecetaDetalle => d !== undefined)
  return {
    lunes,
    plan,
    costo,
    nombres: new Map([...resumen.map((r) => [r.id, r.nombre] as const), ...conDetalle.map((d) => [d.id, d.nombre] as const)]),
    recetas: new Map(conDetalle.map((d) => [d.id, analizarReceta(d, cocina)])),
    cocina,
    adaptaciones,
    diasSinTiempo: perfil?.diasSinTiempo ?? [],
    periodicidad: perfil?.periodicidadIngreso,
  }
}

/**
 * Carga la semana actual (o la siguiente con `?semana=proxima`) al montar la pantalla.
 * `recargar` la vuelve a pedir sin borrar lo que se ve.
 */
export function useSemana(): { estado: EstadoSemana; recargar: () => void; proxima: boolean } {
  const [parametros] = useSearchParams()
  const proxima = esProxima(parametros)
  const lunes = lunesDeSemana(proxima)
  const [estado, setEstado] = useState<EstadoSemana>({ tipo: 'cargando' })
  const [version, setVersion] = useState(0)

  useEffect(() => {
    const control = new AbortController()
    cargarSemana(lunes, control.signal)
      .then((semana) => setEstado({ tipo: 'listo', semana }))
      .catch((error: unknown) => {
        if (!control.signal.aborted) {
          setEstado({ tipo: 'error', error })
        }
      })
    return () => control.abort()
  }, [lunes, version])

  const recargar = useCallback(() => setVersion((v) => v + 1), [])
  // Al volver la conexión se envían los cambios guardados y se vuelve a cargar la semana.
  useEffect(() => {
    window.addEventListener('online', recargar)
    return () => window.removeEventListener('online', recargar)
  }, [recargar])
  // Al cambiar de semana, lo que se ve es de la otra semana hasta que llegue la nueva.
  const vigente: EstadoSemana = estado.tipo === 'listo' && estado.semana.lunes !== lunes ? { tipo: 'cargando' } : estado
  return { estado: vigente, recargar, proxima }
}
