import { beforeEach, describe, expect, it, vi } from 'vitest'
import { respuestaJson } from '../prueba/servicioFalso'
import { configurarCliente, ErrorApi, pedir, RESPUESTA_INESPERADA, SIN_CONEXION } from './cliente'
import { mensajeDeError } from './mensajesError'

describe('cliente de la API', () => {
  const fetchFalso = vi.fn<typeof fetch>()

  beforeEach(() => {
    vi.stubGlobal('fetch', fetchFalso)
    configurarCliente({ urlBase: 'https://api.ejemplo.com/api/', credenciales: async () => ({ Authorization: 'Bearer abc' }) })
  })

  it('adjunta las credenciales y manda el cuerpo como JSON', async () => {
    fetchFalso.mockResolvedValue(respuestaJson({ ok: true }))

    const respuesta = await pedir<{ ok: boolean }>('/perfil/hogar', { metodo: 'PUT', cuerpo: { a: 1 } })

    expect(respuesta).toEqual({ ok: true })
    const [url, opciones] = fetchFalso.mock.calls[0]
    expect(url).toBe('https://api.ejemplo.com/api/perfil/hogar')
    expect(opciones?.method).toBe('PUT')
    expect(opciones?.body).toBe('{"a":1}')
    expect(opciones?.headers).toMatchObject({ Authorization: 'Bearer abc', 'Content-Type': 'application/json' })
  })

  it('devuelve undefined cuando la respuesta es 204', async () => {
    fetchFalso.mockResolvedValue(new Response(null, { status: 204 }))
    await expect(pedir('/algo', { metodo: 'DELETE' })).resolves.toBeUndefined()
  })

  it('convierte un Problem Details en ErrorApi con su código', async () => {
    fetchFalso.mockResolvedValue(
      respuestaJson(
        { status: 403, codigo: 'AUTH_SIN_PERMISO', detail: 'No tienes permiso.', errores: [] },
        403,
        'application/problem+json',
      ),
    )

    const error = await pedir('/admin/cuentas').catch((e: unknown) => e)

    expect(error).toBeInstanceOf(ErrorApi)
    expect(error).toMatchObject({ status: 403, codigo: 'AUTH_SIN_PERMISO' })
    expect(mensajeDeError(error)).toBe('Tu cuenta no tiene permiso para ver esto.')
  })

  it('marca como respuesta inesperada un error que no es Problem Details', async () => {
    fetchFalso.mockResolvedValue(new Response('<html>502</html>', { status: 502 }))
    await expect(pedir('/yo')).rejects.toMatchObject({ status: 502, codigo: RESPUESTA_INESPERADA })
  })

  it('avisa que hay que esperar cuando la API pone el límite de peticiones', async () => {
    fetchFalso.mockResolvedValue(
      respuestaJson({ status: 429, codigo: 'LIMITE_PETICIONES', detail: 'Vas muy rápido.' }, 429, 'application/problem+json'),
    )

    const error = await pedir('/planes-semanales/1/generar', { metodo: 'POST' }).catch((e: unknown) => e)

    expect(error).toMatchObject({ status: 429, codigo: 'LIMITE_PETICIONES' })
    expect(mensajeDeError(error)).toMatch(/Espera un minuto/)
  })

  it('reconoce un 429 o un 413 del balanceador aunque no traiga Problem Details', async () => {
    fetchFalso.mockResolvedValueOnce(new Response('<html>429</html>', { status: 429 }))
    fetchFalso.mockResolvedValueOnce(new Response('<html>413</html>', { status: 413 }))

    await expect(pedir('/yo')).rejects.toMatchObject({ status: 429, codigo: 'LIMITE_PETICIONES' })
    const grande = await pedir('/perfil/hogar', { metodo: 'PUT', cuerpo: {} }).catch((e: unknown) => e)
    expect(grande).toMatchObject({ status: 413, codigo: 'CUERPO_DEMASIADO_GRANDE' })
    expect(mensajeDeError(grande)).toMatch(/demasiado grande/)
  })

  it('distingue cuando no hubo conexión', async () => {
    fetchFalso.mockRejectedValue(new TypeError('Failed to fetch'))

    const error = await pedir('/yo').catch((e: unknown) => e)

    expect(error).toMatchObject({ status: 0, codigo: SIN_CONEXION })
    expect(mensajeDeError(error)).toMatch(/Revisa tu internet/)
  })
})
