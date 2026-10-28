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

export async function obtenerAnalisis(senal?: AbortSignal): Promise<Analisis> {
  const analisis = await pedir<Analisis>('/admin/analisis', { senal })
  return { ...analisis, indicadores: analisis.indicadores.map(completarIndicador) }
}

/**
 * La API no manda los campos vacíos (Jackson `non_null`): un indicador sin valor llega sin
 * `valor`, sin `meta` o sin `cumple`. Se completan con `null`, que es lo que el resto de la
 * pantalla espera; con `undefined` se mostraba «NaN» y «Sin datos».
 */
export function completarIndicador(indicador: Partial<Indicador> & Pick<Indicador, 'nombre'>): Indicador {
  return {
    proceso: indicador.proceso ?? '',
    nombre: indicador.nombre,
    formula: indicador.formula ?? '',
    valor: indicador.valor ?? null,
    unidad: indicador.unidad ?? null,
    meta: indicador.meta ?? null,
    cumple: indicador.cumple ?? null,
    muestra: indicador.muestra ?? 0,
    nota: indicador.nota ?? '',
  }
}
