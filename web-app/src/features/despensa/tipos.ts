/** Tipos de la API de la despensa (backend: módulo despensa, ADR-11 y ADR-15). */

export type TipoCantidad = 'CONTABLE' | 'GRANEL'
export type UnidadBase = 'g' | 'ml' | 'unidad'
export type Nivel = 'AGOTADO' | 'POCO' | 'MEDIO' | 'LLENO'
export type EstadoVencimiento = 'SIN_FECHA' | 'VIGENTE' | 'POR_VENCER' | 'VENCIDO'
export type Filtro = 'TODO' | 'POR_VENCER' | 'BAJO_MINIMO'

/** `GET /api/items-despensa` (ItemDespensaResponse). Los opcionales pueden no venir. */
export interface ItemDespensa {
  id: number
  productoId: number
  nombre: string
  categoria: string
  tipoCantidad: TipoCantidad
  unidadBase: UnidadBase
  cantidad: number
  esEstimada: boolean
  nivel?: Nivel
  cantidadMinima?: number
  nivelMinimo?: Nivel
  fechaVencimiento?: string
  diasParaVencer?: number
  estadoVencimiento: EstadoVencimiento
  bajoMinimo: boolean
  ubicacion?: string
  unidadCompra?: string
  cantidadUnidadCompra?: number
  precioUnidadCompra?: number
}

/** `GET /api/items-despensa/resumen`. */
export interface ResumenDespensa {
  total: number
  porVencer: number
  bajoMinimo: number
}

/** `GET /api/items-despensa/catalogo` (ProductoDisponibleResponse). */
export interface ProductoDisponible {
  id: number
  nombre: string
  categoria: string
  tipoCantidad: TipoCantidad
  unidadBase: UnidadBase
  unidadCompra?: string
  cantidadUnidadCompra?: number
  precioUnidadCompra?: number
  /** Existe si el producto ya está en la despensa. */
  itemDespensaId?: number
}

/** Lo que se edita de un ítem. Según el tipo va `cantidad` o `nivel`, nunca los dos. */
export interface DatosItem {
  cantidad?: number
  nivel?: Nivel
  cantidadMinima?: number
  nivelMinimo?: Nivel
  /** AAAA-MM-DD */
  fechaVencimiento?: string
  ubicacion?: string
}
