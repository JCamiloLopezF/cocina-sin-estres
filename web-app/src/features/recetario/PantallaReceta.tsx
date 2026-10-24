import { useEffect, useState } from 'react'
import { Link, useParams, useSearchParams } from 'react-router'
import { Alerta, Boton, Cargando, EncabezadoPagina } from '../../shared/componentes'
import { VideoReceta } from '../cocinar/VideoReceta'
import { cantidadConUnidad, enPesos } from '../despensa/formato'
import { mensajePlan } from '../planificador/mensajesPlan'
import { diaDe } from '../planificador/semana'
import { obtenerAdaptaciones, textoCambio, type Adaptacion } from './adaptaciones'
import { obtenerReceta } from './apiRecetario'
import { analizarReceta, cargarCocina, type Cocina } from './cocina'
import type { RecetaDetalle } from './tipos'
import './recetario.css'

type Estado =
  | { tipo: 'cargando' }
  | { tipo: 'listo'; receta: RecetaDetalle; cocina: Cocina; adaptacion?: Adaptacion }
  | { tipo: 'error'; error: unknown }

/** Pantalla 23: detalle de la receta, con lo que hay y lo que falta en la despensa. */
export function PantallaReceta() {
  const { id } = useParams()
  const [parametros] = useSearchParams()
  const [estado, setEstado] = useState<Estado>({ tipo: 'cargando' })
  const [version, setVersion] = useState(0)
  const [porciones, setPorciones] = useState<number>()
  const [hoyDia] = useState(() => diaDe(new Date()))

  useEffect(() => {
    const control = new AbortController()
    Promise.all([
      obtenerReceta(Number(id), control.signal),
      cargarCocina(control.signal),
      obtenerAdaptaciones([Number(id)], control.signal),
    ])
      .then(([receta, cocina, adaptaciones]) =>
        setEstado({ tipo: 'listo', receta, cocina, adaptacion: adaptaciones.get(Number(id)) }),
      )
      .catch((error: unknown) => {
        if (!control.signal.aborted) {
          setEstado({ tipo: 'error', error })
        }
      })
    return () => control.abort()
  }, [id, version])

  const consulta = parametros.toString() ? `?${parametros.toString()}` : ''
  const volver = { ruta: `/recetas${consulta}`, etiqueta: 'Volver al recetario' }

  if (estado.tipo !== 'listo') {
    return (
      <>
        <EncabezadoPagina titulo="Receta" volver={volver} />
        {estado.tipo === 'cargando' ? (
          <Cargando texto="Cargando la receta…" />
        ) : (
          <Alerta tono="error">
            {mensajePlan(estado.error)}{' '}
            <Boton variante="texto" onClick={() => setVersion((v) => v + 1)}>
              Reintentar
            </Boton>
          </Alerta>
        )}
      </>
    )
  }

  const { receta, cocina, adaptacion } = estado
  const elegidas = porciones ?? receta.porciones
  const vista = analizarReceta(receta, cocina, elegidas)
  const hoy = new URLSearchParams(parametros)
  hoy.set('dia', String(hoyDia))
  hoy.delete('comida')

  return (
    <>
      <EncabezadoPagina titulo={receta.nombre} volver={volver} />
      {receta.descripcion && <p className="receta__descripcion">{receta.descripcion}</p>}

      <ul className="receta__datos" aria-label="Resumen">
        <li className="pildora">{receta.tiempoMinutos} min</li>
        <li className="pildora">{receta.ingredientes.length} ingredientes</li>
        {vista.costoPorcion !== undefined && (
          <li className="pildora pildora--llena">{enPesos(vista.costoPorcion)} / porción</li>
        )}
      </ul>

      {adaptacion ? (
        adaptacion.cambios.length > 0 && (
          <section className="receta__adaptaciones" aria-label="Ajustes por alergias">
            {adaptacion.cambios.map((cambio, i) => (
              <div key={i} className="receta__aviso">
                <Alerta tono={cambio.tipo === 'NO_APTA' ? 'error' : 'info'}>
                  {textoCambio(cambio)}{' '}
                  {cambio.recetaAparteId && (
                    <Link to={`/recetas/${cambio.recetaAparteId}${consulta}`}>Ver {cambio.recetaAparte}</Link>
                  )}
                </Alerta>
              </div>
            ))}
          </section>
        )
      ) : (
        <>
          {vista.noAptaPara.map((choque) => (
            <div key={`a-${choque.persona}`} className="receta__aviso">
              <Alerta tono="error">
                No apta para {choque.persona}: lleva {choque.restricciones.join(', ').toLowerCase()}.
              </Alerta>
            </div>
          ))}
          {vista.ajustePara.map((choque) => (
            <div key={`j-${choque.persona}`} className="receta__aviso">
              <Alerta tono="info">
                Para {choque.persona}: lleva {choque.restricciones.join(', ').toLowerCase()}; cámbiale ese ingrediente o
                prepárale otro plato.
              </Alerta>
            </div>
          ))}
        </>
      )}
      {vista.precaucionPara.map((choque) => (
        <div key={`p-${choque.persona}`} className="receta__aviso">
          <Alerta tono="info">
            Precaución para {choque.persona} ({choque.restricciones.join(', ').toLowerCase()}): revisa los ingredientes marcados.
          </Alerta>
        </div>
      ))}

      <div className="receta__porciones">
        <span id="etiqueta-porciones">Porciones</span>
        <div className="contador" role="group" aria-labelledby="etiqueta-porciones">
          <Boton
            variante="secundario"
            aria-label="Una porción menos"
            disabled={elegidas <= 1}
            onClick={() => setPorciones(Math.max(1, elegidas - 1))}
          >
            −
          </Boton>
          <output className="receta__porciones-valor" aria-live="polite">
            {elegidas}
          </output>
          <Boton aria-label="Una porción más" disabled={elegidas >= 20} onClick={() => setPorciones(elegidas + 1)}>
            +
          </Boton>
        </div>
      </div>

      <h2 className="rotulo receta__seccion">Ingredientes</h2>
      <ul className="receta__ingredientes">
        {vista.ingredientes.map((ing) => (
          <li key={ing.productoId} className={ing.enDespensa || ing.opcional ? undefined : 'receta__falta'}>
            <span>
              {ing.nombre} · {cantidadConUnidad(redondear(ing.cantidad), ing.unidad)}
              {ing.restricciones.length > 0 && (
                <span className="receta__restriccion"> · {ing.restricciones.join(', ').toLowerCase()}</span>
              )}
            </span>
            <span className="receta__estado">
              {ing.enDespensa ? 'en despensa' : ing.opcional ? 'opcional' : 'falta'}
            </span>
          </li>
        ))}
      </ul>
      {vista.costo !== undefined && (
        <p className="receta__costo">
          Costo estimado para {elegidas} {elegidas === 1 ? 'porción' : 'porciones'}: <strong>{enPesos(vista.costo)}</strong>
        </p>
      )}

      {receta.preparacionPrevia && (
        <>
          <h2 className="rotulo receta__seccion">Antes de empezar</h2>
          <p className="receta__previa">{receta.preparacionPrevia}</p>
        </>
      )}

      <h2 className="rotulo receta__seccion">Preparación</h2>
      {receta.video && <VideoReceta video={receta.video} receta={receta.nombre} />}
      <ol className="receta__pasos">
        {receta.pasos.map((paso, i) => (
          <li key={i}>{paso}</li>
        ))}
      </ol>

      <Link className="boton boton--primario boton--ancho receta__cocinar" to={`/recetas/${receta.id}/cocinar?porciones=${elegidas}`}>
        👩‍🍳 Cocinar ahora, paso a paso
      </Link>

      <div className="receta__acciones">
        <Link className="boton boton--secundario" to={`/recetas/${receta.id}/agregar?${hoy.toString()}`}>
          Ponerla hoy en el plan
        </Link>
        <Link className="boton boton--secundario" to={`/recetas/${receta.id}/agregar${consulta}`}>
          Agregar al plan
        </Link>
      </div>
    </>
  )
}

/** Cantidades escaladas sin decimales raros: 2 decimales como máximo. */
function redondear(valor: number): number {
  return Math.round(valor * 100) / 100
}
