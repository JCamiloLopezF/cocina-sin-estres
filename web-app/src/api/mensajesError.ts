import { ErrorApi, RESPUESTA_INESPERADA, SIN_CONEXION } from './cliente'

/**
 * Mensajes para la usuaria según el código de error del backend (CONTRIBUTING.md §7).
 * Tono cordial y concreto: qué pasó y qué puede hacer (RNF-14).
 */
const MENSAJES: Record<string, string> = {
  [SIN_CONEXION]: 'No pudimos conectarnos. Revisa tu internet e inténtalo de nuevo.',
  [RESPUESTA_INESPERADA]: 'Algo falló de nuestro lado. Inténtalo de nuevo en un momento.',
  AUTH_TOKEN_INVALIDO: 'Tu sesión expiró. Vuelve a entrar.',
  AUTH_SIN_PERMISO: 'Tu cuenta no tiene permiso para ver esto.',
  AUTH_CUENTA_INACTIVA: 'Esta cuenta está desactivada. Escríbele al equipo de Cocina sin estrés para revisarla.',
  VALIDACION_DATOS: 'Revisa los datos marcados.',
  PETICION_MAL_FORMADA: 'No pudimos leer los datos enviados. Recarga la página e inténtalo de nuevo.',
  PETICION_NO_SOPORTADA: 'Esta acción no está disponible. Recarga la página.',
  RECURSO_NO_ENCONTRADO: 'No encontramos lo que buscabas.',
  LIMITE_PETICIONES: 'Vas muy rápido. Espera un minuto y vuelve a intentarlo.',
  CUERPO_DEMASIADO_GRANDE: 'Lo que enviaste es demasiado grande. Revisa los datos e inténtalo de nuevo.',
  ERROR_INTERNO: 'Algo falló de nuestro lado. Inténtalo de nuevo en un momento.',
}

/** Texto para mostrar a partir de cualquier error capturado. */
export function mensajeDeError(error: unknown): string {
  if (error instanceof ErrorApi) {
    return MENSAJES[error.codigo] ?? MENSAJES[RESPUESTA_INESPERADA]
  }
  return MENSAJES[RESPUESTA_INESPERADA]
}
