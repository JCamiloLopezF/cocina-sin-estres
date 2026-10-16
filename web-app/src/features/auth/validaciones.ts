/** Reglas de los formularios de acceso. Devuelven el mensaje de error o `undefined`. */

const PATRON_CORREO = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

/** Largo mínimo de la contraseña. Firebase exige 6; aquí se pide algo más seguro. */
export const LARGO_MINIMO_CONTRASENA = 8

export function validarCorreo(correo: string): string | undefined {
  if (!correo.trim()) {
    return 'Escribe tu correo.'
  }
  if (!PATRON_CORREO.test(correo.trim())) {
    return 'Escribe un correo válido, por ejemplo nombre@correo.com.'
  }
  return undefined
}

export function validarContrasenaNueva(contrasena: string): string | undefined {
  if (!contrasena) {
    return 'Escribe una contraseña.'
  }
  if (contrasena.length < LARGO_MINIMO_CONTRASENA) {
    return `La contraseña debe tener al menos ${LARGO_MINIMO_CONTRASENA} caracteres.`
  }
  return undefined
}

export function validarNombre(nombre: string): string | undefined {
  return nombre.trim() ? undefined : 'Escribe cómo te llamas.'
}
