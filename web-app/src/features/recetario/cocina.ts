import { ErrorApi } from '../../api/cliente'
import { catalogoDisponible, listarDespensa } from '../despensa/apiDespensa'
import type { ProductoDisponible, UnidadBase } from '../despensa/tipos'
import type { RecetaDetalle } from './tipos'

/**
 * Lo que hay en la cocina del hogar: el catálogo (nombres, unidades y precios de
 * referencia) y lo que queda en la despensa. Con esto el recetario sabe qué falta y
 * cuánto cuesta cada receta, con la misma fórmula del backend (KAN-24):
 * `cantidad / cantidadUnidadCompra * precioUnidadCompra`.
 */
export interface Cocina {
  productos: Map<number, ProductoDisponible>
  /** Cantidad disponible por producto, en su unidad base. Lo vencido no cuenta. */
  existencias: Map<number, number>
}

/** Si el hogar todavía no tiene perfil, la despensa y el catálogo responden este código. */
function sinHogar<T>(valor: T) {
  return (error: unknown): T => {
    if (error instanceof ErrorApi && error.codigo === 'DESPENSA_HOGAR_REQUERIDO') {
      return valor
    }
    throw error
  }
}

export async function cargarCocina(senal?: AbortSignal): Promise<Cocina> {
  const [productos, items] = await Promise.all([
    catalogoDisponible('', senal).catch(sinHogar<ProductoDisponible[]>([])),
    listarDespensa('TODO', '', senal).catch(sinHogar([])),
  ])
  const existencias = new Map<number, number>()
  items
    .filter((item) => item.estadoVencimiento !== 'VENCIDO')
    .forEach((item) => existencias.set(item.productoId, (existencias.get(item.productoId) ?? 0) + item.cantidad))
  return { productos: new Map(productos.map((p) => [p.id, p])), existencias }
}

export interface IngredienteVista {
  productoId: number
  nombre: string
  cantidad: number
  unidad: UnidadBase
  opcional: boolean
  /** Costo estimado de esa cantidad; ausente si el producto no tiene precio de referencia. */
  costo?: number
  /** La despensa alcanza para esta cantidad. */
  enDespensa: boolean
}

export interface RecetaVista {
  receta: RecetaDetalle
  ingredientes: IngredienteVista[]
  /** Costo de la receta completa sin los opcionales (que no se compran). */
  costo?: number
  costoPorcion?: number
  /** Ingredientes obligatorios que no alcanzan con lo que hay en la despensa. */
  faltan: number
}

/** Calcula costo y faltantes. `porciones` escala las cantidades (por defecto, las de la receta). */
export function analizarReceta(receta: RecetaDetalle, cocina: Cocina, porciones = receta.porciones): RecetaVista {
  const factor = receta.porciones > 0 ? porciones / receta.porciones : 1
  const ingredientes = receta.ingredientes.map((ing): IngredienteVista => {
    const producto = cocina.productos.get(ing.productoId)
    const cantidad = ing.cantidad * factor
    const costo =
      producto?.precioUnidadCompra != null && producto.cantidadUnidadCompra
        ? (cantidad / producto.cantidadUnidadCompra) * producto.precioUnidadCompra
        : undefined
    return {
      productoId: ing.productoId,
      nombre: producto?.nombre ?? `Producto ${ing.productoId}`,
      cantidad,
      unidad: producto?.unidadBase ?? 'unidad',
      opcional: ing.opcional,
      costo,
      enDespensa: (cocina.existencias.get(ing.productoId) ?? 0) >= cantidad - 1e-9,
    }
  })
  const obligatorios = ingredientes.filter((i) => !i.opcional)
  const conPrecio = obligatorios.filter((i) => i.costo !== undefined)
  const costo = conPrecio.length ? conPrecio.reduce((suma, i) => suma + (i.costo ?? 0), 0) : undefined
  return {
    receta,
    ingredientes,
    costo,
    costoPorcion: costo !== undefined && porciones > 0 ? costo / porciones : undefined,
    faltan: obligatorios.filter((i) => !i.enDespensa).length,
  }
}
