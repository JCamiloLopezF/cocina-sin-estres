import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { backendFalso } from '../../prueba/backendFalso'
import { montarApp } from '../../prueba/montar'
import { crearServicioFalso, respuestaJson } from '../../prueba/servicioFalso'
import type { CuentaAdmin } from './PantallaAdmin'

const ADMIN: CuentaAdmin = { id: 1, correo: 'admin@correo.com', rol: 'ADMIN', activo: true, perfilCompleto: false }
const LUISA: CuentaAdmin = { id: 7, correo: 'luisa@correo.com', rol: 'USUARIA', activo: true, perfilCompleto: true }

describe('Cuentas y roles (rol ADMIN)', () => {
  it('asigna el rol de curador y desactiva una cuenta; la propia no se puede tocar', async () => {
    const { llamadas } = backendFalso({
      'GET /yo': () => respuestaJson({ id: 1, correo: ADMIN.correo, rol: 'ADMIN', perfilCompleto: false }),
      'GET /admin/cuentas': () => respuestaJson([ADMIN, LUISA]),
      'PUT /admin/cuentas/7': (cuerpo) => respuestaJson({ ...LUISA, ...(cuerpo as object) }),
    })
    montarApp(crearServicioFalso({ uid: 'a', correo: ADMIN.correo, nombre: null }).servicio, '/admin')
    const usuario = userEvent.setup()

    expect(await screen.findByText('1 usuarias · 0 curadores · 1 administradores')).toBeInTheDocument()
    expect(screen.getByRole('combobox', { name: 'Rol de admin@correo.com' })).toBeDisabled()
    expect(screen.queryByRole('button', { name: /admin@correo.com/ })).not.toBeInTheDocument()

    await usuario.selectOptions(screen.getByRole('combobox', { name: 'Rol de luisa@correo.com' }), 'CURADOR')
    expect(await screen.findByText('luisa@correo.com ahora es curador de contenido.')).toBeInTheDocument()
    expect(llamadas.find((l) => l.metodo === 'PUT')?.cuerpo).toEqual({ rol: 'CURADOR' })

    await usuario.click(screen.getByRole('button', { name: 'Desactivar luisa@correo.com' }))
    expect(await screen.findByText(/Desactivamos luisa@correo.com/)).toBeInTheDocument()
    const fila = screen.getByText('luisa@correo.com').closest('li')!
    expect(within(fila).getByText('desactivada')).toBeInTheDocument()
    expect(within(fila).getByRole('button', { name: 'Reactivar luisa@correo.com' })).toBeInTheDocument()
  })

  it('explica el error si el backend rechaza el cambio', async () => {
    backendFalso({
      'GET /yo': () => respuestaJson({ id: 1, correo: ADMIN.correo, rol: 'ADMIN', perfilCompleto: false }),
      'GET /admin/cuentas': () => respuestaJson([ADMIN, LUISA]),
      'PUT /admin/cuentas/7': () => respuestaJson({ status: 409, codigo: 'ADMIN_CUENTA_PROPIA' }, 409),
    })
    montarApp(crearServicioFalso({ uid: 'a', correo: ADMIN.correo, nombre: null }).servicio, '/admin')
    const usuario = userEvent.setup()

    await usuario.click(await screen.findByRole('button', { name: 'Desactivar luisa@correo.com' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('No puedes cambiar tu propio rol')
  })
})
