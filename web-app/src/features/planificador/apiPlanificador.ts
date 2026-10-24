import { ErrorApi, pedir } from '../../api/cliente'
import type { ComidaPlan, CostoPlan, ListaCompras, PlanGenerado, PlanSemanal, TipoComida } from './tipos'

/** Llamadas a `/api/planes-semanales`. Todas actúan sobre el hogar de la usuaria que entra. */

/** El plan de la semana que empieza en `lunes`, o `null` si todavía no lo ha armado. */
export async function obtenerPlan(lunes: string, senal?: AbortSignal): Promise<PlanSemanal | null> {
  try {
    return await pedir<PlanSemanal>(`/planes-semanales?semanaInicio=${lunes}`, { senal })
  } catch (error) {
    if (error instanceof ErrorApi && error.status === 404) {
      return null
    }
    throw error
  }
}

/** Crea el plan de la semana; si ya existe, el backend devuelve el que hay. */
export function crearPlan(lunes: string) {
  return pedir<PlanSemanal>('/planes-semanales', { metodo: 'POST', cuerpo: { semanaInicio: lunes } })
}

/** Pone una receta en un día y comida. Si ya había una, la reemplaza. */
export function asignarComida(planId: number, dia: number, tipoComida: TipoComida, recetaId: number) {
  return pedir<ComidaPlan>(`/planes-semanales/${planId}/comidas`, {
    metodo: 'PUT',
    cuerpo: { dia, tipoComida, recetaId },
  })
}

export function quitarComida(planId: number, comidaId: number) {
  return pedir<void>(`/planes-semanales/${planId}/comidas/${comidaId}`, { metodo: 'DELETE' })
}

export function marcarPreparada(planId: number, comidaId: number) {
  return pedir<ComidaPlan>(`/planes-semanales/${planId}/comidas/${comidaId}/marcar-preparada`, { metodo: 'PUT' })
}

export function costoPlan(planId: number, senal?: AbortSignal) {
  return pedir<CostoPlan>(`/planes-semanales/${planId}/costo`, { senal })
}

export function listaCompras(planId: number, senal?: AbortSignal) {
  return pedir<ListaCompras>(`/planes-semanales/${planId}/lista-compras`, { senal })
}

/** "Armar mi semana" (ADR-24): llena los huecos vacíos de almuerzo y cena. Lo que ya está no se toca. */
export function generarPlan(planId: number) {
  return pedir<PlanGenerado>(`/planes-semanales/${planId}/generar`, { metodo: 'POST', cuerpo: {} })
}

/** Cambia la comida por la siguiente mejor receta, sin repetir las que ya se descartaron. */
export function otraOpcion(planId: number, comidaId: number, excluir: number[]) {
  return pedir<ComidaPlan>(`/planes-semanales/${planId}/comidas/${comidaId}/otra-opcion`, {
    metodo: 'POST',
    cuerpo: { excluir },
  })
}

/** Cambia las comidas más caras (no preparadas) por otras más baratas hasta caber en el presupuesto. */
export function ajustarPresupuesto(planId: number) {
  return pedir<PlanGenerado>(`/planes-semanales/${planId}/ajustar-presupuesto`, { metodo: 'POST' })
}
