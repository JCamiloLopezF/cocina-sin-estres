import { EncabezadoPagina } from './EncabezadoPagina'
import { EstadoVacio } from './EstadoVacio'

/** Pantalla que ya tiene lugar en la navegación pero todavía no está construida. */
export function EnConstruccion({ titulo, texto }: { titulo: string; texto: string }) {
  return (
    <>
      <EncabezadoPagina titulo={titulo} />
      <EstadoVacio titulo="Muy pronto" texto={texto} />
    </>
  )
}
