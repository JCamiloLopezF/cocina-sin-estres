import { NavLink } from 'react-router'

/** Las dos pantallas del administrador. */
export function NavAdmin() {
  const clase = ({ isActive }: { isActive: boolean }) => (isActive ? 'nav-admin__enlace nav-admin__enlace--actual' : 'nav-admin__enlace')
  return (
    <nav className="nav-admin" aria-label="Administración">
      <NavLink to="/admin/analisis" className={clase}>
        Análisis
      </NavLink>
      <NavLink to="/admin" end className={clase}>
        Cuentas
      </NavLink>
    </nav>
  )
}
