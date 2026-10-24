import { useId } from 'react'
import { Link } from 'react-router'
import { ICONO_AVISO, marcarLeido, type Aviso } from './apiAvisos'
import './avisos.css'

interface Props {
  aviso: Aviso
  /** Se quitó de la lista ("Ya lo vi" o tocó el botón). */
  alDescartar?: (id: number) => void
}

/**
 * Un aviso del motor (ADR-17): qué pasa, qué se puede hacer y un botón que lleva ahí.
 * Tocar el botón o "Ya lo vi" lo marca como leído, así no vuelve a salir hoy.
 */
export function TarjetaAviso({ aviso, alDescartar }: Props) {
  const id = useId()

  function leido() {
    // Si falla, el aviso vuelve a salir la próxima vez: no vale la pena interrumpir.
    marcarLeido(aviso.id).catch(() => undefined)
    alDescartar?.(aviso.id)
  }

  return (
    <article className={aviso.leido ? 'aviso aviso--leido' : 'aviso'} aria-labelledby={`${id}-titulo`}>
      <span className="aviso__icono" aria-hidden="true">
        {ICONO_AVISO[aviso.tipo] ?? '🔔'}
      </span>
      <div className="aviso__cuerpo">
        <h2 id={`${id}-titulo`} className="aviso__titulo">
          {aviso.titulo}
        </h2>
        <p className="aviso__mensaje">{aviso.mensaje}</p>
        {aviso.enlace && aviso.accion && (
          <Link className="aviso__accion" to={aviso.enlace} onClick={aviso.leido ? undefined : leido}>
            {aviso.accion} <span aria-hidden="true">›</span>
          </Link>
        )}
      </div>
      {!aviso.leido && alDescartar && (
        <button type="button" className="aviso__cerrar" aria-label={`Ya lo vi: ${aviso.titulo}`} onClick={leido}>
          ×
        </button>
      )}
    </article>
  )
}
