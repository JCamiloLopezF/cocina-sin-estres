import { useEffect, useState, type FormEvent } from 'react'
import { Alerta, Boton, CampoTexto, Cargando, EncabezadoPagina } from '../../shared/componentes'
import { relojDe } from '../cocinar/guion'
import {
  cambiarEtiquetasReceta,
  guardarVideoReceta,
  listarVideosRecetas,
  mensajeCuraduria,
  quitarVideoReceta,
  type RecetaConVideo,
} from './apiCuraduria'
import { inicioDelEnlace, segundosDe } from './tiempoVideo'
import './curaduria.css'

type Estado = { tipo: 'cargando' } | { tipo: 'listo'; recetas: RecetaConVideo[] } | { tipo: 'error'; error: unknown }

/**
 * Las recetas del catálogo, para el curador:
 * - un video de YouTube que muestra lo más importante. Solo se guarda el fragmento útil, que
 *   dura como máximo la mitad del tiempo de la receta (pedido del profesor);
 * - si es plato fuerte y si les gusta a los niños, que "Armar mi semana" tiene en cuenta (KAN-17).
 */
export function PantallaVideosRecetas() {
  const [estado, setEstado] = useState<Estado>({ tipo: 'cargando' })
  const [version, setVersion] = useState(0)
  const [editando, setEditando] = useState<number>()
  const [aviso, setAviso] = useState<string>()
  const [errorEtiqueta, setErrorEtiqueta] = useState<string>()
  const [marcando, setMarcando] = useState<number>()

  useEffect(() => {
    const control = new AbortController()
    listarVideosRecetas(control.signal)
      .then((recetas) => setEstado({ tipo: 'listo', recetas }))
      .catch((error: unknown) => {
        if (!control.signal.aborted) setEstado({ tipo: 'error', error })
      })
    return () => control.abort()
  }, [version])

  function actualizar(receta: RecetaConVideo, texto: string) {
    setEstado((antes) =>
      antes.tipo === 'listo'
        ? { tipo: 'listo', recetas: antes.recetas.map((r) => (r.id === receta.id ? receta : r)) }
        : antes,
    )
    setEditando(undefined)
    setAviso(texto)
  }

  async function marcar(receta: RecetaConVideo, cambio: { platoFuerte?: boolean; gustaANinos?: boolean }) {
    setMarcando(receta.id)
    setAviso(undefined)
    setErrorEtiqueta(undefined)
    try {
      const nueva = await cambiarEtiquetasReceta(receta.id, cambio)
      const texto =
        cambio.platoFuerte !== undefined
          ? nueva.platoFuerte
            ? `${nueva.nombre} ya puede ir en el almuerzo o la cena.`
            : `${nueva.nombre} ya no se propone como almuerzo o cena.`
          : nueva.gustaANinos
            ? `${nueva.nombre} queda entre las que les gustan a los niños.`
            : `${nueva.nombre} ya no se marca como de niños.`
      setEstado((antes) =>
        antes.tipo === 'listo' ? { tipo: 'listo', recetas: antes.recetas.map((r) => (r.id === nueva.id ? nueva : r)) } : antes,
      )
      setAviso(texto)
    } catch (causa) {
      setErrorEtiqueta(mensajeCuraduria(causa))
    } finally {
      setMarcando(undefined)
    }
  }

  const recetas = estado.tipo === 'listo' ? estado.recetas : []
  const conVideo = recetas.filter((r) => r.youtubeId).length

  return (
    <>
      <EncabezadoPagina
        titulo="Recetas"
        detalle="Un video corto por receta (máximo la mitad de su tiempo) y cómo la usa «Armar mi semana»."
        detalleAbajo
        volver={{ ruta: '/curaduria', etiqueta: 'Volver al catálogo' }}
      />
      {aviso && <Alerta tono="exito">{aviso}</Alerta>}
      {errorEtiqueta && <Alerta tono="error">{errorEtiqueta}</Alerta>}

      {estado.tipo === 'cargando' && <Cargando texto="Cargando las recetas…" />}
      {estado.tipo === 'error' && (
        <Alerta tono="error">
          {mensajeCuraduria(estado.error)}{' '}
          <Boton variante="texto" onClick={() => setVersion((v) => v + 1)}>
            Reintentar
          </Boton>
        </Alerta>
      )}

      {estado.tipo === 'listo' && (
        <>
          <p className="curaduria__conteo">
            {conVideo} de {recetas.length} recetas tienen video
          </p>
          <ul className="videos-recetas">
            {recetas.map((receta) => (
              <li key={receta.id} className="videos-recetas__item">
                <div className="videos-recetas__fila">
                  <span className="fila__texto">
                    <span className="fila__nombre">{receta.nombre}</span>
                    <span className="fila__detalle">
                      {receta.youtubeId
                        ? `Video de ${relojDe((receta.finSeg ?? 0) - (receta.inicioSeg ?? 0))} (desde ${relojDe(receta.inicioSeg ?? 0)})`
                        : `Sin video · máximo ${relojDe(receta.maximoSegundos)}`}
                    </span>
                  </span>
                  {editando !== receta.id && (
                    <Boton
                      variante="secundario"
                      aria-label={`${receta.youtubeId ? 'Cambiar' : 'Agregar'} video de ${receta.nombre}`}
                      onClick={() => {
                        setAviso(undefined)
                        setEditando(receta.id)
                      }}
                    >
                      {receta.youtubeId ? 'Cambiar' : 'Agregar'}
                    </Boton>
                  )}
                </div>
                <fieldset className="etiquetas-receta" disabled={marcando === receta.id}>
                  <legend className="solo-lectores">Cómo se usa {receta.nombre} al armar la semana</legend>
                  <label className="etiquetas-receta__opcion">
                    <input
                      type="checkbox"
                      checked={receta.platoFuerte}
                      onChange={(e) => void marcar(receta, { platoFuerte: e.target.checked })}
                    />
                    Plato fuerte (almuerzo o cena)
                  </label>
                  <label className="etiquetas-receta__opcion">
                    <input
                      type="checkbox"
                      checked={receta.gustaANinos}
                      onChange={(e) => void marcar(receta, { gustaANinos: e.target.checked })}
                    />
                    Les gusta a los niños
                  </label>
                </fieldset>
                {editando === receta.id && (
                  <FormularioVideo
                    receta={receta}
                    alCancelar={() => setEditando(undefined)}
                    alGuardar={(r) => actualizar(r, `Guardamos el video de ${r.nombre}.`)}
                    alQuitar={(r) => actualizar(r, `Quitamos el video de ${r.nombre}.`)}
                  />
                )}
              </li>
            ))}
          </ul>
        </>
      )}
    </>
  )
}

interface PropsFormulario {
  receta: RecetaConVideo
  alCancelar: () => void
  alGuardar: (receta: RecetaConVideo) => void
  alQuitar: (receta: RecetaConVideo) => void
}

function FormularioVideo({ receta, alCancelar, alGuardar, alQuitar }: PropsFormulario) {
  const [enlace, setEnlace] = useState(receta.youtubeId ? `https://youtu.be/${receta.youtubeId}` : '')
  const [inicio, setInicio] = useState(relojDe(receta.inicioSeg ?? 0))
  const [fin, setFin] = useState(receta.finSeg != null ? relojDe(receta.finSeg) : '')
  const [errores, setErrores] = useState<{ enlace?: string; inicio?: string; fin?: string; rango?: string }>({})
  const [error, setError] = useState<string>()
  const [ocupado, setOcupado] = useState<'guardar' | 'quitar'>()

  const inicioSeg = segundosDe(inicio)
  const finSeg = segundosDe(fin)
  const duracion = inicioSeg !== undefined && finSeg !== undefined && finSeg > inicioSeg ? finSeg - inicioSeg : undefined

  function cambiarEnlace(valor: string) {
    setEnlace(valor)
    const desde = inicioDelEnlace(valor)
    if (desde !== undefined) setInicio(relojDe(desde))
  }

  async function guardar(evento: FormEvent) {
    evento.preventDefault()
    const nuevos: typeof errores = {}
    if (!enlace.trim()) nuevos.enlace = 'Pega el enlace del video de YouTube.'
    if (inicioSeg === undefined) nuevos.inicio = 'Escríbelo como minutos:segundos, por ejemplo 1:30.'
    if (finSeg === undefined) nuevos.fin = 'Escríbelo como minutos:segundos, por ejemplo 4:00.'
    else if (inicioSeg !== undefined && finSeg <= inicioSeg) nuevos.rango = 'El video debe terminar después de donde empieza.'
    else if (duracion !== undefined && duracion > receta.maximoSegundos)
      nuevos.rango = `El video dura ${relojDe(duracion)}; para esta receta el máximo es ${relojDe(receta.maximoSegundos)}.`
    setErrores(nuevos)
    if (Object.keys(nuevos).length > 0) return

    setOcupado('guardar')
    setError(undefined)
    try {
      alGuardar(await guardarVideoReceta(receta.id, { enlace: enlace.trim(), inicioSeg: inicioSeg!, finSeg: finSeg! }))
    } catch (causa) {
      setError(mensajeCuraduria(causa))
    } finally {
      setOcupado(undefined)
    }
  }

  async function quitar() {
    setOcupado('quitar')
    setError(undefined)
    try {
      alQuitar(await quitarVideoReceta(receta.id))
    } catch (causa) {
      setError(mensajeCuraduria(causa))
    } finally {
      setOcupado(undefined)
    }
  }

  return (
    <form className="videos-recetas__formulario" onSubmit={(e) => void guardar(e)} noValidate>
      <CampoTexto
        etiqueta="Enlace de YouTube"
        ayuda="Cópialo con «Compartir» en YouTube. Si lo compartes desde un minuto, ese será el inicio."
        value={enlace}
        onChange={(e) => cambiarEnlace(e.target.value)}
        error={errores.enlace}
        inputMode="url"
        autoComplete="off"
      />
      <div className="videos-recetas__tiempos">
        <CampoTexto
          etiqueta="Desde"
          ayuda="min:seg"
          value={inicio}
          onChange={(e) => setInicio(e.target.value)}
          error={errores.inicio}
          inputMode="numeric"
        />
        <CampoTexto
          etiqueta="Hasta"
          ayuda="min:seg"
          value={fin}
          onChange={(e) => setFin(e.target.value)}
          error={errores.fin}
          inputMode="numeric"
        />
      </div>
      {errores.rango && <Alerta tono="error">{errores.rango}</Alerta>}
      <p className="campo__ayuda" aria-live="polite">
        {duracion !== undefined
          ? `Dura ${relojDe(duracion)} de un máximo de ${relojDe(receta.maximoSegundos)} (la receta toma ${receta.tiempoMinutos} min).`
          : `Máximo ${relojDe(receta.maximoSegundos)}: la mitad de los ${receta.tiempoMinutos} min de la receta.`}
      </p>
      {error && <Alerta tono="error">{error}</Alerta>}
      <div className="formulario-item__acciones">
        <Boton variante="secundario" onClick={alCancelar} disabled={ocupado !== undefined}>
          Cancelar
        </Boton>
        <Boton type="submit" cargando={ocupado === 'guardar'} disabled={ocupado === 'quitar'}>
          Guardar video
        </Boton>
      </div>
      {receta.youtubeId && (
        <Boton variante="texto" cargando={ocupado === 'quitar'} disabled={ocupado === 'guardar'} onClick={() => void quitar()}>
          Quitar el video
        </Boton>
      )}
    </form>
  )
}
