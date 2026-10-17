import { Olla } from './Olla'

/** Indicador de carga a pantalla completa, anunciado a lectores de pantalla. */
export function Cargando({ texto = 'Cargando…' }: { texto?: string }) {
  return (
    <div className="cargando" role="status" aria-live="polite">
      <Olla className="cargando__olla" />
      <span>{texto}</span>
    </div>
  )
}
