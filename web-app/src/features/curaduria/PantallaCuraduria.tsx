import { mensajeDeError } from '../../api/mensajesError'
import { useConsulta } from '../../api/useConsulta'
import { Alerta, Boton, Cargando, EncabezadoPagina } from '../../shared/componentes'

/** Lo que muestra `GET /api/curaduria/productos` (ProductoResponse); solo lo que se usa aquí. */
interface Producto {
  id: number
  nombre: string
  categoria: string
  activo: boolean
}

/** Inicio del curador (rol CURADOR). La edición del catálogo llega con su propia tarea. */
export function PantallaCuraduria() {
  const { estado, recargar } = useConsulta<Producto[]>('/curaduria/productos')

  return (
    <>
      <EncabezadoPagina titulo="Catálogo" detalle="Productos y precios de referencia que ven todas las usuarias." />
      {estado.tipo === 'cargando' && <Cargando texto="Cargando el catálogo…" />}
      {estado.tipo === 'error' && (
        <Alerta tono="error">
          {mensajeDeError(estado.error)}{' '}
          <Boton variante="texto" onClick={recargar}>
            Reintentar
          </Boton>
        </Alerta>
      )}
      {estado.tipo === 'listo' && (
        <p>
          {estado.datos.filter((p) => p.activo).length} productos activos en {new Set(estado.datos.map((p) => p.categoria)).size}{' '}
          categorías.
        </p>
      )}
    </>
  )
}
