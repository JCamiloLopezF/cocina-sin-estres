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

/**
 * Reglas de una contraseña nueva (ajuste del prototipo 2). Son las mismas que se
 * configuran en la política de contraseñas de Firebase, para que también apliquen en la
 * página de "olvidé mi contraseña", que no es nuestra.
 */
export const REGLAS_CONTRASENA: { texto: string; cumple: (contrasena: string) => boolean }[] = [
  { texto: `Mínimo ${LARGO_MINIMO_CONTRASENA} caracteres`, cumple: (c) => c.length >= LARGO_MINIMO_CONTRASENA },
  { texto: 'Una mayúscula', cumple: (c) => /\p{Lu}/u.test(c) },
  { texto: 'Una minúscula', cumple: (c) => /\p{Ll}/u.test(c) },
  { texto: 'Un número', cumple: (c) => /\d/.test(c) },
  { texto: 'Un carácter especial, como ! # $ % & * ?', cumple: (c) => /[^\p{L}\d\s]/u.test(c) },
]

export function validarContrasenaNueva(contrasena: string): string | undefined {
  if (!contrasena) {
    return 'Escribe una contraseña.'
  }
  const faltan = REGLAS_CONTRASENA.filter((regla) => !regla.cumple(contrasena))
  if (faltan.length) {
    return `A la contraseña le falta: ${faltan.map((r) => r.texto.toLowerCase()).join(', ')}.`
  }
  return undefined
}

export function validarNombre(nombre: string): string | undefined {
  return nombre.trim() ? undefined : 'Escribe cómo te llamas.'
}

export function validarConfirmacion(contrasena: string, confirmacion: string): string | undefined {
  if (!confirmacion) {
    return 'Escribe de nuevo la contraseña.'
  }
  return confirmacion === contrasena ? undefined : 'Las contraseñas no coinciden.'
}
