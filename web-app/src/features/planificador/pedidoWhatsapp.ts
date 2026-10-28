import { cantidadConUnidad } from '../despensa/formato'
import type { ProductoDisponible } from '../despensa/tipos'

/**
 * El pedido a la tienda por WhatsApp (KAN-53): un mensaje con lo que falta de la lista de
 * compras, en las cantidades que vende una tienda, y el enlace `wa.me` que lo abre listo
 * para enviar. Son funciones puras para probarlas sin pantalla.
 */

/** "bolsa" → "bolsas"; "unidad" → "unidades". */
export function plural(palabra: string, cantidad: number): string {
  if (cantidad === 1) return palabra
  return /[aeiouáéíóú]$/i.test(palabra) ? `${palabra}s` : `${palabra}es`
}

/**
 * Cuánto pedir de un producto, como se pide en la tienda:
 * - por libra, bolsa, paquete o botella: "1 libra", "2 bolsas de 1 kg";
 * - lo que se vende por unidad: "2 latas", y si viene en racimo, las unidades exactas;
 * - los huevos, como se piden en la tienda: media docena, una docena o cubetas
 *   (auditoría: el pedido decía «Huevos: 1 unidad»).
 */
export function cantidadParaPedir(cantidad: number, producto?: ProductoDisponible): string {
  const unidadBase = producto?.unidadBase ?? 'unidad'
  const presentacion = producto?.cantidadUnidadCompra
  const unidadCompra = producto?.unidadCompra?.trim().toLowerCase()
  if (!presentacion || presentacion <= 0 || !unidadCompra) {
    return cantidadConUnidad(unidadBase === 'unidad' ? Math.ceil(cantidad) : Math.round(cantidad), unidadBase)
  }
  if (unidadBase === 'unidad') {
    if (unidadCompra === 'cubeta') return huevos(Math.ceil(cantidad), presentacion)
    if (presentacion !== 1) return cantidadConUnidad(Math.ceil(cantidad), 'unidad')
    const n = Math.ceil(cantidad)
    return `${n} ${plural(unidadCompra, n)}`
  }
  const n = Math.max(1, Math.ceil(cantidad / presentacion))
  const tamano = unidadCompra === 'libra' ? '' : ` de ${cantidadConUnidad(presentacion, unidadBase)}`
  return `${n} ${plural(unidadCompra, n)}${tamano}`
}

/** 1 a 6 huevos: media docena; 7 a 12: una docena; más: cubetas completas. */
function huevos(n: number, porCubeta: number): string {
  if (n <= 6) return 'media docena'
  if (n <= 12) return '1 docena'
  const cubetas = Math.ceil(n / porCubeta)
  return `${cubetas} ${plural('cubeta', cubetas)}`
}

export interface LineaPedido {
  producto: string
  cantidad: string
}

/** El mensaje que la usuaria le manda a la tienda; la trata de "usted", como se le habla al tendero. */
export function mensajePedido(lineas: LineaPedido[], tienda?: string, firma?: string): string {
  const saludo = tienda ? `Hola, ${tienda}. ` : 'Hola. '
  return [
    `${saludo}Le quiero hacer este pedido:`,
    '',
    ...lineas.map((l) => `- ${l.producto}: ${l.cantidad}`),
    '',
    `¿Me confirma el valor y si me lo pueden traer? Muchas gracias.${firma ? ` ${firma}` : ''}`,
  ].join('\n')
}

/** Abre WhatsApp con el mensaje escrito; sin número, WhatsApp deja escoger el contacto. */
export function enlaceWhatsapp(texto: string, numero?: string): string {
  return `https://wa.me/${numero ?? ''}?text=${encodeURIComponent(texto)}`
}
