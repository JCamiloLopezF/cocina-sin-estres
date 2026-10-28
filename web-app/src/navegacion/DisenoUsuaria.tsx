import { NavLink, Outlet } from 'react-router'
import { AvisoVerificarCorreo } from '../features/auth/AvisoVerificarCorreo'
import { useCuenta } from '../features/auth/contextoSesion'
import { IconoDespensa, IconoInicio, IconoPerfil, IconoPlan, IconoRecetas } from './IconosMenu'
import './diseno.css'

const PESTANAS = [
  { ruta: '/', texto: 'Inicio', Icono: IconoInicio },
  { ruta: '/despensa', texto: 'Despensa', Icono: IconoDespensa },
  { ruta: '/plan', texto: 'Plan', Icono: IconoPlan },
  { ruta: '/recetas', texto: 'Recetas', Icono: IconoRecetas },
  { ruta: '/perfil', texto: 'Perfil', Icono: IconoPerfil },
]

/** Marco de la usuaria (rol USUARIA): la pantalla actual y el menú de cinco pestañas abajo. */
export function DisenoUsuaria() {
  // Mientras arma el perfil por primera vez no hay menú: dejaba salir a medias (auditoría).
  const { perfilCompleto } = useCuenta()
  return (
    <div className={perfilCompleto ? 'app' : 'app app--sin-menu'}>
      <a className="saltar" href="#contenido">
        Saltar al contenido
      </a>
      <main id="contenido" className="app__contenido" tabIndex={-1}>
        <AvisoVerificarCorreo />
        <Outlet />
      </main>
      {perfilCompleto && (
        <nav className="menu" aria-label="Menú principal">
          <ul className="menu__lista">
            {PESTANAS.map(({ ruta, texto, Icono }) => (
              <li key={ruta}>
                <NavLink to={ruta} end={ruta === '/'} className="menu__enlace">
                  <Icono />
                  {texto}
                </NavLink>
              </li>
            ))}
          </ul>
        </nav>
      )}
    </div>
  )
}
