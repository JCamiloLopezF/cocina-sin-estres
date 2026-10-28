import { ErrorApi } from '../../api/cliente'
import { mensajeDeError } from '../../api/mensajesError'

/** Mensajes para los códigos del módulo despensa (CodigoDespensa en el backend). */
const MENSAJES: Record<string, string> = {
  DESPENSA_HOGAR_REQUERIDO: 'Completa el perfil de tu hogar para empezar a usar tu despensa.',
  DESPENSA_PRODUCTO_REQUERIDO: 'Elige el producto que quieres agregar.',
  DESPENSA_PRODUCTO_REPETIDO: 'Ya tienes este producto en tu despensa. Puedes editar su cantidad.',
  DESPENSA_CANTIDAD_REQUERIDA: 'Indica cuánto tienes de este producto.',
  DESPENSA_CANTIDAD_INVALIDA: 'Revisa la cantidad: no puede ser negativa ni tan grande, y admite hasta 3 decimales.',
  DESPENSA_NIVEL_REQUERIDO: 'Elige cuánto te queda: Poco, Medio o Lleno.',
  DESPENSA_MINIMO_INVALIDO: 'El mínimo no puede ser negativo y admite hasta 3 decimales.',
  DESPENSA_NIVEL_MINIMO_INVALIDO: 'El nivel mínimo no puede ser Lleno; elige Poco o Medio.',
  DESPENSA_UBICACION_LARGA: 'La ubicación no puede superar 40 caracteres.',
  DESPENSA_NOMBRE_REQUERIDO: 'Escribe el nombre del producto.',
  DESPENSA_NOMBRE_LARGO: 'El nombre puede tener hasta 120 caracteres.',
  DESPENSA_TIPO_REQUERIDO: 'Elige si el producto se cuenta por unidades o va a granel.',
  DESPENSA_UNIDAD_INVALIDA: 'Lo que se cuenta va en unidades; lo que va a granel, en gramos o mililitros.',
  DESPENSA_PRODUCTO_EXISTE: 'Ya hay un producto llamado así. Búscalo en el catálogo y agrégalo desde ahí.',
  RECURSO_NO_ENCONTRADO: 'Ese producto ya no está en tu despensa.',
}

export function mensajeDespensa(error: unknown): string {
  if (error instanceof ErrorApi && MENSAJES[error.codigo]) {
    return MENSAJES[error.codigo]
  }
  return mensajeDeError(error)
}

/** La usuaria todavía no tiene hogar: hay que llevarla al perfil. */
export function faltaHogar(error: unknown): boolean {
  return error instanceof ErrorApi && error.codigo === 'DESPENSA_HOGAR_REQUERIDO'
}
