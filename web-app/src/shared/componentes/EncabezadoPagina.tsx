import type { ReactNode } from 'react'
import { Link } from 'react-router'

export interface EncabezadoPaginaProps {
  titulo: string
  /** Dato corto que va a la derecha del título, como "24 productos". */
  detalle?: ReactNode
  /** Si el detalle es una frase, va debajo del título y no a la derecha. */
  detalleAbajo?: boolean
  /** Línea pequeña encima del título, como la fecha del inicio. */
  previo?: ReactNode
  /** Enlace "‹" a la izquierda del título para volver (wireframes 19 y 25). */
  volver?: { ruta: string; etiqueta: string }
}

/**
 * Título principal de una pantalla, como la barra superior de los wireframes: título a
 * la izquierda y un dato corto a la derecha. Cada pantalla tiene exactamente un `h1`.
 */
export function EncabezadoPagina({ titulo, detalle, detalleAbajo = false, previo, volver }: EncabezadoPaginaProps) {
  return (
    <header className={detalleAbajo ? 'encabezado encabezado--detalle-abajo' : 'encabezado'}>
      {previo && <p className="encabezado__previo">{previo}</p>}
      <div className="encabezado__fila">
        {volver && (
          <Link className="encabezado__volver" to={volver.ruta} aria-label={volver.etiqueta}>
            ‹
          </Link>
        )}
        <h1 className="encabezado__titulo">{titulo}</h1>
      </div>
      {detalle && <p className="encabezado__detalle">{detalle}</p>}
    </header>
  )
}
