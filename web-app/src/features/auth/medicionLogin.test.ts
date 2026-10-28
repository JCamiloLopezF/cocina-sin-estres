import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { configurarCliente } from '../../api/cliente'
import { backendFalso } from '../../prueba/backendFalso'
import { empezarIntento, intentoFallido, reportarInicioSesion } from './medicionLogin'

describe('Medición del inicio de sesión (KAN-54)', () => {
  beforeEach(() => {
    sessionStorage.clear()
    localStorage.clear()
    configurarCliente({ urlBase: 'https://api.ejemplo.com/api/', credenciales: async () => ({}) })
  })
  afterEach(() => {
    sessionStorage.clear()
    localStorage.clear()
  })

  it('manda cuánto tardó y cuántos intentos fallaron antes, y empieza de cero', async () => {
    const { llamadas } = backendFalso({ 'POST /metricas/inicio-sesion': () => new Response(null, { status: 204 }) })

    empezarIntento(1_000)
    intentoFallido()
    empezarIntento(5_000)
    await reportarInicioSesion(5_850)

    expect(llamadas).toHaveLength(1)
    expect(llamadas[0].cuerpo).toEqual({ duracionMs: 850, fallidosPrevios: 1 })

    // Abrir la app con la sesión guardada no es un inicio de sesión: no se manda nada.
    await reportarInicioSesion(9_000)
    expect(llamadas).toHaveLength(1)
  })

  it('si la API no la recibe, no interrumpe la entrada', async () => {
    backendFalso({ 'POST /metricas/inicio-sesion': () => new Response(null, { status: 500 }) })
    empezarIntento(1_000)

    await expect(reportarInicioSesion(2_000)).resolves.toBeUndefined()
  })
})
