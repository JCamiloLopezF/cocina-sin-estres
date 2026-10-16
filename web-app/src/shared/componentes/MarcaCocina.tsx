import { Link } from 'react-router'
import { Olla } from './Olla'

/** Nombre de la aplicación con su ícono; lleva al inicio. */
export function MarcaCocina() {
  return (
    <Link to="/" className="marca">
      <Olla className="marca__olla" />
      <span className="marca__nombre">Cocina sin estrés</span>
    </Link>
  )
}
