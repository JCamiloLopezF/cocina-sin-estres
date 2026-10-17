import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { montarApp } from '../../prueba/montar'
import { crearServicioFalso, LUISA, respuestaJson } from '../../prueba/servicioFalso'
import { ErrorAuth } from './servicioAuth'

describe('Entrar', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn(async () => respuestaJson({ id: 1, correo: LUISA.correo, rol: 'USUARIA', perfilCompleto: true })))
  })

  it('marca los campos vacíos, los asocia a su mensaje y lleva el foco al primero', async () => {
    const { servicio } = crearServicioFalso()
    montarApp(servicio, '/entrar')
    const usuario = userEvent.setup()

    await usuario.click(await screen.findByRole('button', { name: 'Entrar' }))

    const correo = screen.getByLabelText('Correo')
    expect(correo).toHaveAttribute('aria-invalid', 'true')
    expect(correo).toHaveAccessibleDescription('Escribe tu correo.')
    expect(screen.getByLabelText('Contraseña')).toHaveAccessibleDescription('Escribe tu contraseña.')
    await waitFor(() => expect(correo).toHaveFocus())
    expect(servicio.entrar).not.toHaveBeenCalled()
  })

  it('muestra un mensaje claro si las credenciales no coinciden', async () => {
    const { servicio } = crearServicioFalso()
    servicio.entrar.mockRejectedValueOnce(new ErrorAuth('auth/invalid-credential'))
    montarApp(servicio, '/entrar')
    const usuario = userEvent.setup()

    await usuario.type(await screen.findByLabelText('Correo'), 'luisa@correo.com')
    await usuario.type(screen.getByLabelText('Contraseña'), 'equivocada')
    await usuario.click(screen.getByRole('button', { name: 'Entrar' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('El correo o la contraseña no coinciden.')
    expect(servicio.entrar).toHaveBeenCalledWith('luisa@correo.com', 'equivocada')
  })

  it('al entrar lleva a la página que la usuaria quería abrir', async () => {
    const { servicio, cambiar } = crearServicioFalso()
    servicio.entrar.mockImplementationOnce(async () => cambiar(LUISA))
    const { router } = montarApp(servicio, '/perfil')
    const usuario = userEvent.setup()

    await usuario.type(await screen.findByLabelText('Correo'), LUISA.correo)
    await usuario.type(screen.getByLabelText('Contraseña'), 'clave-segura')
    await usuario.click(screen.getByRole('button', { name: 'Entrar' }))

    // Al entrar, la sesión se vuelve a cargar y la pantalla puede montarse dos veces: con
    // findBy el título encontrado a veces ya no está cuando se revisa. waitFor repite la
    // revisión completa hasta que el estado final se mantiene.
    await waitFor(
      () => expect(screen.getByRole('heading', { level: 1, name: 'Perfil' })).toBeInTheDocument(),
      { timeout: 4000 },
    )
    expect(router.state.location.pathname).toBe('/perfil')
  })
})
