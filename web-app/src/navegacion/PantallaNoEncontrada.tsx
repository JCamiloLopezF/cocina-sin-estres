import { Link } from 'react-router'
import { EstadoVacio } from '../shared/componentes'

/** Cualquier dirección que no existe. */
export function PantallaNoEncontrada() {
  return (
    <main className="app__contenido">
      <EstadoVacio
        titulo="Esta página no existe"
        texto="Puede que el enlace esté incompleto o que la página haya cambiado de lugar."
        acciones={<Link to="/">Ir al inicio</Link>}
      />
    </main>
  )
}
