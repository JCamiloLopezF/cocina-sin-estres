import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { backendFalso } from '../../prueba/backendFalso'
import { montarApp } from '../../prueba/montar'
import { crearServicioFalso, LUISA, respuestaJson } from '../../prueba/servicioFalso'

function cuentaUsuaria() {
  backendFalso({
    'GET /yo': () => respuestaJson({ id: 7, correo: LUISA.correo, rol: 'USUARIA', perfilCompleto: true }),
  })
}

describe('Confirmar el correo', () => {
  it('avisa mientras el correo no esté confirmado y deja reenviar el enlace', async () => {
    cuentaUsuaria()
    const { servicio } = crearServicioFalso({ ...LUISA, correoVerificado: false })
    const reenviar = vi.fn(async () => undefined)
    montarApp({ ...servicio, reenviarVerificacion: reenviar }, '/perfil')
    const usuario = userEvent.setup()

    expect(await screen.findByText('Confirma tu correo')).toBeInTheDocument()
    expect(screen.getByText(/Te enviamos un enlace a luisa@correo.com/)).toBeInTheDocument()
    await usuario.click(screen.getByRole('button', { name: 'Reenviar el enlace' }))

    expect(reenviar).toHaveBeenCalledOnce()
    expect(await screen.findByText(/Te enviamos el enlace otra vez/)).toBeInTheDocument()
  })

  it('no aparece con el correo confirmado ni en el modo dev', async () => {
    cuentaUsuaria()
    const { servicio } = crearServicioFalso({ ...LUISA, correoVerificado: true })
    montarApp(servicio, '/perfil')

    expect(await screen.findByRole('navigation', { name: 'Menú principal' })).toBeInTheDocument()
    expect(screen.queryByText('Confirma tu correo')).not.toBeInTheDocument()
  })
})
