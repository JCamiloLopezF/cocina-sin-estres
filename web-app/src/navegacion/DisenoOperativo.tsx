import { Outlet } from 'react-router'
import { useSesion } from '../features/auth/contextoSesion'
import { Boton, MarcaCocina } from '../shared/componentes'
import './diseno.css'

/** Marco de los roles operativos (CURADOR y ADMIN): sin menú de hogar, con salida visible. */
export function DisenoOperativo() {
  const { salir } = useSesion()
  return (
    <div className="app app--sin-menu">
      <header className="app__barra-superior">
        <MarcaCocina />
        <Boton variante="texto" onClick={() => void salir()}>
          Cerrar sesión
        </Boton>
      </header>
      <main id="contenido" className="app__contenido">
        <Outlet />
      </main>
    </div>
  )
}
