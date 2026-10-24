import { pedir } from '../../api/cliente'

/** Los cinco avisos de ADR-17, en el orden en que se muestran. */
export type TipoAviso = 'BAJO_MINIMO' | 'POR_VENCER' | 'PREPARACION_PREVIA' | 'SEMANA_SIN_PLANIFICAR' | 'APROVECHAR_PRODUCTO'

/** `GET /api/avisos` (AvisoResponse). */
export interface Aviso {
  id: number
  tipo: TipoAviso
  titulo: string
  mensaje: string
  /** Ruta de la app a la que lleva el botón. */
  enlace?: string | null
  accion?: string | null
  leido: boolean
  creadoEn?: string | null
}

export interface PreferenciaAviso {
  tipo: TipoAviso
  titulo: string
  descripcion: string
  activo: boolean
}

/** Corre el motor de avisos al abrir la app: hasta 5 avisos sin leer (ADR-17). */
export function evaluarAvisos(senal?: AbortSignal) {
  return pedir<Aviso[]>('/avisos', { senal })
}

/** Centro de avisos: los de los últimos 30 días. */
export function avisosRecientes(senal?: AbortSignal) {
  return pedir<Aviso[]>('/avisos/recientes', { senal })
}

export function marcarLeido(id: number) {
  return pedir<void>(`/avisos/${id}/leido`, { metodo: 'PUT' })
}

export function marcarTodosLeidos() {
  return pedir<void>('/avisos/leidos', { metodo: 'PUT' })
}

export function preferenciasAvisos(senal?: AbortSignal) {
  return pedir<PreferenciaAviso[]>('/avisos/preferencias', { senal })
}

export function cambiarPreferencia(tipo: TipoAviso, activo: boolean) {
  return pedir<PreferenciaAviso[]>(`/avisos/preferencias/${tipo}`, { metodo: 'PUT', cuerpo: { activo } })
}

/** Un dibujo por tipo, para reconocerlos de un vistazo. */
export const ICONO_AVISO: Record<TipoAviso, string> = {
  BAJO_MINIMO: '🧺',
  POR_VENCER: '⏳',
  PREPARACION_PREVIA: '🌙',
  SEMANA_SIN_PLANIFICAR: '📅',
  APROVECHAR_PRODUCTO: '🍳',
}
