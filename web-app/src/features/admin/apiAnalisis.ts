import { pedir } from '../../api/cliente'

/** Un indicador del Plan de Gestión de la Calidad con su valor actual (KAN-54). */
export interface Indicador {
  proceso: string
  nombre: string
  formula: string
  /** `null` si no hay datos o si se mide fuera de la app. */
  valor: number | null
  unidad: string | null
  /** `null` si es una métrica de seguimiento, sin meta. */
  meta: string | null
  cumple: boolean | null
  muestra: number
  nota: string
}

export interface RecetaUsada {
  nombre: string
  veces: number
}

/** `GET /api/admin/analisis`. */
export interface Analisis {
  /** AAAA-MM-DD. */
  desde: string
  hasta: string
  semanasDesde: string
  uso: {
    usuarias: number
    hogares: number
    activas7Dias: number
    aperturas: number
    planesEstaSemana: number
    preparaciones: number
    preparacionesFuera: number
    usosRescate: number
  }
  indicadores: Indicador[]
  masPreparadas: RecetaUsada[]
  masPlaneadas: RecetaUsada[]
  sinUsar: string[]
}

export function obtenerAnalisis(senal?: AbortSignal) {
  return pedir<Analisis>('/admin/analisis', { senal })
}
