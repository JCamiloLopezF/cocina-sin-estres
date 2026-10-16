import { useId, type InputHTMLAttributes } from 'react'

export interface CampoTextoProps extends Omit<InputHTMLAttributes<HTMLInputElement>, 'id'> {
  etiqueta: string
  /** Texto de apoyo debajo de la etiqueta, por ejemplo el formato esperado. */
  ayuda?: string
  /** Mensaje de error. Si existe, el campo queda marcado como inválido. */
  error?: string
}

/**
 * Campo de texto con etiqueta visible, ayuda y error asociados al control, para que
 * los lectores de pantalla los anuncien (RNF-12, WCAG 1.3.1 y 3.3.1).
 */
export function CampoTexto({ etiqueta, ayuda, error, ...control }: CampoTextoProps) {
  const id = useId()
  const idAyuda = `${id}-ayuda`
  const idError = `${id}-error`
  const descritoPor = [ayuda && idAyuda, error && idError].filter(Boolean).join(' ') || undefined

  return (
    <div className="campo">
      <label className="campo__etiqueta" htmlFor={id}>
        {etiqueta}
      </label>
      {ayuda && (
        <span className="campo__ayuda" id={idAyuda}>
          {ayuda}
        </span>
      )}
      <input
        {...control}
        id={id}
        className="campo__control"
        aria-invalid={error ? true : undefined}
        aria-describedby={descritoPor}
      />
      {error && (
        <span className="campo__error" id={idError}>
          {error}
        </span>
      )}
    </div>
  )
}
