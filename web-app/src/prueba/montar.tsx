import { render } from '@testing-library/react'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { ProveedorSesion } from '../features/auth/ProveedorSesion'
import type { ServicioAuth } from '../features/auth/servicioAuth'
import { crearRutas } from '../navegacion/rutas'

/** Monta la aplicación completa en una ruta, con el proveedor de identidad que se le pase. */
export function montarApp(servicio: ServicioAuth, ruta: string, { modoDev = false } = {}) {
  const router = createMemoryRouter(crearRutas({ modoDev }), { initialEntries: [ruta] })
  const resultado = render(
    <ProveedorSesion servicio={servicio} urlApi="http://api.prueba/api">
      <RouterProvider router={router} />
    </ProveedorSesion>,
  )
  return { ...resultado, router }
}
