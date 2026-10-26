import { useEffect, useState } from 'react'
import { Boton } from '../../shared/componentes'
import type { Voz } from './voz'

interface Tarjeta {
  icono: string
  titulo: string
  texto: string
  /** Lo que dice el asistente en voz alta (más corto que el texto). */
  voz: string
}

const TARJETAS: Tarjeta[] = [
  {
    icono: '👩‍🍳',
    titulo: 'Te acompaño paso a paso',
    texto: 'Vamos un paso a la vez, con letra grande. Yo te leo cada paso en voz alta para que no tengas que estar leyendo.',
    voz: '¡Hola! Te acompaño paso a paso y te leo cada paso en voz alta.',
  },
  {
    icono: '✅',
    titulo: 'Cuando termines, toca «Listo»',
    texto: 'Al tocar «Listo» te digo el siguiente paso. Si un paso tiene tiempo, te ofrezco el temporizador y te aviso cuando acabe.',
    voz: 'Cuando termines un paso, toca Listo y te digo el siguiente. Si el paso tiene tiempo, yo te lo cuento.',
  },
  {
    icono: '🗣️',
    titulo: 'Puedes hablarme',
    texto: 'Con «Manos libres» no tienes que tocar la pantalla. Dime: «siguiente», «repite», «atrás» o «temporizador».',
    voz: 'Si activas manos libres, puedes decirme: siguiente, repite, atrás o temporizador.',
  },
  {
    icono: '🔇',
    titulo: '¿Prefieres silencio?',
    texto: 'Toca 🔇 arriba cuando quieras y sigo en silencio. Con 🔊 vuelvo a hablar.',
    voz: 'Si prefieres silencio, toca el botón del parlante. ¡Empecemos!',
  },
]

interface Props {
  voz: Voz
  /** El navegador puede escuchar: se ofrece Manos libres. */
  puedeEscuchar: boolean
  manosLibres: boolean
  alternarManosLibres: () => void
  alTerminar: () => void
}

/**
 * Introducción del Modo cocinar: sale la primera vez (y con el botón «?»). Cuatro
 * tarjetas cortas, leídas en voz alta, y al final se prueban la voz y Manos libres.
 */
export function IntroCocina({ voz, puedeEscuchar, manosLibres, alternarManosLibres, alTerminar }: Props) {
  const [indice, setIndice] = useState(0)
  const tarjeta = TARJETAS[indice]
  const ultima = indice === TARJETAS.length - 1
  const { decir, encendida } = voz

  useEffect(() => {
    decir(tarjeta.voz)
  }, [tarjeta, decir, encendida])

  return (
    <section className="intro-cocina" aria-labelledby="titulo-intro">
      <ol className="intro-cocina__puntos" aria-label={`Tarjeta ${indice + 1} de ${TARJETAS.length}`}>
        {TARJETAS.map((t, i) => (
          <li key={t.titulo} className={i === indice ? 'intro-cocina__punto intro-cocina__punto--actual' : 'intro-cocina__punto'} />
        ))}
      </ol>
      <p className="intro-cocina__icono" aria-hidden="true">
        {tarjeta.icono}
      </p>
      <h1 id="titulo-intro" className="intro-cocina__titulo">
        {tarjeta.titulo}
      </h1>
      <p className="intro-cocina__texto">{tarjeta.texto}</p>

      {ultima && (
        <div className="intro-cocina__ajustes">
          {voz.disponible ? (
            <>
              <Boton variante="secundario" ancho onClick={voz.alternar}>
                {voz.encendida ? '🔊 Voz encendida' : '🔇 Voz apagada'}
              </Boton>
              {voz.encendida && (
                <Boton variante="texto" onClick={() => voz.decir('¡Hola! Así sueno. ¿Me escuchas bien?')}>
                  Probar la voz
                </Boton>
              )}
              {voz.encendida && voz.nombreVoz && (
                <p className="campo__ayuda intro-cocina__voz">
                  Voz: {voz.nombreVoz}.{!voz.vozNatural && consejoVoz() ? ` ${consejoVoz()}` : ''}
                </p>
              )}
            </>
          ) : (
            <p className="campo__ayuda">Este celular no puede hablar, pero puedes cocinar leyendo los pasos.</p>
          )}
          {puedeEscuchar && (
            <label className="intro-cocina__manos-libres">
              <input type="checkbox" checked={manosLibres} onChange={alternarManosLibres} />
              <span>
                <strong>Manos libres</strong>
                <span className="campo__ayuda">
                  Para escucharte, el celular te pedirá permiso para usar el micrófono. Tu voz la procesa el servicio de
                  voz de Google y solo te escucho mientras esta pantalla está abierta.
                </span>
              </span>
            </label>
          )}
        </div>
      )}

      <div className="intro-cocina__acciones">
        {!ultima && (
          <Boton variante="texto" onClick={alTerminar}>
            Saltar
          </Boton>
        )}
        <Boton ancho={ultima} onClick={() => (ultima ? alTerminar() : setIndice(indice + 1))}>
          {ultima ? 'Empezar a cocinar' : 'Siguiente'}
        </Boton>
      </div>
    </section>
  )
}

/**
 * Dónde descargar una voz más natural, según el celular. Las voces "mejoradas" de iPhone
 * y las de Google en Android suenan mucho menos robóticas que las que vienen instaladas.
 */
function consejoVoz(): string | undefined {
  const agente = typeof navigator === 'undefined' ? '' : navigator.userAgent
  if (/iPhone|iPad|Macintosh/.test(agente)) {
    return 'Para que suene más natural, descarga una voz «Mejorada» en Ajustes → Accesibilidad → Contenido leído → Voces → Español.'
  }
  if (/Android/.test(agente)) {
    return 'Para que suene más natural, en Ajustes → Idiomas → Salida de texto a voz elige «Servicios de voz de Google».'
  }
  return undefined
}
