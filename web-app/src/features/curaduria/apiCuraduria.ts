import { ErrorApi, pedir } from '../../api/cliente'
import { mensajeDeError } from '../../api/mensajesError'
import type { TipoCantidad, UnidadBase } from '../despensa/tipos'

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
}

export type DatosProducto = Omit<ProductoCurado, 'id' | 'activo'>

export const CATEGORIAS = ['GRANOS', 'PROTEINAS', 'LACTEOS', 'VERDURAS', 'FRUTAS', 'ACEITES', 'CONDIMENTOS']

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

/**
 * Los errores de validación del catálogo (`PRODUCTO_*`) ya traen en el detalle qué dato
 * falló, redactado para quien cura el catálogo; los demás usan el mensaje general.
 */
export function mensajeCuraduria(error: unknown): string {
  if (error instanceof ErrorApi && error.codigo.startsWith('PRODUCTO_') && error.message) {
    return error.message
  }
  return mensajeDeError(error)
}
