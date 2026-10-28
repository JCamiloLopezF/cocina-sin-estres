import type { ItemDespensa, Nivel, ProductoDisponible, UnidadBase } from './tipos'

/** Textos de la despensa para la usuaria: cantidades, niveles, vencimientos y precios. */

export const NOMBRE_NIVEL: Record<Nivel, string> = {
  AGOTADO: 'Agotado',
  POCO: 'Poco',
  MEDIO: 'Medio',
  LLENO: 'Lleno',
}

const CATEGORIAS: Record<string, string> = {
  GRANOS: 'Arroz, pasta y granos',
  LEGUMBRES: 'Legumbres',
  TUBERCULOS: 'Papa, yuca y plátano',
  HARINAS: 'Harinas, arepas y pan',
  PROTEINAS: 'Carnes, huevos y pescado',
  LACTEOS: 'Lácteos',
  VERDURAS: 'Verduras',
  FRUTAS: 'Frutas',
  ACEITES: 'Aceites',
  CONDIMENTOS: 'Condimentos y panela',
  BEBIDAS: 'Café y chocolate',
  OTROS: 'Otros',
}

/** Nombre legible de una categoría del catálogo (en la base va en mayúsculas). */
export function nombreCategoria(codigo: string): string {
  return CATEGORIAS[codigo] ?? codigo.charAt(0) + codigo.slice(1).toLowerCase()
}

const numero = new Intl.NumberFormat('es-CO', { maximumFractionDigits: 3 })
const pesos = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 })

/** Pesos colombianos sin decimales: "$ 2.900". */
export function enPesos(valor: number): string {
  return pesos.format(valor)
}

/** Lo que la usuaria escribe como precio: "12.500", "$12.500" o "12500" → 12500. Sin números → `undefined`. */
export function leerPesos(texto: string): number | undefined {
  const digitos = texto.replace(/\D/g, '')
  return digitos ? Number(digitos) : undefined
}

/** "6 unidades", "1 unidad", "500 g", "1,5 l"… */
export function cantidadConUnidad(cantidad: number, unidad: UnidadBase): string {
  if (unidad === 'unidad') {
    return `${numero.format(cantidad)} ${cantidad === 1 ? 'unidad' : 'unidades'}`
  }
  if (cantidad >= 1000) {
    return `${numero.format(cantidad / 1000)} ${unidad === 'g' ? 'kg' : 'l'}`
  }
  return `${numero.format(cantidad)} ${unidad}`
}

/** Cuánto hay, como lo ve la usuaria: nivel para granel, cantidad para contables. */
export function describirCantidad(item: ItemDespensa): string {
  if (item.tipoCantidad === 'GRANEL') {
    return `Nivel: ${NOMBRE_NIVEL[item.nivel ?? 'AGOTADO'].toLowerCase()}`
  }
  return cantidadConUnidad(item.cantidad, item.unidadBase)
}

/** "vence hoy", "vence mañana", "vence en 5 días", "venció hace 2 días"; vacío sin fecha. */
export function describirVencimiento(item: Pick<ItemDespensa, 'diasParaVencer'>): string {
  const dias = item.diasParaVencer
  if (dias === undefined || dias === null) {
    return ''
  }
  if (dias < 0) {
    return Math.abs(dias) === 1 ? 'venció ayer' : `venció hace ${Math.abs(dias)} días`
  }
  if (dias === 0) {
    return 'vence hoy'
  }
  if (dias === 1) {
    return 'vence mañana'
  }
  return `vence en ${dias} días`
}

/** Línea de detalle del listado: "6 unidades · vence en 2 días · bajo el mínimo". */
export function detalleItem(item: ItemDespensa): string {
  return [describirCantidad(item), describirVencimiento(item), item.bajoMinimo ? 'bajo el mínimo' : '']
    .filter(Boolean)
    .join(' · ')
}

/** "Granel · $4.500 / bolsa" o "Contable · $18.000 / cubeta" (wireframe 16). */
export function detalleProducto(producto: ProductoDisponible): string {
  const tipo = producto.tipoCantidad === 'GRANEL' ? 'Granel' : 'Contable'
  if (producto.precioUnidadCompra == null || !producto.unidadCompra) {
    return tipo
  }
  return `${tipo} · ${pesos.format(producto.precioUnidadCompra)} / ${producto.unidadCompra.toLowerCase()}`
}

/** Fecha AAAA-MM-DD como "17/09/2026". */
export function fechaCorta(iso: string): string {
  const [anio, mes, dia] = iso.split('-')
  return `${dia}/${mes}/${anio}`
}
