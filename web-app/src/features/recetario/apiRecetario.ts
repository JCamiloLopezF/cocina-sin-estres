import { pedir } from '../../api/cliente'
import type { Receta, RecetaDetalle } from './tipos'

/** Llamadas a `/api/recetas`. El recetario es el mismo para todos los hogares. */

export function listarRecetas(senal?: AbortSignal) {
  return pedir<Receta[]>('/recetas', { senal })
}

export function obtenerReceta(id: number, senal?: AbortSignal) {
  return pedir<RecetaDetalle>(`/recetas/${id}`, { senal })
}

/** Todas las recetas con sus ingredientes, para poder filtrar por ingrediente, costo y despensa. */
export async function listarRecetasConDetalle(senal?: AbortSignal): Promise<RecetaDetalle[]> {
  const recetas = await listarRecetas(senal)
  return Promise.all(recetas.map((r) => obtenerReceta(r.id, senal)))
}
