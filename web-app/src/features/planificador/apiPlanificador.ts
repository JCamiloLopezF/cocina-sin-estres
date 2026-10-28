import { ErrorApi, pedir } from '../../api/cliente'
import { comidaProvisional, conPendientes, encolar, esSinConexion, nuevoId, olvidarProvisional } from './sinConexion'
import type { ComidaPlan, CompraRegistrada, CostoPlan, ListaCompras, PlanGenerado, PlanSemanal, TipoComida } from './tipos'

/** Llamadas a `/api/planes-semanales`. Todas actúan sobre el hogar de la usuaria que entra. */

/**
 * El plan de la semana que empieza en `lunes`, o `null` si todavía no lo ha armado. Sin
 * conexión devuelve el último que se vio, con los cambios que faltan por enviar (RNF-17).
 */
export async function obtenerPlan(lunes: string, senal?: AbortSignal): Promise<PlanSemanal | null> {
  try {
    return conPendientes(await pedir<PlanSemanal>(`/planes-semanales?semanaInicio=${lunes}`, { senal, respaldo: true }))
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

/**
 * Pone una receta en un día y comida. Si ya había una, la reemplaza. Sin conexión, el cambio
 * queda guardado en el navegador y se envía después (RNF-17): devuelve la comida provisional.
 */
export async function asignarComida(planId: number, dia: number, tipoComida: TipoComida, recetaId: number): Promise<ComidaPlan> {
  const idCambioCliente = nuevoId()
  try {
    return await pedir<ComidaPlan>(`/planes-semanales/${planId}/comidas`, {
      metodo: 'PUT',
      cuerpo: { dia, tipoComida, recetaId, idCambioCliente },
    })
  } catch (error) {
    if (!esSinConexion(error)) throw error
    const pendiente = { id: idCambioCliente, tipo: 'ASIGNAR' as const, planId, dia, tipoComida, recetaId }
    encolar(pendiente)
    return comidaProvisional(pendiente)
  }
}

/** Quita una comida del plan. Sin conexión, se quita al volver la señal (RNF-17). */
export async function quitarComida(planId: number, comidaId: number): Promise<void> {
  if (comidaId < 0 && olvidarProvisional(comidaId)) return
  try {
    await pedir<void>(`/planes-semanales/${planId}/comidas/${comidaId}`, { metodo: 'DELETE' })
  } catch (error) {
    if (!esSinConexion(error)) throw error
    encolar({ id: nuevoId(), tipo: 'QUITAR', planId, comidaId })
  }
}

export function marcarPreparada(planId: number, comidaId: number) {
  return pedir<ComidaPlan>(`/planes-semanales/${planId}/comidas/${comidaId}/marcar-preparada`, { metodo: 'PUT' })
}

export function costoPlan(planId: number, senal?: AbortSignal) {
  return pedir<CostoPlan>(`/planes-semanales/${planId}/costo`, { senal, respaldo: true })
}

/** Marca un producto como comprado (entra a la despensa) o lo deshace (sale de la despensa). */
export function marcarComprado(planId: number, itemId: number, comprado: boolean) {
  return pedir<CompraRegistrada>(`/planes-semanales/${planId}/lista-compras/items/${itemId}`, {
    metodo: 'PUT',
    cuerpo: { comprado },
  })
}

/** Corrige lo que pagó de verdad por un producto (KAN-26); `null` vuelve al estimado. Devuelve la lista con el total ajustado. */
export function cambiarCostoItem(planId: number, itemId: number, costoReal: number | null) {
  return pedir<ListaCompras>(`/planes-semanales/${planId}/lista-compras/items/${itemId}/costo`, {
    metodo: 'PUT',
    cuerpo: { costoReal },
  })
}

export function listaCompras(planId: number, senal?: AbortSignal) {
  return pedir<ListaCompras>(`/planes-semanales/${planId}/lista-compras`, { senal, respaldo: true })
}

/** "Armar mi semana" (ADR-24): llena los huecos vacíos. Sin `comidas`, el almuerzo y la cena (el desayuno es opcional). */
export function generarPlan(planId: number, comidas?: TipoComida[]) {
  return pedir<PlanGenerado>(`/planes-semanales/${planId}/generar`, { metodo: 'POST', cuerpo: comidas ? { comidas } : {} })
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

/** La usuaria se queda con la semana que la app armó sola: deja de ser borrador (ADR-24). */
export function confirmarBorrador(planId: number) {
  return pedir<PlanSemanal>(`/planes-semanales/${planId}/confirmar`, { metodo: 'PUT' })
}
