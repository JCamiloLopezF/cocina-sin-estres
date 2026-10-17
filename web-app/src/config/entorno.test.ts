import { describe, expect, it } from 'vitest'
import { ConfiguracionIncompletaError, leerEntorno } from './entorno'

const FIREBASE = {
  VITE_FIREBASE_API_KEY: 'clave',
  VITE_FIREBASE_AUTH_DOMAIN: 'proyecto.firebaseapp.com',
  VITE_FIREBASE_PROJECT_ID: 'proyecto',
  VITE_FIREBASE_APP_ID: '1:2:web:3',
}

describe('leerEntorno', () => {
  it('usa Firebase por defecto y quita la barra final de la URL', () => {
    const entorno = leerEntorno({ VITE_API_URL: 'https://api.ejemplo.com/api/', ...FIREBASE })
    expect(entorno.modoAuth).toBe('firebase')
    expect(entorno.apiUrl).toBe('https://api.ejemplo.com/api')
    expect(entorno.firebase?.projectId).toBe('proyecto')
  })

  it('en modo dev no pide la configuración de Firebase', () => {
    const entorno = leerEntorno({ VITE_API_URL: 'http://localhost:8080/api', VITE_AUTH_MODO: 'dev' })
    expect(entorno.modoAuth).toBe('dev')
    expect(entorno.firebase).toBeUndefined()
  })

  it('lista todas las variables que faltan', () => {
    try {
      leerEntorno({ VITE_FIREBASE_API_KEY: 'clave' })
      expect.unreachable()
    } catch (error) {
      expect(error).toBeInstanceOf(ConfiguracionIncompletaError)
      expect((error as ConfiguracionIncompletaError).faltantes).toEqual([
        'VITE_API_URL',
        'VITE_FIREBASE_AUTH_DOMAIN',
        'VITE_FIREBASE_PROJECT_ID',
        'VITE_FIREBASE_APP_ID',
      ])
    }
  })

  it('rechaza un modo desconocido', () => {
    expect(() => leerEntorno({ VITE_API_URL: 'x', VITE_AUTH_MODO: 'google' })).toThrow(ConfiguracionIncompletaError)
  })
})
