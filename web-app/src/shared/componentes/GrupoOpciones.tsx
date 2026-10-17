import { useId } from 'react'

export interface Opcion<T extends string> {
  valor: T
  texto: string
}

export interface GrupoOpcionesProps<T extends string> {
  leyenda: string
  ayuda?: string
  opciones: Opcion<T>[]
  valor: T | undefined
  alCambiar: (valor: T) => void
  error?: string
  /** Nombre del grupo de radios; por defecto se genera uno. */
  nombre?: string
  /** Píldoras que se acomodan en filas (por defecto) o segmentos iguales en una fila. */
  variante?: 'pildoras' | 'segmentos'
  /** La leyenda solo la leen los lectores de pantalla (el contexto ya se ve arriba). */
  leyendaOculta?: boolean
}

/**
 * Elegir una opción entre pocas (nivel, periodicidad…). Son radios de verdad dentro de un
 * fieldset, así que se manejan con teclado y los lectores de pantalla anuncian el grupo.
 */
export function GrupoOpciones<T extends string>({
  leyenda,
  ayuda,
  opciones,
  valor,
  alCambiar,
  error,
  nombre,
  variante = 'pildoras',
  leyendaOculta = false,
}: GrupoOpcionesProps<T>) {
  const id = useId()
  const nombreGrupo = nombre ?? id
  const descritoPor = [ayuda && `${id}-ayuda`, error && `${id}-error`].filter(Boolean).join(' ') || undefined

  return (
    <fieldset
      className={variante === 'segmentos' ? 'opciones opciones--segmentos' : 'opciones'}
      aria-describedby={descritoPor} aria-invalid={error ? true : undefined}>
      <legend className={leyendaOculta ? 'solo-lectores' : 'campo__etiqueta'}>{leyenda}</legend>
      {ayuda && (
        <span className="campo__ayuda" id={`${id}-ayuda`}>
          {ayuda}
        </span>
      )}
      <div className="opciones__lista">
        {opciones.map((opcion) => (
          <label key={opcion.valor} className="opciones__opcion">
            <input
              type="radio"
              name={nombreGrupo}
              value={opcion.valor}
              checked={valor === opcion.valor}
              onChange={() => alCambiar(opcion.valor)}
            />
            <span>{opcion.texto}</span>
          </label>
        ))}
      </div>
      {error && (
        <span className="campo__error" id={`${id}-error`}>
          {error}
        </span>
      )}
    </fieldset>
  )
}
