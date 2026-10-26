import { ErrorApi } from '../../api/cliente'
import { mensajeDeError } from '../../api/mensajesError'

/** Mensajes para los códigos del planificador y el recetario. */
const MENSAJES: Record<string, string> = {
  PLAN_SIN_PERFIL_HOGAR: 'Completa el perfil de tu hogar antes de planear la semana.',
  PLAN_SEMANA_INVALIDA: 'La semana del plan debe empezar en lunes. Recarga la página.',
  PLAN_DATOS_INVALIDOS: 'Elige el día y la comida para esta receta.',
  RECETA_NO_DISPONIBLE: 'Esta receta ya no está disponible. Elige otra.',
  CONFLICTO_ASIGNACION: 'Ese cambio ya se había guardado. Recarga el plan.',
  PLAN_SIN_ALTERNATIVAS: 'Ya viste todas las recetas que encajan en esta comida. Puedes elegir una en el recetario.',
  PLAN_COMIDA_PREPARADA: 'Esta comida ya está preparada; no se puede cambiar.',
  RECURSO_NO_ENCONTRADO: 'No encontramos lo que buscabas. Puede que ya no esté en tu plan.',
  LISTA_COSTO_INVALIDO: 'Escribe cuánto pagaste: un valor en pesos.',
}

export function mensajePlan(error: unknown): string {
  if (error instanceof ErrorApi && MENSAJES[error.codigo]) {
    return MENSAJES[error.codigo]
  }
  return mensajeDeError(error)
}

/** La usuaria todavía no tiene hogar: hay que llevarla al perfil. */
export function planSinHogar(error: unknown): boolean {
  return error instanceof ErrorApi && (error.codigo === 'PLAN_SIN_PERFIL_HOGAR' || error.codigo === 'DESPENSA_HOGAR_REQUERIDO')
}
