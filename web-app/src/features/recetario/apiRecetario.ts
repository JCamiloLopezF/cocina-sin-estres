import { ErrorApi, pedir, respaldoDe, SIN_CONEXION } from '../../api/cliente'
import type { Receta, RecetaDetalle } from './tipos'

/** Llamadas a `/api/recetas`. El recetario es el mismo para todos los hogares. */

export function listarRecetas(senal?: AbortSignal) {
  return pedir<Receta[]>('/recetas', { senal, respaldo: true })
}

/** Sin conexión, sale del último recetario completo que se cargó (RNF-17). */
export async function obtenerReceta(id: number, senal?: AbortSignal): Promise<RecetaDetalle> {
  try {
    return await pedir<RecetaDetalle>(`/recetas/${id}`, { senal, respaldo: true })
  } catch (error) {
    const guardada = error instanceof ErrorApi && error.codigo === SIN_CONEXION
      ? respaldoDe<RecetaDetalle[]>('/recetas/detalle')?.find((r) => r.id === id)
      : undefined
    if (guardada) return guardada
    throw error
  }
}

/**
 * Todas las recetas con sus ingredientes, para poder filtrar por ingrediente, costo y despensa.
 * Una sola petición (`/recetas/detalle`); si la API todavía no la tiene, una por receta.
 */
export async function listarRecetasConDetalle(senal?: AbortSignal): Promise<RecetaDetalle[]> {
  try {
    return await pedir<RecetaDetalle[]>('/recetas/detalle', { senal, respaldo: true })
  } catch (error) {
    if (!(error instanceof ErrorApi) || (error.status !== 400 && error.status !== 404)) throw error
  }
  const recetas = await listarRecetas(senal)
  return Promise.all(recetas.map((r) => obtenerReceta(r.id, senal)))
}
