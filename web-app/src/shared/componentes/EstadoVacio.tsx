import type { ReactNode } from 'react'

export interface EstadoVacioProps {
  titulo: string
  texto: string
  /** Botones o enlaces para salir del estado vacío. */
  acciones?: ReactNode
}

/** Lo que se muestra cuando una lista todavía no tiene nada: explica qué hacer. */
export function EstadoVacio({ titulo, texto, acciones }: EstadoVacioProps) {
  return (
    <section className="vacio">
      <h2 className="vacio__titulo">{titulo}</h2>
      <p className="vacio__texto">{texto}</p>
      {acciones && <div className="vacio__acciones">{acciones}</div>}
    </section>
  )
}
