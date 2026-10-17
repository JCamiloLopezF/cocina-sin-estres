import type { ReactNode } from 'react'

export interface AlertaProps {
  tono: 'error' | 'exito' | 'info'
  titulo?: string
  children: ReactNode
}

/**
 * Mensaje destacado. Los errores usan `role="alert"` para que se anuncien de
 * inmediato; los demás, `role="status"`, que no interrumpe.
 */
export function Alerta({ tono, titulo, children }: AlertaProps) {
  return (
    <div className={`alerta alerta--${tono}`} role={tono === 'error' ? 'alert' : 'status'}>
      {titulo && <strong className="alerta__titulo">{titulo}</strong>}
      {children}
    </div>
  )
}
