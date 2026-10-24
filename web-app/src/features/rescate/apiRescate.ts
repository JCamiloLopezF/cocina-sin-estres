import { pedir } from '../../api/cliente'

/** Un producto por vencer que la receta salva. `dias`: 0 es hoy. */
export interface PorVencer {
  productoId: number
  nombre: string
  venceEn: string
  dias: number
}

/** `GET /api/rescate` (RescateResponse.Opcion, KAN-25). */
export interface OpcionRescate {
  recetaId: number
  nombre: string
  tiempoMinutos: number
  /** APTA, ADAPTADA o PLATO_APARTE frente a las alergias del hogar. */
  estado: string
  aprovecha: PorVencer[]
}

export function opcionesRescate(maxMinutos?: number, senal?: AbortSignal) {
  const consulta = maxMinutos ? `?maxMinutos=${maxMinutos}` : ''
  return pedir<{ opciones: OpcionRescate[] }>(`/rescate${consulta}`, { senal })
}

/** "hoy", "mañana" o "en 3 días". */
export function cuando(dias: number): string {
  if (dias <= 0) return 'hoy'
  return dias === 1 ? 'mañana' : `en ${dias} días`
}
