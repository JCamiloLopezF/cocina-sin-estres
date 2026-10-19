import { Link } from 'react-router'
import { DisenoAcceso } from './DisenoAcceso'

/** Pantalla 01: la puerta de entrada para quien todavía no ha iniciado sesión. */
export function PantallaBienvenida() {
  return (
    <DisenoAcceso
      titulo="Te damos la bienvenida"
      detalle="Planifica la semana con lo que ya tienes en casa y ajustado a tu presupuesto."
    >
      <div className="bienvenida__acciones">
        <Link className="boton boton--primario boton--ancho" to="/registro">
          Crear mi cuenta
        </Link>
        <Link className="boton boton--secundario boton--ancho" to="/entrar">
          Ya tengo cuenta
        </Link>
        <p className="bienvenida__nota">
          Al continuar aceptas la política de tratamiento de datos personales (Ley 1581 de 2012).
        </p>
      </div>
    </DisenoAcceso>
  )
}
