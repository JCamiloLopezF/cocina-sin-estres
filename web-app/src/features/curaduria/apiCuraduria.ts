import { ErrorApi, pedir } from '../../api/cliente'
import { mensajeDeError } from '../../api/mensajesError'
import type { TipoCantidad, UnidadBase } from '../despensa/tipos'
import type { TipoComida } from '../planificador/tipos'

/** `GET /api/curaduria/productos` (ProductoResponse): el catálogo global con precio de referencia. */
export interface ProductoCurado {
  id: number
  nombre: string
  categoria: string
  tipoCantidad: TipoCantidad
  unidadBase: UnidadBase
  unidadCompra: string
  cantidadUnidadCompra: number
  precioUnidadCompra: number
  /** Solo granel: cantidad que equivale a "Lleno" (ADR-15). */
  capacidadReferencia?: number
  esBasico: boolean
  activo: boolean
  /** Días que dura desde que se compra: lo comprado entra a la despensa con ese vencimiento (ADR-24). */
  vidaUtilDias?: number | null
}

export type DatosProducto = Omit<ProductoCurado, 'id' | 'activo'>

/** Las categorías que acepta el catálogo (V17). La base rechaza cualquier otra. */
export const CATEGORIAS = [
  'GRANOS',
  'LEGUMBRES',
  'TUBERCULOS',
  'HARINAS',
  'PROTEINAS',
  'LACTEOS',
  'VERDURAS',
  'FRUTAS',
  'ACEITES',
  'CONDIMENTOS',
  'BEBIDAS',
  'OTROS',
]

export function listarProductos(senal?: AbortSignal) {
  return pedir<ProductoCurado[]>('/curaduria/productos', { senal })
}

export function obtenerProducto(id: number, senal?: AbortSignal) {
  return pedir<ProductoCurado>(`/curaduria/productos/${id}`, { senal })
}

export function crearProducto(datos: DatosProducto) {
  return pedir<ProductoCurado>('/curaduria/productos', { metodo: 'POST', cuerpo: datos })
}

export function actualizarProducto(id: number, datos: DatosProducto) {
  return pedir<ProductoCurado>(`/curaduria/productos/${id}`, { metodo: 'PUT', cuerpo: datos })
}

/** Desactiva el producto: deja de ofrecerse, pero las despensas que lo tienen lo conservan. */
export function desactivarProducto(id: number) {
  return pedir<void>(`/curaduria/productos/${id}`, { metodo: 'DELETE' })
}

/** Un reemplazo para adaptar recetas a una alergia (ADR-24). */
export interface Reemplazo {
  reemplazoId: number
  nombre: string
  categoria: string
  /** SISTEMA: lo propuso la app. CURADOR: lo decidió un curador. */
  origen: 'SISTEMA' | 'CURADOR'
  /** Inactivo: el curador lo quitó y la app no lo vuelve a proponer. */
  activo: boolean
}

export function listarReemplazos(productoId: number, senal?: AbortSignal) {
  return pedir<Reemplazo[]>(`/curaduria/productos/${productoId}/reemplazos`, { senal })
}

export function usarReemplazo(productoId: number, reemplazoId: number) {
  return pedir<Reemplazo[]>(`/curaduria/productos/${productoId}/reemplazos/${reemplazoId}`, { metodo: 'PUT' })
}

export function quitarReemplazo(productoId: number, reemplazoId: number) {
  return pedir<Reemplazo[]>(`/curaduria/productos/${productoId}/reemplazos/${reemplazoId}`, { metodo: 'DELETE' })
}

/** `GET /api/curaduria/recetas`: cada receta con su video (V10) y sus etiquetas (V7). */
export interface RecetaConVideo {
  id: number
  nombre: string
  tiempoMinutos: number
  /** Lo más que puede durar el fragmento: la mitad del tiempo de la receta. */
  maximoSegundos: number
  youtubeId?: string | null
  inicioSeg?: number | null
  finSeg?: number | null
  /** Sirve como almuerzo o cena: "Armar mi semana" solo usa estas. */
  platoFuerte: boolean
  /** En general les gusta a los niños: se prefiere si en el hogar hay niños. */
  gustaANinos: boolean
}

export function listarVideosRecetas(senal?: AbortSignal) {
  return pedir<RecetaConVideo[]>('/curaduria/recetas', { senal })
}

export function guardarVideoReceta(recetaId: number, datos: { enlace: string; inicioSeg: number; finSeg: number }) {
  return pedir<RecetaConVideo>(`/curaduria/recetas/${recetaId}/video`, { metodo: 'PUT', cuerpo: datos })
}

/** Marca la receta como plato fuerte o que les gusta a los niños (KAN-17). Lo que no se envía no cambia. */
export function cambiarEtiquetasReceta(recetaId: number, etiquetas: { platoFuerte?: boolean; gustaANinos?: boolean }) {
  return pedir<RecetaConVideo>(`/curaduria/recetas/${recetaId}/etiquetas`, { metodo: 'PUT', cuerpo: etiquetas })
}

export function quitarVideoReceta(recetaId: number) {
  return pedir<RecetaConVideo>(`/curaduria/recetas/${recetaId}/video`, { metodo: 'DELETE' })
}

/** Un ingrediente de una receta del curador (RNF-28). `cantidad` va en la unidad base del producto. */
export interface IngredienteCurado {
  productoId: number
  nombre: string
  categoria: string
  unidadBase: UnidadBase
  cantidad: number
  /** La receta se puede preparar sin él: no se compra ni se descuenta. */
  opcional: boolean
  /** Define el plato: si alguien es alérgico, no se reemplaza (le toca plato aparte). */
  principal: boolean
  /** El producto sigue en el catálogo. */
  activo: boolean
}

/** `GET /api/curaduria/recetas/{id}`: la receta completa como la edita el curador. */
export interface RecetaCurada {
  id: number
  nombre: string
  descripcion?: string | null
  tiempoMinutos: number
  porciones: number
  pasos: string[]
  preparacionPrevia?: string | null
  ingredientes: IngredienteCurado[]
  momentos: TipoComida[]
  platoFuerte: boolean
  gustaANinos: boolean
  activa: boolean
  /** Costo de la receta con los precios de referencia, sin los opcionales. */
  costo: number
  tieneVideo: boolean
}

/** `GET /api/curaduria/recetas/catalogo`: una fila del listado, activas y desactivadas. */
export interface RecetaCuradaResumen {
  id: number
  nombre: string
  tiempoMinutos: number
  porciones: number
  momentos: TipoComida[]
  platoFuerte: boolean
  activa: boolean
  ingredientes: number
  costo: number
  tieneVideo: boolean
}

/** Lo que se envía al crear o editar una receta. */
export interface DatosReceta {
  nombre: string
  descripcion?: string
  tiempoMinutos: number
  porciones: number
  pasos: string[]
  preparacionPrevia?: string
  ingredientes: { productoId: number; cantidad: number; opcional: boolean; principal: boolean }[]
  momentos: TipoComida[]
  platoFuerte: boolean
  gustaANinos: boolean
}

export function listarRecetasCuradas(senal?: AbortSignal) {
  return pedir<RecetaCuradaResumen[]>('/curaduria/recetas/catalogo', { senal })
}

export function obtenerRecetaCurada(id: number, senal?: AbortSignal) {
  return pedir<RecetaCurada>(`/curaduria/recetas/${id}`, { senal })
}

export function crearReceta(datos: DatosReceta) {
  return pedir<RecetaCurada>('/curaduria/recetas', { metodo: 'POST', cuerpo: datos })
}

export function actualizarReceta(id: number, datos: DatosReceta) {
  return pedir<RecetaCurada>(`/curaduria/recetas/${id}`, { metodo: 'PUT', cuerpo: datos })
}

/** La receta deja de ofrecerse en el recetario y en el plan; los planes que ya la tienen la conservan. */
export function desactivarReceta(id: number) {
  return pedir<RecetaCurada>(`/curaduria/recetas/${id}`, { metodo: 'DELETE' })
}

export function reactivarReceta(id: number) {
  return pedir<RecetaCurada>(`/curaduria/recetas/${id}/reactivar`, { metodo: 'POST' })
}

/**
 * Los errores de validación del catálogo (`PRODUCTO_*`, `RECETA_*`) ya traen en el detalle qué dato
 * falló, redactado para quien cura el catálogo; los demás usan el mensaje general.
 */
export function mensajeCuraduria(error: unknown): string {
  if (error instanceof ErrorApi && /^(PRODUCTO|RECETA)_/.test(error.codigo) && error.message) {
    return error.message
  }
  return mensajeDeError(error)
}
