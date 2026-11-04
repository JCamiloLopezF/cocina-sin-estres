import { useState } from 'react'
import { GrupoOpciones } from '../../shared/componentes'
import { guardarPreferencias, leerPreferencias, type TamanoLetra } from '../../shared/preferenciasVisuales'

/** «Ver mejor»: tamaño de letra y alto contraste, guardados en este celular o computador. */
export function VerMejor() {
  const [preferencias, setPreferencias] = useState(leerPreferencias)

  function cambiar(cambio: Partial<typeof preferencias>) {
    const nuevas = { ...preferencias, ...cambio }
    setPreferencias(nuevas)
    guardarPreferencias(nuevas)
  }

  return (
    <section className="perfil__bloque" aria-labelledby="titulo-ver-mejor">
      <h2 id="titulo-ver-mejor" className="perfil__subtitulo">
        Ver mejor
      </h2>
      <GrupoOpciones<TamanoLetra>
        leyenda="Tamaño de la letra"
        ayuda="Se guarda en este celular o computador."
        variante="segmentos"
        opciones={[
          { valor: 'normal', texto: 'Normal' },
          { valor: 'grande', texto: 'Grande' },
          { valor: 'muy-grande', texto: 'Muy grande' },
        ]}
        valor={preferencias.letra}
        alCambiar={(letra) => cambiar({ letra })}
      />
      <label className="casilla casilla--tarjeta">
        Alto contraste: bordes y textos más oscuros
        <input
          type="checkbox"
          checked={preferencias.altoContraste}
          onChange={(e) => cambiar({ altoContraste: e.target.checked })}
        />
      </label>
    </section>
  )
}
