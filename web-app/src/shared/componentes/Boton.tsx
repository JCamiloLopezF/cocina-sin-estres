import type { ButtonHTMLAttributes, ReactNode } from 'react'

export interface BotonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variante?: 'primario' | 'secundario' | 'texto'
  /** Ocupa todo el ancho disponible. */
  ancho?: boolean
  /** Muestra un indicador y deshabilita el botón mientras se completa la acción. */
  cargando?: boolean
  children: ReactNode
}

/** Botón de la aplicación. Por defecto es `type="button"` para no enviar formularios por error. */
export function Boton({
  variante = 'primario',
  ancho = false,
  cargando = false,
  disabled,
  type = 'button',
  className,
  children,
  ...resto
}: BotonProps) {
  const clases = ['boton', `boton--${variante}`, ancho && 'boton--ancho', className].filter(Boolean).join(' ')
  return (
    <button {...resto} type={type} className={clases} disabled={disabled || cargando} aria-busy={cargando || undefined}>
      {cargando && <span className="boton__giro" aria-hidden="true" />}
      {children}
    </button>
  )
}
