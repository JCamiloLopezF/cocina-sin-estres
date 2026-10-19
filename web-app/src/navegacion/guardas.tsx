import { Navigate, Outlet, useLocation } from 'react-router'
import { mensajeDeError } from '../api/mensajesError'
import { Alerta, Boton, Cargando } from '../shared/componentes'
import { esErrorDeCuenta, useSesion, type Rol } from '../features/auth/contextoSesion'
import { inicioDelRol } from './inicioDelRol'

interface EstadoNavegacion {
  desde?: string
}

/** Rutas de acceso: si ya hay sesión, lleva a donde iba o al inicio de su rol. */
export function RutaPublica() {
  const { estado } = useSesion()
  const ubicacion = useLocation()

  if (estado.tipo === 'cargando') {
    return <Cargando />
  }
  if (estado.tipo === 'con-sesion') {
    const desde = (ubicacion.state as EstadoNavegacion | null)?.desde
    return <Navigate to={desde ?? inicioDelRol(estado.cuenta.rol)} replace />
  }
  return <Outlet />
}

/** Rutas que piden sesión. Sin sesión, manda a entrar y recuerda a dónde iba. */
export function RutaPrivada() {
  const { estado, salir, recargarCuenta } = useSesion()
  const ubicacion = useLocation()

  if (estado.tipo === 'cargando') {
    return <Cargando />
  }
  if (estado.tipo === 'sin-sesion') {
    // Quien llega a la raíz sin sesión ve la bienvenida (wireframe 01); a cualquier otra
    // ruta, se le pide entrar y se recuerda a dónde iba.
    if (ubicacion.pathname === '/') {
      return <Navigate to="/bienvenida" replace />
    }
    return <Navigate to="/entrar" replace state={{ desde: ubicacion.pathname } satisfies EstadoNavegacion} />
  }
  if (estado.tipo === 'error') {
    return (
      <main className="app__contenido">
        <Alerta tono="error" titulo="No pudimos abrir tu cuenta">
          {mensajeDeError(estado.error)}
        </Alerta>
        <div style={{ display: 'grid', gap: 'var(--espacio-3)', marginTop: 'var(--espacio-5)' }}>
          {!esErrorDeCuenta(estado.error) && (
            <Boton ancho onClick={() => void recargarCuenta()}>
              Reintentar
            </Boton>
          )}
          <Boton variante="secundario" ancho onClick={() => void salir()}>
            Cerrar sesión
          </Boton>
        </div>
      </main>
    )
  }
  return <Outlet />
}

/** Deja pasar solo a los roles indicados; a los demás los lleva a su propio inicio (RNF-23). */
export function RequiereRol({ roles }: { roles: Rol[] }) {
  const { estado } = useSesion()
  if (estado.tipo !== 'con-sesion') {
    return null
  }
  if (!roles.includes(estado.cuenta.rol)) {
    return <Navigate to={inicioDelRol(estado.cuenta.rol)} replace />
  }
  return <Outlet />
}
