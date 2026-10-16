import { EnConstruccion } from '../../shared/componentes'

/** Inicio del administrador (rol ADMIN). La gestión de cuentas y roles llega con su propia tarea. */
export function PantallaAdmin() {
  return (
    <EnConstruccion
      titulo="Cuentas"
      texto="Aquí vas a ver las cuentas, cambiar roles y desactivar cuentas."
    />
  )
}
