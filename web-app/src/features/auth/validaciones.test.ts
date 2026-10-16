import { describe, expect, it } from 'vitest'
import { mensajeDeErrorAuth } from './mensajesAuth'
import { ErrorAuth } from './servicioAuth'
import { validarContrasenaNueva, validarCorreo, validarNombre } from './validaciones'

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

  it('pide al menos 8 caracteres en la contraseña nueva', () => {
    expect(validarContrasenaNueva('')).toBe('Escribe una contraseña.')
    expect(validarContrasenaNueva('1234567')).toMatch(/al menos 8/)
    expect(validarContrasenaNueva('12345678')).toBeUndefined()
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
