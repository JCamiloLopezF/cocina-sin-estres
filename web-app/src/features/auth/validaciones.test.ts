import { describe, expect, it } from 'vitest'
import { mensajeDeErrorAuth } from './mensajesAuth'
import { ErrorAuth } from './servicioAuth'
import { validarConfirmacion, validarContrasenaNueva, validarCorreo, validarNombre } from './validaciones'

describe('validaciones de acceso', () => {
  it.each([
    ['', 'Escribe tu correo.'],
    ['   ', 'Escribe tu correo.'],
    ['luisa', 'Escribe un correo válido, por ejemplo nombre@correo.com.'],
    ['luisa@correo', 'Escribe un correo válido, por ejemplo nombre@correo.com.'],
    [' luisa@correo.com ', undefined],
  ])('correo %j → %j', (correo, esperado) => {
    expect(validarCorreo(correo)).toBe(esperado)
  })

  it('la contraseña nueva exige 8 caracteres, mayúscula, minúscula, número y carácter especial', () => {
    expect(validarContrasenaNueva('')).toBe('Escribe una contraseña.')
    expect(validarContrasenaNueva('cocina2026')).toBe(
      'A la contraseña le falta: una mayúscula, un carácter especial, como ! # $ % & * ?.',
    )
    expect(validarContrasenaNueva('Cocina#')).toMatch(/mínimo 8 caracteres, un número/)
    expect(validarContrasenaNueva('Cocina#2026')).toBeUndefined()
    // Las letras con tilde cuentan como letras, no como carácter especial.
    expect(validarContrasenaNueva('Ñandú2026')).toMatch(/carácter especial/)
    expect(validarContrasenaNueva('Ñandú-2026')).toBeUndefined()
  })

  it('la confirmación tiene que ser igual a la contraseña', () => {
    expect(validarConfirmacion('Cocina#2026', 'Cocina#2025')).toBe('Las contraseñas no coinciden.')
    expect(validarConfirmacion('Cocina#2026', 'Cocina#2026')).toBeUndefined()
  })

  it('pide el nombre', () => {
    expect(validarNombre('  ')).toBeDefined()
    expect(validarNombre('Luisa')).toBeUndefined()
  })
})

describe('mensajes de Firebase Auth', () => {
  it('no revela si el correo existe: usuario inexistente y contraseña errada dicen lo mismo', () => {
    const inexistente = mensajeDeErrorAuth(new ErrorAuth('auth/user-not-found'))
    expect(mensajeDeErrorAuth(new ErrorAuth('auth/wrong-password'))).toBe(inexistente)
    expect(mensajeDeErrorAuth(new ErrorAuth('auth/invalid-credential'))).toBe(inexistente)
  })

  it('da un mensaje genérico para códigos desconocidos', () => {
    expect(mensajeDeErrorAuth(new ErrorAuth('auth/algo-nuevo'))).toMatch(/Inténtalo de nuevo/)
    expect(mensajeDeErrorAuth(new Error('x'))).toMatch(/Inténtalo de nuevo/)
  })
})
