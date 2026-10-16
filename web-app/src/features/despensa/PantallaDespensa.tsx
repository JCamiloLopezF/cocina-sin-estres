import { EncabezadoPagina, EstadoVacio } from '../../shared/componentes'

/**
 * Pantalla 15: despensa vacía. El listado, la carga rápida de básicos y el alta de
 * productos llegan con KAN-15, junto con sus endpoints.
 */
export function PantallaDespensa() {
  return (
    <>
      <EncabezadoPagina titulo="Mi despensa" />
      <EstadoVacio
        titulo="Aún no hay nada aquí"
        texto="Registra lo que tienes en casa para que podamos sugerirte recetas y armar tu lista de compras. Muy pronto podrás agregar tus productos desde esta pantalla."
      />
    </>
  )
}
