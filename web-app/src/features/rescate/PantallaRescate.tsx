import { useEffect, useState } from 'react'
import { Link } from 'react-router'
import { Alerta, Boton, Cargando, EncabezadoPagina, EstadoVacio, GrupoOpciones } from '../../shared/componentes'
import { mensajePlan, planSinHogar } from '../planificador/mensajesPlan'
import { PrimeroElHogar } from '../planificador/PantallaPlan'
import { cuando, opcionesRescate, type OpcionRescate } from './apiRescate'
import './rescate.css'

type Tiempo = 'SIN_LIMITE' | '20' | '40'

const TIEMPOS: { valor: Tiempo; texto: string }[] = [
  { valor: '20', texto: '20 min' },
  { valor: '40', texto: '40 min' },
  { valor: 'SIN_LIMITE', texto: 'Sin afán' },
]

type Estado = { tipo: 'cargando' } | { tipo: 'listo'; opciones: OpcionRescate[] } | { tipo: 'error'; error: unknown }

/**
 * Modo rescate (KAN-25): un imprevisto y no hay tiempo de ir a la tienda. Muestra lo que
 * se puede cocinar hoy sin comprar nada, primero lo que salva lo que está por dañarse.
 */
export function PantallaRescate() {
  const [tiempo, setTiempo] = useState<Tiempo>('SIN_LIMITE')
  const [estado, setEstado] = useState<Estado>({ tipo: 'cargando' })
  const [version, setVersion] = useState(0)

  useEffect(() => {
    const control = new AbortController()
    opcionesRescate(tiempo === 'SIN_LIMITE' ? undefined : Number(tiempo), control.signal)
      .then((r) => setEstado({ tipo: 'listo', opciones: r.opciones }))
      .catch((error: unknown) => {
        if (!control.signal.aborted) setEstado({ tipo: 'error', error })
      })
    return () => control.abort()
  }, [tiempo, version])

  function elegirTiempo(valor: Tiempo) {
    setEstado({ tipo: 'cargando' })
    setTiempo(valor)
  }

  return (
    <>
      <EncabezadoPagina
        titulo="¿Qué cocino hoy?"
        detalle="Solo con lo que tienes en casa, sin comprar nada."
        detalleAbajo
        volver={{ ruta: '/', etiqueta: 'Volver al inicio' }}
      />
      <div className="rescate__tiempo">
        <GrupoOpciones<Tiempo>
          leyenda="¿Cuánto tiempo tienes?"
          variante="segmentos"
          opciones={TIEMPOS}
          valor={tiempo}
          alCambiar={elegirTiempo}
        />
      </div>

      {estado.tipo === 'cargando' && <Cargando texto="Revisando tu despensa…" />}
      {estado.tipo === 'error' &&
        (planSinHogar(estado.error) ? (
          <PrimeroElHogar />
        ) : (
          <Alerta tono="error">
            {mensajePlan(estado.error)}{' '}
            <Boton variante="texto" onClick={() => setVersion((v) => v + 1)}>
              Reintentar
            </Boton>
          </Alerta>
        ))}

      {estado.tipo === 'listo' &&
        (estado.opciones.length === 0 ? (
          <EstadoVacio
            titulo={tiempo === 'SIN_LIMITE' ? 'Con lo que hay no alcanza para una receta' : 'Nada tan rápido con lo que hay'}
            texto={
              tiempo === 'SIN_LIMITE'
                ? 'Ninguna receta completa sale con lo que tienes en la despensa. Revisa si falta registrar algo o mira las recetas a las que les falta poco.'
                : 'Prueba con un poco más de tiempo: puede que sí alcance para una receta.'
            }
            acciones={
              tiempo === 'SIN_LIMITE' ? (
                <>
                  <Link className="boton boton--primario boton--ancho" to="/despensa">
                    Revisar mi despensa
                  </Link>
                  <Link className="boton boton--secundario boton--ancho" to="/recetas">
                    Ver el recetario
                  </Link>
                </>
              ) : (
                <Boton ancho variante="secundario" onClick={() => elegirTiempo('SIN_LIMITE')}>
                  Ver todas
                </Boton>
              )
            }
          />
        ) : (
          <>
            <p className="rescate__conteo" aria-live="polite">
              {estado.opciones.length === 1
                ? 'Te sale 1 receta con lo que tienes.'
                : `Te salen ${estado.opciones.length} recetas con lo que tienes.`}
            </p>
            <ul className="rescate__lista">
              {estado.opciones.map((opcion, i) => (
                <li key={opcion.recetaId}>
                  <TarjetaRescate opcion={opcion} destacada={i === 0 && opcion.aprovecha.length > 0} />
                </li>
              ))}
            </ul>
          </>
        ))}
    </>
  )
}

function TarjetaRescate({ opcion, destacada }: { opcion: OpcionRescate; destacada: boolean }) {
  const titulo = `rescate-${opcion.recetaId}`
  return (
    <article className={destacada ? 'opcion-rescate opcion-rescate--destacada' : 'opcion-rescate'} aria-labelledby={titulo}>
      {destacada && <p className="opcion-rescate__marca">La que más te conviene hoy</p>}
      <h2 id={titulo} className="opcion-rescate__nombre">
        {opcion.nombre}
      </h2>
      <p className="opcion-rescate__datos">
        {opcion.tiempoMinutos} min
        {opcion.estado !== 'APTA' && ' · con un ajuste por alergias'}
      </p>
      {opcion.aprovecha.length > 0 && (
        <ul className="opcion-rescate__aprovecha" aria-label="Aprovecha">
          {opcion.aprovecha.map((p) => (
            <li key={p.productoId} className="opcion-rescate__pildora">
              {p.nombre} · vence {cuando(p.dias)}
            </li>
          ))}
        </ul>
      )}
      <div className="opcion-rescate__acciones">
        <Link className="boton boton--secundario" to={`/recetas/${opcion.recetaId}`}>
          Ver receta
        </Link>
        <Link className="boton boton--primario" to={`/recetas/${opcion.recetaId}/cocinar`}>
          Cocinar ahora
        </Link>
      </div>
    </article>
  )
}
