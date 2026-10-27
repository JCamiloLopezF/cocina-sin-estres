import { useEffect, useState } from 'react'
import { mensajeDeError } from '../../api/mensajesError'
import { Alerta, Boton, Cargando, EncabezadoPagina } from '../../shared/componentes'
import { fechaCorta } from '../despensa/formato'
import { estadoIndicador, numero, textoParaInforme, valorIndicador } from './analisis'
import { obtenerAnalisis, type Analisis } from './apiAnalisis'
import { NavAdmin } from './NavAdmin'
import './admin.css'

type Estado = { tipo: 'cargando' } | { tipo: 'listo'; analisis: Analisis } | { tipo: 'error'; error: unknown }

/**
 * Análisis del administrador (KAN-54): cómo se está usando la app y los indicadores del
 * Plan de Gestión de la Calidad con los datos reales. Solo cifras agregadas.
 */
export function PantallaAnalisis() {
  const [estado, setEstado] = useState<Estado>({ tipo: 'cargando' })
  const [version, setVersion] = useState(0)
  const [copiado, setCopiado] = useState<'si' | 'no'>()

  useEffect(() => {
    const control = new AbortController()
    obtenerAnalisis(control.signal)
      .then((analisis) => setEstado({ tipo: 'listo', analisis }))
      .catch((error: unknown) => {
        if (!control.signal.aborted) setEstado({ tipo: 'error', error })
      })
    return () => control.abort()
  }, [version])

  if (estado.tipo !== 'listo') {
    return (
      <>
        <NavAdmin />
        <EncabezadoPagina titulo="Análisis" />
        {estado.tipo === 'cargando' ? (
          <Cargando texto="Calculando los indicadores…" />
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

  const { analisis } = estado
  const { uso } = analisis
  const medibles = analisis.indicadores.filter((i) => i.cumple !== null)
  const cumplen = medibles.filter((i) => i.cumple).length
  const procesos = [...new Set(analisis.indicadores.map((i) => i.proceso))]

  async function copiar() {
    try {
      await navigator.clipboard.writeText(textoParaInforme(analisis))
      setCopiado('si')
    } catch {
      setCopiado('no')
    }
  }

  return (
    <>
      <NavAdmin />
      <EncabezadoPagina
        titulo="Análisis"
        detalle={`Últimos 30 días · del ${fechaCorta(analisis.desde)} al ${fechaCorta(analisis.hasta)}`}
        detalleAbajo
      />

      <section className="analisis__bloque" aria-labelledby="titulo-uso">
        <h2 id="titulo-uso" className="rotulo">
          Uso de la app
        </h2>
        <dl className="cifras">
          <Cifra valor={uso.usuarias} etiqueta="Usuarias" detalle={`${uso.hogares} con hogar`} />
          <Cifra valor={uso.activas7Dias} etiqueta="Activas esta semana" detalle="abrieron la app en 7 días" />
          <Cifra valor={uso.planesEstaSemana} etiqueta="Planes esta semana" detalle="con al menos una comida" />
          <Cifra
            valor={uso.preparaciones}
            etiqueta="Recetas cocinadas"
            detalle={`${uso.preparacionesFuera} fuera del plan`}
          />
          <Cifra valor={uso.aperturas} etiqueta="Aperturas" detalle="de la app en 30 días" />
          <Cifra valor={uso.usosRescate} etiqueta="Modo rescate" detalle="veces que se usó" />
        </dl>
      </section>

      <section className="analisis__bloque" aria-labelledby="titulo-calidad">
        <div className="analisis__cabecera">
          <h2 id="titulo-calidad" className="rotulo">
            Plan de Gestión de la Calidad
          </h2>
          <Boton variante="secundario" onClick={() => void copiar()}>
            Copiar para el informe
          </Boton>
        </div>
        <p className="analisis__resumen">
          {medibles.length === 0
            ? 'Todavía no hay datos para medir los indicadores.'
            : `${cumplen} de ${medibles.length} indicadores medidos alcanzan su meta.`}
        </p>
        {copiado === 'si' && <Alerta tono="exito">Copiamos la tabla. Pégala en el Plan de Calidad, sección 5.</Alerta>}
        {copiado === 'no' && <Alerta tono="error">No pudimos copiar. Selecciona la tabla y cópiala a mano.</Alerta>}

        {procesos.map((proceso) => (
          <section key={proceso} className="indicadores" aria-label={proceso}>
            <h3 className="indicadores__proceso">{proceso}</h3>
            <ul className="indicadores__lista">
              {analisis.indicadores
                .filter((i) => i.proceso === proceso)
                .map((indicador) => {
                  const { texto, clase } = estadoIndicador(indicador)
                  return (
                    <li key={indicador.nombre} className="indicador" aria-label={indicador.nombre}>
                      <div className="indicador__cabecera">
                        <strong className="indicador__nombre">{indicador.nombre}</strong>
                        <span className={`estado ${clase}`}>{texto}</span>
                      </div>
                      <p className="indicador__valor">
                        {valorIndicador(indicador)}
                        {indicador.meta && <span className="indicador__meta"> · meta {indicador.meta}</span>}
                      </p>
                      <p className="indicador__nota">
                        {indicador.formula}
                        {indicador.muestra > 0 && ` · sobre ${indicador.muestra}`}. {indicador.nota}
                      </p>
                    </li>
                  )
                })}
            </ul>
          </section>
        ))}
      </section>

      <section className="analisis__bloque" aria-labelledby="titulo-recetas">
        <h2 id="titulo-recetas" className="rotulo">
          Recetas
        </h2>
        <div className="recetas-uso">
          <ListaRecetas titulo="Más cocinadas (30 días)" recetas={analisis.masPreparadas} vacio="Nadie ha marcado una receta como preparada." />
          <ListaRecetas
            titulo={`Más planeadas (desde el ${fechaCorta(analisis.semanasDesde)})`}
            recetas={analisis.masPlaneadas}
            vacio="Todavía no hay planes."
          />
        </div>
        {analisis.sinUsar.length > 0 && (
          <div className="sin-usar">
            <h3 className="indicadores__proceso">Sin usar en ningún plan ({analisis.sinUsar.length})</h3>
            <p className="indicador__nota">Candidatas para revisar con el curador: fotos, tiempo o costo.</p>
            <ul className="sin-usar__lista">
              {analisis.sinUsar.map((nombre) => (
                <li key={nombre}>{nombre}</li>
              ))}
            </ul>
          </div>
        )}
      </section>
    </>
  )
}

function Cifra({ valor, etiqueta, detalle }: { valor: number; etiqueta: string; detalle: string }) {
  return (
    <div className="cifra">
      <dt className="cifra__etiqueta">{etiqueta}</dt>
      <dd className="cifra__valor">{numero.format(valor)}</dd>
      <dd className="cifra__detalle">{detalle}</dd>
    </div>
  )
}

function ListaRecetas({ titulo, recetas, vacio }: { titulo: string; recetas: Analisis['masPreparadas']; vacio: string }) {
  return (
    <div className="recetas-uso__grupo">
      <h3 className="indicadores__proceso">{titulo}</h3>
      {recetas.length === 0 ? (
        <p className="indicador__nota">{vacio}</p>
      ) : (
        <ol className="recetas-uso__lista">
          {recetas.map((r) => (
            <li key={r.nombre}>
              <span>{r.nombre}</span>
              <span className="recetas-uso__veces">
                {r.veces} {r.veces === 1 ? 'vez' : 'veces'}
              </span>
            </li>
          ))}
        </ol>
      )}
    </div>
  )
}
