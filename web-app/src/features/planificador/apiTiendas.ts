import { pedir } from '../../api/cliente'

/** Una tienda del barrio a la que la usuaria le pide el mercado por WhatsApp (KAN-53). */
export interface Tienda {
  id: number
  nombre: string
  /** Solo dígitos y con indicativo (573001234567), listo para `https://wa.me/`. */
  whatsapp: string
}

export function listarTiendas(senal?: AbortSignal) {
  return pedir<Tienda[]>('/tiendas', { senal })
}

/** El backend acepta el número como lo escriba ("300 123 4567", "+57…") y lo normaliza. */
export function crearTienda(nombre: string, whatsapp: string) {
  return pedir<Tienda>('/tiendas', { metodo: 'POST', cuerpo: { nombre, whatsapp } })
}

export function borrarTienda(id: number) {
  return pedir<void>(`/tiendas/${id}`, { metodo: 'DELETE' })
}
