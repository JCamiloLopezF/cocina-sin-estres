import { useEffect, useState } from 'react'
import { Link } from 'react-router'
import { mensajeDeError } from '../../api/mensajesError'
import { Alerta, Boton, Cargando, EncabezadoPagina, EstadoVacio } from '../../shared/componentes'
import {
  avisosRecientes,
  cambiarPreferencia,
  marcarTodosLeidos,
  preferenciasAvisos,
  type Aviso,
  type PreferenciaAviso,
  type TipoAviso,
} from './apiAvisos'
import { TarjetaAviso } from './TarjetaAviso'
import './avisos.css'

type Estado =
  | { tipo: 'cargando' }
  | { tipo: 'listo'; avisos: Aviso[]; preferencias: PreferenciaAviso[] }
  | { tipo: 'error'; error: unknown }

/**
 * Centro de avisos (Proceso 4): lo que la app le contó a la usuaria en el último mes y
 * qué avisos quiere recibir (activación por tipo, ADR-17).
 */
export function PantallaAvisos() {
  const [estado, setEstado] = useState<Estado>({ tipo: 'cargando' })
  const [version, setVersion] = useState(0)
  const [cambiando, setCambiando] = useState<TipoAviso | 'todos'>()
  const [aviso, setAviso] = useState<{ tono: 'exito' | 'error'; texto: string }>()

  useEffect(() => {
    const control = new AbortController()
    Promise.all([avisosRecientes(control.signal), preferenciasAvisos(control.signal)])
      .then(([avisos, preferencias]) => setEstado({ tipo: 'listo', avisos, preferencias }))
      .catch((error: unknown) => {
        if (!control.signal.aborted) setEstado({ tipo: 'error', error })
      })
    return () => control.abort()
  }, [version])

  const volver = { ruta: '/', etiqueta: 'Volver al inicio' }
  if (estado.tipo !== 'listo') {
    return (
      <>
        <EncabezadoPagina titulo="Avisos" volver={volver} />
        {estado.tipo === 'cargando' ? (
          <Cargando texto="Cargando tus avisos…" />
        ) : (
          <Alerta tono="error">
            {mensajeDeError(estado.error)}{' '}
            <Boton variante="texto" onClick={() => setVersion((v) => v + 1)}>
              Reintentar
            </Boton>
          </Alerta>
        )}
      </>
    )
  }

  const { avisos, preferencias } = estado
  const sinLeer = avisos.filter((a) => !a.leido).length

  function marcarLocal(cambio: (a: Aviso) => Aviso) {
    setEstado((antes) => (antes.tipo === 'listo' ? { ...antes, avisos: antes.avisos.map(cambio) } : antes))
  }

  async function todosLeidos() {
    setCambiando('todos')
    setAviso(undefined)
    try {
      await marcarTodosLeidos()
      marcarLocal((a) => ({ ...a, leido: true }))
    } catch (error) {
      setAviso({ tono: 'error', texto: mensajeDeError(error) })
    } finally {
      setCambiando(undefined)
    }
  }

  async function alternar(preferencia: PreferenciaAviso) {
    setCambiando(preferencia.tipo)
    setAviso(undefined)
    try {
      const nuevas = await cambiarPreferencia(preferencia.tipo, !preferencia.activo)
      setEstado((antes) => (antes.tipo === 'listo' ? { ...antes, preferencias: nuevas } : antes))
      setAviso({
        tono: 'exito',
        texto: preferencia.activo
          ? `Listo, ya no te avisamos de «${preferencia.titulo.toLowerCase()}».`
          : `Listo, te volvemos a avisar de «${preferencia.titulo.toLowerCase()}».`,
      })
    } catch (error) {
      setAviso({ tono: 'error', texto: mensajeDeError(error) })
    } finally {
      setCambiando(undefined)
    }
  }

  return (
    <>
      <EncabezadoPagina
        titulo="Avisos"
        detalle={sinLeer === 0 ? 'Estás al día' : sinLeer === 1 ? '1 sin leer' : `${sinLeer} sin leer`}
        volver={volver}
      />
      {aviso && <Alerta tono={aviso.tono}>{aviso.texto}</Alerta>}

      {avisos.length === 0 ? (
        <EstadoVacio
          titulo="Todavía no hay avisos"
          texto="Aquí te contamos cuando algo esté por vencer, se esté acabando o haya que alistar algo para mañana."
          acciones={
            <Link className="boton boton--secundario boton--ancho" to="/despensa">
              Ver mi despensa
            </Link>
          }
        />
      ) : (
        <>
          {sinLeer > 0 && (
            <div className="avisos__barra">
              <Boton variante="texto" cargando={cambiando === 'todos'} onClick={() => void todosLeidos()}>
                Marcar todo como leído
              </Boton>
            </div>
          )}
          <ul className="avisos">
            {avisos.map((a) => (
              <li key={a.id}>
                <TarjetaAviso aviso={a} alDescartar={(id) => marcarLocal((x) => (x.id === id ? { ...x, leido: true } : x))} />
                {a.creadoEn && <p className="avisos__fecha">{fechaCorta(a.creadoEn)}</p>}
              </li>
            ))}
          </ul>
        </>
      )}

      <section className="preferencias-avisos" aria-labelledby="titulo-preferencias">
        <h2 id="titulo-preferencias" className="rotulo">
          Qué avisos quiero recibir
        </h2>
        <ul className="preferencias-avisos__lista">
          {preferencias.map((p) => (
            <li key={p.tipo}>
              <label className="preferencia-aviso">
                <span className="preferencia-aviso__texto">
                  <strong>{p.titulo}</strong>
                  <span className="campo__ayuda">{p.descripcion}</span>
                </span>
                <input
                  type="checkbox"
                  role="switch"
                  className="interruptor"
                  checked={p.activo}
                  disabled={cambiando !== undefined}
                  aria-busy={cambiando === p.tipo || undefined}
                  onChange={() => void alternar(p)}
                />
              </label>
            </li>
          ))}
        </ul>
      </section>
    </>
  )
}

const FORMATO_FECHA = new Intl.DateTimeFormat('es-CO', { weekday: 'long', day: 'numeric', month: 'long' })

/** "Hoy", "Ayer" o "viernes 23 de octubre". */
function fechaCorta(iso: string): string {
  const fecha = new Date(iso)
  if (Number.isNaN(fecha.getTime())) return ''
  const hoy = new Date()
  const dias = Math.round(
    (new Date(hoy.getFullYear(), hoy.getMonth(), hoy.getDate()).getTime() -
      new Date(fecha.getFullYear(), fecha.getMonth(), fecha.getDate()).getTime()) /
      86_400_000,
  )
  if (dias === 0) return 'Hoy'
  if (dias === 1) return 'Ayer'
  const texto = FORMATO_FECHA.format(fecha)
  return texto.charAt(0).toUpperCase() + texto.slice(1)
}
