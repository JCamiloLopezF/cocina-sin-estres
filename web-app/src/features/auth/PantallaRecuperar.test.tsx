import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { montarApp } from '../../prueba/montar'
import { crearServicioFalso } from '../../prueba/servicioFalso'
import { ErrorAuth } from './servicioAuth'

describe('Recuperar contraseña', () => {
  it.each([
    ['la cuenta existe', null],
    ['la cuenta no existe', new ErrorAuth('auth/user-not-found')],
  ])('da la misma confirmación cuando %s, para no revelar qué correos están registrados', async (_caso, error) => {
    const { servicio } = crearServicioFalso()
    if (error) {
      servicio.recuperar.mockRejectedValueOnce(error)
    }
    montarApp(servicio, '/recuperar')
    const usuario = userEvent.setup()

    await usuario.type(await screen.findByLabelText('Correo'), 'alguien@correo.com')
    await usuario.click(screen.getByRole('button', { name: 'Enviar enlace' }))

    expect(await screen.findByRole('status')).toHaveTextContent('Si hay una cuenta con alguien@correo.com')
    expect(servicio.recuperar).toHaveBeenCalledWith('alguien@correo.com')
  })

  it('si no hay internet lo dice y deja el formulario para reintentar', async () => {
    const { servicio } = crearServicioFalso()
    servicio.recuperar.mockRejectedValueOnce(new ErrorAuth('auth/network-request-failed'))
    montarApp(servicio, '/recuperar')
    const usuario = userEvent.setup()

    await usuario.type(await screen.findByLabelText('Correo'), 'alguien@correo.com')
    await usuario.click(screen.getByRole('button', { name: 'Enviar enlace' }))

    expect(await screen.findByRole('alert')).toHaveTextContent('Revisa tu internet')
    expect(screen.getByRole('button', { name: 'Enviar enlace' })).toBeEnabled()
  })
})
