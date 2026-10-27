import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { backendFalso } from '../../prueba/backendFalso'
import { montarApp } from '../../prueba/montar'
import { crearServicioFalso } from '../../prueba/servicioFalso'

describe('Política de tratamiento de datos (Ley 1581)', () => {
  it('se abre desde la bienvenida sin iniciar sesión y explica la voz y WhatsApp', async () => {
    backendFalso({})
    montarApp(crearServicioFalso(null).servicio, '/bienvenida')
    const usuario = userEvent.setup()

    await usuario.click(await screen.findByRole('link', { name: 'política de tratamiento de datos personales' }))

    expect(await screen.findByRole('heading', { name: 'Política de tratamiento de datos personales' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'La voz del asistente y Manos libres' })).toBeInTheDocument()
    expect(screen.getByText(/el audio lo procesa el servicio de voz de Google/)).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Pedir por WhatsApp' })).toBeInTheDocument()
    expect(screen.getByText(/pedir que los borremos/)).toBeInTheDocument()
  })
})
