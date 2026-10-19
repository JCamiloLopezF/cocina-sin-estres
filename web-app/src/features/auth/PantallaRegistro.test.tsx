import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { backendFalso } from '../../prueba/backendFalso'
import { montarApp } from '../../prueba/montar'
import { crearServicioFalso } from '../../prueba/servicioFalso'

describe('Registro (pantalla 02)', () => {
  it('pide confirmar la contraseña y autorizar el tratamiento de datos', async () => {
    backendFalso({})
    const { servicio } = crearServicioFalso(null)
    montarApp(servicio, '/registro')
    const usuario = userEvent.setup()

    await usuario.type(await screen.findByLabelText('Tu nombre'), 'Luisa')
    await usuario.type(screen.getByLabelText('Correo'), 'luisa@correo.com')
    await usuario.type(screen.getByLabelText('Contraseña'), 'cocina2026')
    await usuario.type(screen.getByLabelText('Confirmar contraseña'), 'cocina2025')
    await usuario.click(screen.getByRole('button', { name: 'Crear cuenta' }))

    expect(screen.getByText('Las contraseñas no coinciden.')).toBeInTheDocument()
    expect(screen.getByText('Para crear la cuenta necesitamos tu autorización.')).toBeInTheDocument()
    expect(servicio.registrar).not.toHaveBeenCalled()

    await usuario.clear(screen.getByLabelText('Confirmar contraseña'))
    await usuario.type(screen.getByLabelText('Confirmar contraseña'), 'cocina2026')
    await usuario.click(screen.getByRole('checkbox', { name: /Autorizo el tratamiento/ }))
    await usuario.click(screen.getByRole('button', { name: 'Crear cuenta' }))

    expect(servicio.registrar).toHaveBeenCalledWith('Luisa', 'luisa@correo.com', 'cocina2026')
  })
})
