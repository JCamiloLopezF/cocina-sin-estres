import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { montarApp } from '../prueba/montar'
import { crearServicioFalso, LUISA, respuestaJson } from '../prueba/servicioFalso'

function backendResponde(cuenta: object | Response) {
  const fetchFalso = vi.fn(async (url: string | URL | Request, _opciones?: RequestInit) => {
    if (String(url).endsWith('/yo')) {
      return cuenta instanceof Response ? cuenta : respuestaJson(cuenta)
    }
    return respuestaJson({ codigo: 'RECURSO_NO_ENCONTRADO', status: 404 }, 404)
  })
  vi.stubGlobal('fetch', fetchFalso)
  return fetchFalso
}

describe('Navegación por sesión y rol', () => {
  it('sin sesión, cualquier pantalla privada lleva a Entrar', async () => {
    backendResponde({})
    const { servicio } = crearServicioFalso(null)
    const { router } = montarApp(servicio, '/despensa')

    expect(await screen.findByRole('heading', { level: 1, name: 'Entrar' })).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/entrar')
  })

  it('la usuaria ve el menú de cinco pestañas y el aviso de perfil incompleto', async () => {
    const fetchFalso = backendResponde({ id: 1, correo: LUISA.correo, rol: 'USUARIA', perfilCompleto: false })
    const { servicio } = crearServicioFalso(LUISA)
    montarApp(servicio, '/')

    expect(await screen.findByRole('heading', { level: 1, name: 'Hola, Luisa' })).toBeInTheDocument()
    const menu = screen.getByRole('navigation', { name: 'Menú principal' })
    expect(within(menu).getAllByRole('link').map((a) => a.textContent)).toEqual([
      'Inicio',
      'Despensa',
      'Plan',
      'Recetas',
      'Perfil',
    ])
    expect(within(menu).getByRole('link', { name: 'Inicio' })).toHaveAttribute('aria-current', 'page')
    expect(screen.getByText('Falta el perfil de tu hogar')).toBeInTheDocument()
    // La identidad viaja al backend en cada petición.
    expect(fetchFalso.mock.calls[0][1]?.headers).toMatchObject({ Authorization: 'Bearer prueba' })
  })

  it('un curador que abre una pantalla de usuaria termina en su catálogo (RNF-23)', async () => {
    backendResponde({ id: 2, correo: 'curador@correo.com', rol: 'CURADOR', perfilCompleto: false })
    const { servicio } = crearServicioFalso({ uid: 'c', correo: 'curador@correo.com', nombre: null })
    const { router } = montarApp(servicio, '/despensa')

    expect(await screen.findByRole('heading', { level: 1, name: 'Catálogo' })).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/curaduria')
    expect(screen.queryByRole('navigation', { name: 'Menú principal' })).not.toBeInTheDocument()
  })

  it('una usuaria no entra a la zona de administración', async () => {
    backendResponde({ id: 1, correo: LUISA.correo, rol: 'USUARIA', perfilCompleto: true })
    const { servicio } = crearServicioFalso(LUISA)
    const { router } = montarApp(servicio, '/admin')

    expect(await screen.findByRole('heading', { level: 1, name: 'Hola, Luisa' })).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/')
  })

  it('si la cuenta está desactivada lo explica y solo deja cerrar sesión', async () => {
    backendResponde(respuestaJson({ status: 403, codigo: 'AUTH_CUENTA_INACTIVA' }, 403))
    const { servicio } = crearServicioFalso(LUISA)
    montarApp(servicio, '/')
    const usuario = userEvent.setup()

    expect(await screen.findByRole('alert')).toHaveTextContent('Esta cuenta está desactivada')
    expect(screen.queryByRole('button', { name: 'Reintentar' })).not.toBeInTheDocument()

    await usuario.click(screen.getByRole('button', { name: 'Cerrar sesión' }))
    expect(await screen.findByRole('heading', { level: 1, name: 'Entrar' })).toBeInTheDocument()
  })

  it('desde Perfil se cierra la sesión', async () => {
    backendResponde({ id: 1, correo: LUISA.correo, rol: 'USUARIA', perfilCompleto: true })
    const { servicio } = crearServicioFalso(LUISA)
    montarApp(servicio, '/perfil')
    const usuario = userEvent.setup()

    expect(await screen.findByText('Todavía no has llenado el perfil del hogar')).toBeInTheDocument()
    await usuario.click(screen.getByRole('button', { name: 'Cerrar sesión' }))

    expect(await screen.findByRole('heading', { level: 1, name: 'Entrar' })).toBeInTheDocument()
    expect(servicio.salir).toHaveBeenCalled()
  })

  it('una dirección que no existe muestra la página de no encontrada', async () => {
    backendResponde({})
    const { servicio } = crearServicioFalso(null)
    montarApp(servicio, '/no-existe')
    expect(await screen.findByRole('heading', { name: 'Esta página no existe' })).toBeInTheDocument()
  })
})
