import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { Link, useParams, useSearchParams } from 'react-router'
import { pedir } from '../../api/cliente'
import { Alerta, Boton, Cargando } from '../../shared/componentes'
import { useSesion } from '../auth/contextoSesion'
import { cantidadConUnidad } from '../despensa/formato'
import { marcarPreparada } from '../planificador/apiPlanificador'
import { mensajePlan } from '../planificador/mensajesPlan'
import { obtenerAdaptaciones, textoCambio, type Adaptacion } from '../recetario/adaptaciones'
import { obtenerReceta } from '../recetario/apiRecetario'
import { analizarReceta, cargarCocina, type RecetaVista } from '../recetario/cocina'
import { nuevoTemporizador, sonarAlarma, type EstadoTemporizador } from './cuentaRegresiva'
import { escuchaDisponible, useEscucha } from './escucha'
import { animo, fraseDelPaso, minutosDelPaso, minutosEnPalabras, primerNombre, type Comando } from './guion'
import { IntroCocina } from './IntroCocina'
import { Temporizador } from './Temporizador'
import { useVoz } from './voz'
import './cocinar.css'

/** `alistar`, el índice del paso (0…n-1) o `final`. */
type Etapa = 'alistar' | number | 'final'

type Carga =
  | { tipo: 'cargando' }
  | { tipo: 'listo'; vista: RecetaVista; adaptacion?: Adaptacion }
  | { tipo: 'error'; error: unknown }

const CLAVE_INTRO = 'cocina-sin-estres:intro-cocina-vista'
const CLAVE_MANOS_LIBRES = 'cocina-sin-estres:manos-libres'
const progresoDe = (id: string) => `cocina-sin-estres:cocinando:${id}`
/** El avance guardado sirve durante medio día: después se empieza de nuevo. */
const VIGENCIA_PROGRESO = 12 * 60 * 60 * 1000

function leer(clave: string): string | null {
  try {
    return localStorage.getItem(clave)
  } catch {
    return null
  }
}

function guardar(clave: string, valor: string | null) {
  try {
    if (valor === null) localStorage.removeItem(clave)
    else localStorage.setItem(clave, valor)
  } catch {
    // Sin almacenamiento (modo privado): el avance dura mientras la pantalla esté abierta.
  }
}

interface Progreso {
  etapa: Etapa
  alistados: number[]
  en: number
}

function progresoGuardado(id: string): Progreso | undefined {
  try {
    const progreso = JSON.parse(leer(progresoDe(id)) ?? 'null') as Progreso | null
    return progreso && Date.now() - progreso.en < VIGENCIA_PROGRESO ? progreso : undefined
  } catch {
    return undefined
  }
}

/** Mientras cocina, la pantalla no se apaga (si el navegador lo permite). */
function usePantallaEncendida() {
  useEffect(() => {
    let candado: WakeLockSentinel | undefined
    const pedirCandado = async () => {
      try {
        candado = await navigator.wakeLock?.request('screen')
      } catch {
        // Sin permiso o sin soporte: la pantalla se apaga como siempre.
      }
    }
    const alVolver = () => {
      if (document.visibilityState === 'visible') void pedirCandado()
    }
    void pedirCandado()
    document.addEventListener('visibilitychange', alVolver)
    return () => {
      document.removeEventListener('visibilitychange', alVolver)
      void candado?.release().catch(() => undefined)
    }
  }, [])
}

/**
 * Modo cocinar: la receta como lista de tareas, un paso por pantalla, con un asistente
 * que lee cada paso, temporizador automático y comandos de voz (ajuste de la
 * sustentación). Al final, «La preparé» descuenta la despensa (KAN-16).
 */
export function PantallaCocinar() {
  const { id = '' } = useParams()
  const [parametros] = useSearchParams()
  const porciones = Number(parametros.get('porciones')) || undefined
  const planId = Number(parametros.get('plan')) || undefined
  const comidaId = Number(parametros.get('comida')) || undefined
  const { estado: sesion } = useSesion()
  const nombre = primerNombre(sesion.tipo === 'con-sesion' ? sesion.sesion.nombre : undefined)

  const [carga, setCarga] = useState<Carga>({ tipo: 'cargando' })
  const [verIntro, setVerIntro] = useState(() => leer(CLAVE_INTRO) === null)
  const [guardado] = useState(() => progresoGuardado(id))
  const [etapa, setEtapa] = useState<Etapa>(guardado?.etapa ?? 'alistar')
  const [alistados, setAlistados] = useState<number[]>(guardado?.alistados ?? [])
  const [manosLibres, setManosLibres] = useState(() => leer(CLAVE_MANOS_LIBRES) === 'si')
  const [temporizador, setTemporizador] = useState<EstadoTemporizador>()
  const [preparando, setPreparando] = useState(false)
  const [preparada, setPreparada] = useState(false)
  const [errorPreparar, setErrorPreparar] = useState<unknown>()
  const voz = useVoz()
  const puedeEscuchar = escuchaDisponible()
  usePantallaEncendida()

  useEffect(() => {
    const control = new AbortController()
    Promise.all([
      obtenerReceta(Number(id), control.signal),
      cargarCocina(control.signal),
      obtenerAdaptaciones([Number(id)], control.signal),
    ])
      .then(([receta, cocina, adaptaciones]) =>
        setCarga({
          tipo: 'listo',
          vista: analizarReceta(receta, cocina, porciones ?? receta.porciones),
          adaptacion: adaptaciones.get(Number(id)),
        }),
      )
      .catch((error: unknown) => {
        if (!control.signal.aborted) setCarga({ tipo: 'error', error })
      })
    return () => control.abort()
  }, [id, porciones])

  // El avance se guarda para seguir donde iba si cierra la app.
  useEffect(() => {
    if (etapa === 'final') guardar(progresoDe(id), null)
    else guardar(progresoDe(id), JSON.stringify({ etapa, alistados, en: Date.now() } satisfies Progreso))
  }, [id, etapa, alistados])

  const vista = carga.tipo === 'listo' ? carga.vista : undefined
  const pasos = useMemo(() => vista?.receta.pasos ?? [], [vista])
  const minutosPaso = typeof etapa === 'number' ? minutosDelPaso(pasos[etapa] ?? '') : undefined
  const { decir } = voz

  /** Lo que dice el asistente al llegar a una etapa. */
  const anunciar = useCallback(
    (cual: Etapa, prefijo?: string) => {
      if (!vista) return
      let frase: string
      if (cual === 'alistar') {
        const ajustes = (carga.tipo === 'listo' ? (carga.adaptacion?.cambios ?? []) : []).map(textoCambio)
        frase = [
          `${nombre ? `¡Hola, ${nombre}! ` : '¡Hola! '}Vamos a preparar ${vista.receta.nombre}.`,
          `Primero alistemos los ${vista.ingredientes.length} ingredientes. Ve marcándolos cuando los tengas a la mano.`,
          vista.receta.preparacionPrevia ? `Antes de empezar: ${vista.receta.preparacionPrevia}` : '',
          ajustes.length ? `Recuerda: ${ajustes.join(' ')}` : '',
        ]
          .filter(Boolean)
          .join(' ')
      } else if (cual === 'final') {
        frase = `¡Quedó listo${nombre ? `, ${nombre}` : ''}! Buen provecho. Cuando sirvas, toca «La preparé» y descuento los ingredientes de tu despensa.`
      } else {
        frase = fraseDelPaso(cual + 1, pasos.length, pasos[cual], minutosDelPaso(pasos[cual]))
      }
      decir(prefijo ? `${prefijo} ${frase}` : frase)
    },
    [vista, carga, nombre, pasos, decir],
  )

  // Al abrir (después de la introducción) se anuncia dónde va.
  const anunciado = useRef(false)
  useEffect(() => {
    if (vista && !verIntro && !anunciado.current) {
      anunciado.current = true
      anunciar(etapa, guardado && etapa !== 'alistar' ? 'Seguimos donde ibas.' : undefined)
    }
  }, [vista, verIntro, etapa, anunciar, guardado])

  const irA = useCallback(
    (destino: Etapa, prefijo?: string) => {
      setEtapa(destino)
      anunciar(destino, prefijo)
    },
    [anunciar],
  )

  const siguiente = useCallback(() => {
    if (etapa === 'alistar') irA(pasos.length ? 0 : 'final', '¡Listo! Empecemos.')
    else if (typeof etapa === 'number') irA(etapa + 1 < pasos.length ? etapa + 1 : 'final', animo(etapa, nombre))
  }, [etapa, pasos.length, irA, nombre])

  const atras = useCallback(() => {
    if (etapa === 'final') irA(pasos.length - 1)
    else if (typeof etapa === 'number') irA(etapa === 0 ? 'alistar' : etapa - 1)
  }, [etapa, pasos.length, irA])

  const iniciarTemporizador = useCallback(() => {
    if (!minutosPaso) return
    setTemporizador(nuevoTemporizador(minutosPaso))
    decir(`Listo, cuento ${minutosEnPalabras(minutosPaso)}. Te aviso cuando acabe.`)
  }, [minutosPaso, decir])

  const alTerminarTemporizador = useCallback(
    (minutos: number) => {
      sonarAlarma()
      decir(`¡Ya pasaron los ${minutosEnPalabras(minutos)}!`)
    },
    [decir],
  )

  const ejecutar = useCallback(
    (comando: Comando) => {
      if (comando === 'siguiente') siguiente()
      else if (comando === 'atras') atras()
      else if (comando === 'repite') anunciar(etapa)
      else if (comando === 'temporizador') {
        if (minutosPaso) iniciarTemporizador()
        else decir('Este paso no tiene tiempo para contar.')
      }
    },
    [siguiente, atras, anunciar, etapa, minutosPaso, iniciarTemporizador, decir],
  )

  const escucha = useEscucha(manosLibres && !verIntro, voz.hablando, ejecutar)

  function alternarManosLibres() {
    const ahora = !manosLibres
    setManosLibres(ahora)
    guardar(CLAVE_MANOS_LIBRES, ahora ? 'si' : null)
  }

  function terminarIntro() {
    guardar(CLAVE_INTRO, 'si')
    setVerIntro(false)
  }

  async function laPrepare() {
    if (!vista) return
    setPreparando(true)
    setErrorPreparar(undefined)
    try {
      if (planId && comidaId) await marcarPreparada(planId, comidaId)
      else await pedir<void>('/preparaciones', { metodo: 'POST', cuerpo: { recetaId: vista.receta.id } })
      setPreparada(true)
      decir('¡Hecho! Descontamos los ingredientes de tu despensa.')
    } catch (error) {
      setErrorPreparar(error)
    } finally {
      setPreparando(false)
    }
  }

  const salir = `/recetas/${id}`

  if (carga.tipo !== 'listo' || !vista) {
    return (
      <main className="cocinar">
        <BarraCocina salir={salir} titulo="Modo cocinar" />
        {carga.tipo === 'error' ? (
          <Alerta tono="error">{mensajePlan(carga.error)}</Alerta>
        ) : (
          <Cargando texto="Alistando tu receta…" />
        )}
      </main>
    )
  }

  if (verIntro) {
    return (
      <main className="cocinar">
        <BarraCocina salir={salir} titulo={vista.receta.nombre} />
        <IntroCocina
          voz={voz}
          puedeEscuchar={puedeEscuchar}
          manosLibres={manosLibres}
          alternarManosLibres={alternarManosLibres}
          alTerminar={terminarIntro}
        />
      </main>
    )
  }

  const total = pasos.length + 1
  const hechos = etapa === 'alistar' ? 0 : etapa === 'final' ? total : etapa + 1
  const ajustes = carga.adaptacion?.cambios ?? []

  return (
    <main className="cocinar">
      <BarraCocina salir={salir} titulo={vista.receta.nombre}>
        {voz.disponible && (
          <button
            type="button"
            className="cocinar__icono"
            aria-pressed={voz.encendida}
            aria-label={voz.encendida ? 'Silenciar el asistente' : 'Encender la voz del asistente'}
            onClick={voz.alternar}
          >
            {voz.encendida ? '🔊' : '🔇'}
          </button>
        )}
        {puedeEscuchar && (
          <button
            type="button"
            className="cocinar__icono"
            aria-pressed={manosLibres}
            aria-label={manosLibres ? 'Apagar manos libres' : 'Encender manos libres'}
            onClick={alternarManosLibres}
          >
            🎤
          </button>
        )}
        <button type="button" className="cocinar__icono" aria-label="Ver la introducción" onClick={() => setVerIntro(true)}>
          ?
        </button>
      </BarraCocina>

      <div
        className="cocinar__avance"
        role="progressbar"
        aria-label="Avance de la receta"
        aria-valuemin={0}
        aria-valuemax={total}
        aria-valuenow={hechos}
      >
        <span style={{ width: `${(hechos / total) * 100}%` }} />
      </div>

      {manosLibres && (
        <p className="cocinar__escucha" aria-live="polite">
          {escucha.error === 'permiso'
            ? 'No tengo permiso para usar el micrófono. Actívalo en el navegador o sigue tocando los botones.'
            : escucha.escuchando
              ? '🎤 Te escucho: di «siguiente», «repite», «atrás» o «temporizador».'
              : voz.hablando
                ? 'Hablando… te escucho cuando termine.'
                : 'Manos libres encendido.'}
        </p>
      )}

      {temporizador && (
        <Temporizador estado={temporizador} alCambiar={setTemporizador} alTerminar={alTerminarTemporizador} />
      )}

      {etapa === 'alistar' && (
        <section className="cocinar__tarjeta" aria-labelledby="titulo-etapa">
          <p className="cocinar__etapa">Para empezar</p>
          <h1 id="titulo-etapa" className="cocinar__titulo">
            Alistemos los ingredientes
          </h1>
          {vista.receta.preparacionPrevia && (
            <p className="cocinar__previa">
              <strong>Antes de empezar:</strong> {vista.receta.preparacionPrevia}
            </p>
          )}
          {ajustes.length > 0 && (
            <ul className="cocinar__ajustes" aria-label="Ajustes por alergias">
              {ajustes.map((cambio, i) => (
                <li key={i}>{textoCambio(cambio)}</li>
              ))}
            </ul>
          )}
          <ul className="cocinar__ingredientes">
            {vista.ingredientes.map((ing) => (
              <li key={ing.productoId}>
                <label className="cocinar__ingrediente">
                  <input
                    type="checkbox"
                    checked={alistados.includes(ing.productoId)}
                    onChange={() =>
                      setAlistados((antes) =>
                        antes.includes(ing.productoId)
                          ? antes.filter((p) => p !== ing.productoId)
                          : [...antes, ing.productoId],
                      )
                    }
                  />
                  <span>
                    {ing.nombre} · {cantidadConUnidad(Math.round(ing.cantidad * 100) / 100, ing.unidad)}
                    {ing.opcional && <span className="cocinar__opcional"> (opcional)</span>}
                  </span>
                </label>
              </li>
            ))}
          </ul>
          <div className="cocinar__acciones">
            <Boton variante="secundario" onClick={() => anunciar('alistar')}>
              Repetir
            </Boton>
            <Boton onClick={siguiente}>Empezar a cocinar</Boton>
          </div>
        </section>
      )}

      {typeof etapa === 'number' && (
        <section className="cocinar__tarjeta" aria-labelledby="titulo-etapa">
          <p className="cocinar__etapa">
            Paso {etapa + 1} de {pasos.length}
          </p>
          <h1 id="titulo-etapa" className="cocinar__paso">
            {pasos[etapa]}
          </h1>
          {minutosPaso && !temporizador && (
            <Boton variante="secundario" ancho onClick={iniciarTemporizador}>
              ⏱ Contar {minutosEnPalabras(minutosPaso)}
            </Boton>
          )}
          <div className="cocinar__acciones cocinar__acciones--tres">
            <Boton variante="secundario" onClick={atras}>
              Atrás
            </Boton>
            <Boton variante="secundario" onClick={() => anunciar(etapa)}>
              Repetir
            </Boton>
            <Boton onClick={siguiente}>Listo</Boton>
          </div>
        </section>
      )}

      {etapa === 'final' && (
        <section className="cocinar__tarjeta cocinar__tarjeta--final" aria-labelledby="titulo-etapa">
          <p className="cocinar__celebracion" aria-hidden="true">
            🎉
          </p>
          <h1 id="titulo-etapa" className="cocinar__titulo">
            ¡Quedó listo{nombre ? `, ${nombre}` : ''}! Buen provecho
          </h1>
          {preparada ? (
            <>
              <Alerta tono="exito">Descontamos los ingredientes de tu despensa.</Alerta>
              <Link className="boton boton--primario boton--ancho" to={planId ? '/plan' : '/recetas'}>
                {planId ? 'Volver a mi semana' : 'Volver al recetario'}
              </Link>
            </>
          ) : (
            <>
              <p>Cuando sirvas, toca «La preparé» y descuento de tu despensa lo que usaste.</p>
              {errorPreparar !== undefined && <Alerta tono="error">{mensajePlan(errorPreparar)}</Alerta>}
              <Boton ancho cargando={preparando} onClick={() => void laPrepare()}>
                La preparé
              </Boton>
              <Boton variante="texto" onClick={atras}>
                Volver al último paso
              </Boton>
            </>
          )}
        </section>
      )}
    </main>
  )
}

function BarraCocina({ salir, titulo, children }: { salir: string; titulo: string; children?: React.ReactNode }) {
  return (
    <header className="cocinar__barra">
      <Link className="cocinar__salir" to={salir} aria-label="Salir del modo cocinar">
        ‹ Salir
      </Link>
      <p className="cocinar__nombre">{titulo}</p>
      <div className="cocinar__botones">{children}</div>
    </header>
  )
}
