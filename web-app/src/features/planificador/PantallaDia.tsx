import { useEffect, useState } from 'react'
import { Link, useLocation, useNavigate, useParams } from 'react-router'
import { Alerta, Boton, Cargando, EncabezadoPagina } from '../../shared/componentes'
import { enPesos } from '../despensa/formato'
import { asignarComida, marcarPreparada, otraOpcion, quitarComida } from './apiPlanificador'
import { mensajePlan, planSinHogar } from './mensajesPlan'
import { ErrorCarga, PrimeroElHogar } from './PantallaPlan'
import { COMIDAS, esDia, NOMBRE_COMIDA, NOMBRE_DIA, tituloDia } from './semana'
import type { ComidaPlan, Dia, TipoComida } from './tipos'
import { textoCambio } from '../recetario/adaptaciones'
import { useSemana, type Semana } from './useSemana'
import './planificador.css'

interface Aviso {
  texto: string
  /** Si se quitó una comida, lo necesario para volver a ponerla (RNF-13). */
  quitada?: ComidaPlan
}

/** Pantallas 29 y 33: un día del plan, con sus comidas y la confirmación de "preparada". */
export function PantallaDia() {
  const parametros = useParams()
  const dia = Number(parametros.dia)
  const ubicacion = useLocation()
  const navegar = useNavigate()
  const { estado, recargar } = useSemana()
  const [aviso, setAviso] = useState<Aviso | undefined>(() => {
    const texto = (ubicacion.state as { aviso?: string } | null)?.aviso
    return texto ? { texto } : undefined
  })
  const [confirmando, setConfirmando] = useState<number>()
  const [ocupado, setOcupado] = useState<number | 'deshacer'>()
  const [error, setError] = useState<unknown>()
  // Recetas que la usuaria ya descartó con "Otra opción", por comida, para no volver a proponerlas.
  const [descartadas, setDescartadas] = useState<Record<number, number[]>>({})

  // El aviso se muestra una sola vez: se limpia del historial para que no vuelva al recargar.
  useEffect(() => {
    if ((ubicacion.state as { aviso?: string } | null)?.aviso) {
      navegar(ubicacion.pathname, { replace: true, state: null })
    }
  }, [ubicacion, navegar])

  const volver = { ruta: '/plan', etiqueta: 'Volver a mi semana' }
  if (!esDia(dia)) {
    return (
      <>
        <EncabezadoPagina titulo="Día no encontrado" volver={volver} />
        <p>Ese día no existe en la semana.</p>
      </>
    )
  }
  if (estado.tipo !== 'listo') {
    return (
      <>
        <EncabezadoPagina titulo={NOMBRE_DIA[dia]} volver={volver} />
        {estado.tipo === 'cargando' ? (
          <Cargando texto="Cargando el día…" />
        ) : planSinHogar(estado.error) ? (
          <PrimeroElHogar />
        ) : (
          <ErrorCarga error={estado.error} reintentar={recargar} />
        )}
      </>
    )
  }

  const { semana } = estado
  const nombreDe = (c: ComidaPlan) => semana.nombres.get(c.recetaId) ?? 'La receta'

  async function accion(comida: ComidaPlan, hacer: () => Promise<unknown>, texto: string, quitada?: ComidaPlan) {
    setOcupado(comida.id)
    setError(undefined)
    try {
      await hacer()
      setAviso({ texto, quitada })
      setConfirmando(undefined)
      recargar()
    } catch (causa) {
      setError(causa)
    } finally {
      setOcupado(undefined)
    }
  }

  async function cambiarPorOtra(comida: ComidaPlan) {
    setOcupado(comida.id)
    setError(undefined)
    const excluir = [...new Set([...(descartadas[comida.id] ?? []), comida.recetaId])]
    try {
      const nueva = await otraOpcion(comida.planSemanalId, comida.id, excluir)
      setDescartadas((previas) => ({ ...previas, [comida.id]: excluir }))
      setAviso({
        texto: `Cambiamos ${nombreDe(comida)} por ${semana.nombres.get(nueva.recetaId) ?? 'otra receta'}.`,
        quitada: comida,
      })
      recargar()
    } catch (causa) {
      setError(causa)
    } finally {
      setOcupado(undefined)
    }
  }

  async function deshacer(comida: ComidaPlan) {
    setOcupado('deshacer')
    try {
      await asignarComida(comida.planSemanalId, comida.dia, comida.tipoComida, comida.recetaId)
      setAviso({ texto: `${nombreDe(comida)} volvió a tu plan.` })
      recargar()
    } catch (causa) {
      setError(causa)
    } finally {
      setOcupado(undefined)
    }
  }

  const comidas = (semana.plan?.comidas ?? []).filter((c) => c.dia === dia)
  const sinTiempo = semana.diasSinTiempo.includes(dia)
  const primeraLibre = (['ALMUERZO', 'CENA'] as TipoComida[]).find((t) => !comidas.some((c) => c.tipoComida === t))

  return (
    <>
      <EncabezadoPagina titulo={tituloDia(semana.lunes, dia)} detalle={sinTiempo ? 'día sin tiempo' : undefined} volver={volver} />

      {aviso && (
        <div className="dia-detalle__aviso">
          <Alerta tono="exito">
            {aviso.texto}{' '}
            {aviso.quitada && (
              <Boton variante="texto" cargando={ocupado === 'deshacer'} onClick={() => void deshacer(aviso.quitada!)}>
                Deshacer
              </Boton>
            )}
          </Alerta>
        </div>
      )}
      {error !== undefined && <Alerta tono="error">{mensajePlan(error)}</Alerta>}

      {COMIDAS.map((tipo) => {
        const comida = comidas.find((c) => c.tipoComida === tipo)
        return (
          <section key={tipo} className="hueco" aria-labelledby={`hueco-${tipo}`}>
            <h2 id={`hueco-${tipo}`} className="rotulo hueco__titulo">
              {NOMBRE_COMIDA[tipo]}
              {tipo === 'DESAYUNO' ? ' (opcional)' : ''}
            </h2>
            {comida ? (
              <Comida
                semana={semana}
                comida={comida}
                dia={dia}
                ocupado={ocupado === comida.id}
                confirmando={confirmando === comida.id}
                alPreguntar={() => setConfirmando(comida.id)}
                alOtraOpcion={() => void cambiarPorOtra(comida)}
                alCancelar={() => setConfirmando(undefined)}
                alPreparar={() =>
                  void accion(
                    comida,
                    () => marcarPreparada(comida.planSemanalId, comida.id),
                    `¡Bien! Marcamos ${nombreDe(comida)} como preparada.`,
                  )
                }
                alQuitar={() =>
                  void accion(
                    comida,
                    () => quitarComida(comida.planSemanalId, comida.id),
                    `Quitamos ${nombreDe(comida)} del ${NOMBRE_DIA[dia].toLowerCase()}.`,
                    comida,
                  )
                }
              />
            ) : (
              <Link className="hueco__agregar" to={`/recetas?dia=${dia}&comida=${tipo}`}>
                + Agregar receta
              </Link>
            )}
          </section>
        )
      })}

      {sinTiempo && primeraLibre && (
        <section className="sugerencia" aria-labelledby="titulo-sugerencia">
          <h2 id="titulo-sugerencia">Sugerencia para el {NOMBRE_DIA[dia].toLowerCase()}</h2>
          <p>Es un día sin tiempo: te proponemos recetas de 20 minutos o menos.</p>
          <Link className="boton boton--primario boton--ancho" to={`/recetas?dia=${dia}&comida=${primeraLibre}&tiempo=20`}>
            Ver opciones rápidas
          </Link>
        </section>
      )}
    </>
  )
}

interface ComidaProps {
  semana: Semana
  comida: ComidaPlan
  dia: Dia
  ocupado: boolean
  confirmando: boolean
  alPreguntar: () => void
  alOtraOpcion: () => void
  alCancelar: () => void
  alPreparar: () => void
  alQuitar: () => void
}

function Comida({ semana, comida, dia, ocupado, confirmando, alPreguntar, alOtraOpcion, alCancelar, alPreparar, alQuitar }: ComidaProps) {
  const vista = semana.recetas.get(comida.recetaId)
  const nombre = semana.nombres.get(comida.recetaId) ?? 'Receta'
  const detalle = [
    vista && `${vista.receta.tiempoMinutos} min`,
    vista && `${vista.receta.porciones} porciones`,
    vista?.costo !== undefined && enPesos(vista.costo),
  ]
    .filter(Boolean)
    .join(' · ')

  return (
    <>
      <Link className={comida.preparada ? 'fila fila--destacada' : 'fila'} to={`/recetas/${comida.recetaId}`}>
        <span className="fila__texto">
          <span className="fila__nombre">{nombre}</span>
          {detalle && <span className="fila__detalle">{detalle}</span>}
        </span>
        {comida.preparada ? <span className="etiqueta">preparada</span> : <span className="fila__flecha" aria-hidden="true">›</span>}
      </Link>

      {(semana.adaptaciones.get(comida.recetaId)?.cambios.length ?? 0) > 0 && (
        <ul className="hueco__ajustes" aria-label={`Ajustes por alergias en ${nombre}`}>
          {semana.adaptaciones.get(comida.recetaId)!.cambios.map((cambio, i) => (
            <li key={i}>
              {textoCambio(cambio)}{' '}
              {cambio.recetaAparteId && <Link to={`/recetas/${cambio.recetaAparteId}`}>Ver receta</Link>}
            </li>
          ))}
        </ul>
      )}

      {confirmando ? (
        <div className="confirmar hueco__confirmar">
          <p>¿Preparaste esta comida?</p>
          <span>
            {nombre} · {NOMBRE_DIA[dia].toLowerCase()}, {NOMBRE_COMIDA[comida.tipoComida].toLowerCase()}
          </span>
          <div className="formulario-item__acciones">
            <Boton variante="secundario" onClick={alCancelar} disabled={ocupado}>
              Todavía no
            </Boton>
            <Boton cargando={ocupado} onClick={alPreparar}>
              Sí, la preparé
            </Boton>
          </div>
        </div>
      ) : (
        <div className="hueco__acciones">
          {!comida.preparada && (
            <Boton variante="secundario" cargando={ocupado} onClick={alOtraOpcion}>
              Otra opción
            </Boton>
          )}
          <Link className="boton boton--secundario" to={`/recetas?dia=${dia}&comida=${comida.tipoComida}`}>
            Elegir otra
          </Link>
          <Boton variante="secundario" cargando={ocupado} onClick={alQuitar}>
            Quitar
          </Boton>
          {!comida.preparada && (
            <Boton onClick={alPreguntar} disabled={ocupado}>
              Preparada
            </Boton>
          )}
        </div>
      )}
    </>
  )
}
