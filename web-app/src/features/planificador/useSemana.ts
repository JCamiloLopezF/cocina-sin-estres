import { useCallback, useEffect, useState } from 'react'
import { useSearchParams } from 'react-router'
import { pedir } from '../../api/cliente'
import { obtenerAdaptaciones, type Adaptacion } from '../recetario/adaptaciones'
import { listarRecetas, obtenerReceta } from '../recetario/apiRecetario'
import { analizarReceta, cargarCocina, type Cocina, type RecetaVista } from '../recetario/cocina'
import { costoPlan, obtenerPlan } from './apiPlanificador'
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
  const plan = await obtenerPlan(lunes, senal)
  const idsEnPlan = [...new Set((plan?.comidas ?? []).map((c) => c.recetaId))]
  const [resumen, cocina, perfil, costo, detalles, adaptaciones] = await Promise.all([
    listarRecetas(senal),
    cargarCocina(senal),
    pedir<{ diasSinTiempo?: number[]; periodicidadIngreso?: Periodicidad }>('/perfil/hogar', { senal }).catch(() => undefined),
    plan ? costoPlan(plan.id, senal) : Promise.resolve(undefined),
    Promise.all(idsEnPlan.map((id) => obtenerReceta(id, senal))),
    obtenerAdaptaciones(idsEnPlan, senal),
  ])
  return {
    lunes,
    plan,
    costo,
    nombres: new Map(resumen.map((r) => [r.id, r.nombre])),
    recetas: new Map(detalles.map((d) => [d.id, analizarReceta(d, cocina)])),
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
  // Al cambiar de semana, lo que se ve es de la otra semana hasta que llegue la nueva.
  const vigente: EstadoSemana = estado.tipo === 'listo' && estado.semana.lunes !== lunes ? { tipo: 'cargando' } : estado
  return { estado: vigente, recargar, proxima }
}
