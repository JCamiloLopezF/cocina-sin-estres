/** Tipos de la API del recetario (backend: módulo recetario, `/api/recetas`). */

/** `GET /api/recetas` (RecetaResponse): resumen sin pasos ni ingredientes. */
export interface Receta {
  id: number
  nombre: string
  descripcion?: string
  tiempoMinutos: number
  porciones: number
}

/** Un ingrediente de la receta. `cantidad` va en la unidad base del producto (g, ml o unidad). */
export interface IngredienteReceta {
  productoId: number
  cantidad: number
  opcional: boolean
}

/** `GET /api/recetas/{id}` (RecetaDetalleResponse). */
export interface RecetaDetalle extends Receta {
  preparacionPrevia?: string
  pasos: string[]
  ingredientes: IngredienteReceta[]
}
