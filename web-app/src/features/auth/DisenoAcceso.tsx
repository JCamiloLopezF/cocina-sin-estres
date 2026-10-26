import type { ReactNode } from 'react'
import { Olla } from '../../shared/componentes'
import './auth.css'

interface Props {
  titulo: string
  detalle?: string
  children: ReactNode
}

/** Marco común de las pantallas de acceso: portada con la marca y el formulario debajo. */
export function DisenoAcceso({ titulo, detalle, children }: Props) {
  return (
    <div className="acceso">
      <div className="acceso__portada">
        <div className="acceso__portada-interior">
          {/* Dos líneas en pantalla; los lectores de pantalla lo leen seguido. Un <p> no admite aria-label. */}
          <p className="acceso__nombre">
            <span>Cocina</span> <span>sin estrés</span>
          </p>
          <p className="acceso__lema">Lo que hay en tu despensa, lo que vas a cocinar y lo que cuesta, en un solo lugar.</p>
          <Olla className="acceso__vapor" />
        </div>
      </div>
      <main className="acceso__cuerpo">
        <div className="acceso__cuerpo-interior">
          <h1 className="acceso__titulo">{titulo}</h1>
          {detalle && <p className="acceso__detalle">{detalle}</p>}
          {children}
        </div>
      </main>
    </div>
  )
}
