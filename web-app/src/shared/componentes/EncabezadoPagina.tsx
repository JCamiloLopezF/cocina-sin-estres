import type { ReactNode } from 'react'

/** Título principal de una pantalla. Cada pantalla tiene exactamente un `h1`. */
export function EncabezadoPagina({ titulo, detalle }: { titulo: string; detalle?: ReactNode }) {
  return (
    <header className="encabezado">
      <h1 className="encabezado__titulo">{titulo}</h1>
      {detalle && <p className="encabezado__detalle">{detalle}</p>}
    </header>
  )
}
