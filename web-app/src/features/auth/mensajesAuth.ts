import { ErrorAuth } from './servicioAuth'

/**
 * Mensajes en español para los códigos de error de Firebase Auth.
 *
 * Correo inexistente y contraseña equivocada dan el mismo mensaje a propósito: así
 * nadie puede averiguar qué correos tienen cuenta.
 */
const MENSAJES: Record<string, string> = {
  'auth/invalid-credential': 'El correo o la contraseña no coinciden.',
  'auth/wrong-password': 'El correo o la contraseña no coinciden.',
  'auth/user-not-found': 'El correo o la contraseña no coinciden.',
  'auth/invalid-email': 'Escribe un correo válido, por ejemplo nombre@correo.com.',
  'auth/missing-password': 'Escribe tu contraseña.',
  'auth/email-already-in-use': 'Ya existe una cuenta con este correo. Entra o recupera tu contraseña.',
  'auth/weak-password': 'La contraseña debe tener al menos 8 caracteres.',
  'auth/password-does-not-meet-requirements': 'La contraseña debe tener al menos 8 caracteres.',
  'auth/too-many-requests': 'Hubo demasiados intentos. Espera unos minutos y vuelve a intentarlo.',
  'auth/user-disabled': 'Esta cuenta está desactivada.',
  'auth/network-request-failed': 'No pudimos conectarnos. Revisa tu internet e inténtalo de nuevo.',
}

const GENERICO = 'No pudimos completar la acción. Inténtalo de nuevo.'

/** Texto para la usuaria a partir de un error del inicio de sesión. */
export function mensajeDeErrorAuth(error: unknown): string {
  if (error instanceof ErrorAuth) {
    return MENSAJES[error.codigo] ?? GENERICO
  }
  return GENERICO
}
