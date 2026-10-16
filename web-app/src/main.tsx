import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { createBrowserRouter, RouterProvider } from 'react-router'
import { ConfiguracionIncompletaError, entorno } from './config/entorno'
import { ProveedorSesion } from './features/auth/ProveedorSesion'
import { crearServicioAuthDev } from './features/auth/servicioAuthDev'
import { crearServicioAuthFirebase } from './features/auth/servicioAuthFirebase'
import type { ServicioAuth } from './features/auth/servicioAuth'
import { crearRutas } from './navegacion/rutas'
import './shared/estilos/base.css'
import './navegacion/diseno.css'

const raiz = createRoot(document.getElementById('root')!)

try {
  const { apiUrl, modoAuth, firebase } = entorno()
  const servicio: ServicioAuth = modoAuth === 'dev' ? crearServicioAuthDev() : crearServicioAuthFirebase(firebase!)
  const router = createBrowserRouter(crearRutas({ modoDev: modoAuth === 'dev' }))

  raiz.render(
    <StrictMode>
      <ProveedorSesion servicio={servicio} urlApi={apiUrl}>
        <RouterProvider router={router} />
      </ProveedorSesion>
    </StrictMode>,
  )
} catch (error) {
  // Pasa al montar el proyecto sin .env.local: mejor decirlo en pantalla que dejarla en blanco.
  const faltantes = error instanceof ConfiguracionIncompletaError ? error.faltantes : [String(error)]
  raiz.render(
    <main className="app__contenido">
      <h1>Falta configuración</h1>
      <p>Copia web-app/.env.example como web-app/.env.local y completa estas variables:</p>
      <ul>
        {faltantes.map((f) => (
          <li key={f}>
            <code>{f}</code>
          </li>
        ))}
      </ul>
    </main>,
  )
}
