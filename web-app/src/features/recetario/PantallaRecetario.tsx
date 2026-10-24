import { useEffect, useMemo, useState } from 'react'
import { Link, useSearchParams } from 'react-router'
import { Alerta, Boton, Cargando, EncabezadoPagina, EstadoVacio, GrupoOpciones } from '../../shared/componentes'
import { enPesos } from '../despensa/formato'
import { mensajePlan } from '../planificador/mensajesPlan'
import { huecoDesde, NOMBRE_COMIDA, NOMBRE_DIA } from '../planificador/semana'
import { listarRecetasConDetalle } from './apiRecetario'
import { analizarReceta, cargarCocina, listaPersonas, type RecetaVista } from './cocina'
import './recetario.css'

type Atajo = 'TODAS' | 'RAPIDAS' | 'ECONOMICAS' | 'CON_LO_QUE_TENGO'
type Tiempo = '20' | '40' | 'SIN_LIMITE'
type Costo = '3000' | '6000' | 'CUALQUIERA'

const ATAJOS: { valor: Atajo; texto: string }[] = [
  { valor: 'TODAS', texto: 'Todas' },
  { valor: 'RAPIDAS', texto: 'Rápidas' },
  { valor: 'ECONOMICAS', texto: 'Económicas' },
  { valor: 'CON_LO_QUE_TENGO', texto: 'Con lo que tengo' },
]

/** Rápida: lista en media hora o menos. Económica: hasta $4.000 por porción. */
const MINUTOS_RAPIDA = 30
const COSTO_ECONOMICA = 4000
const MAX_INGREDIENTES = 12

interface Filtros {
  tiempo: Tiempo
  ingredientes: number
  costo: Costo
  soloHoy: boolean
}

const SIN_FILTROS: Filtros = { tiempo: 'SIN_LIMITE', ingredientes: MAX_INGREDIENTES, costo: 'CUALQUIERA', soloHoy: false }

type Estado = { tipo: 'cargando' } | { tipo: 'listo'; recetas: RecetaVista[] } | { tipo: 'error'; error: unknown }

/** Pantallas 21 y 22: el recetario con búsqueda, atajos y filtros. */
export function PantallaRecetario() {
  const [parametros] = useSearchParams()
  const hueco = huecoDesde(parametros)
  const [estado, setEstado] = useState<Estado>({ tipo: 'cargando' })
  const [version, setVersion] = useState(0)
  const [buscar, setBuscar] = useState('')
  const [atajo, setAtajo] = useState<Atajo>(() => {
    const valor = parametros.get('atajo')
    return ATAJOS.some((a) => a.valor === valor) ? (valor as Atajo) : 'TODAS'
  })
  const [abiertos, setAbiertos] = useState(false)
  // Por defecto se ocultan las recetas con alergias de alguien del hogar (ajuste del prototipo 2).
  const [aplicarAlergias, setAplicarAlergias] = useState(true)
  // Desde un día sin tiempo el plan abre el recetario con ?tiempo=20.
  const [filtros, setFiltros] = useState<Filtros>(() => ({
    ...SIN_FILTROS,
    tiempo: parametros.get('tiempo') === '20' ? '20' : 'SIN_LIMITE',
  }))

  useEffect(() => {
    const control = new AbortController()
    Promise.all([listarRecetasConDetalle(control.signal), cargarCocina(control.signal)])
      .then(([recetas, cocina]) => setEstado({ tipo: 'listo', recetas: recetas.map((r) => analizarReceta(r, cocina)) }))
      .catch((error: unknown) => {
        if (!control.signal.aborted) {
          setEstado({ tipo: 'error', error })
        }
      })
    return () => control.abort()
  }, [version])

  const candidatas = useMemo(
    () => (estado.tipo === 'listo' ? estado.recetas.filter((v) => cumple(v, buscar, atajo, filtros)) : []),
    [estado, buscar, atajo, filtros],
  )
  const visibles = aplicarAlergias ? candidatas.filter((v) => v.noAptaPara.length === 0) : candidatas
  const ocultasPorAlergia = candidatas.length - visibles.length
  const conFiltros = JSON.stringify(filtros) !== JSON.stringify(SIN_FILTROS)
  const sufijo = hueco ? `?dia=${hueco.dia}&comida=${hueco.comida}` : ''

  return (
    <>
      <EncabezadoPagina
        titulo="Recetario"
        detalle={estado.tipo === 'listo' && estado.recetas.length > 0 ? contar(visibles.length) : undefined}
      />

      {hueco && (
        <div className="recetario__hueco">
          <Alerta tono="info">
            Eligiendo para el {NOMBRE_DIA[hueco.dia].toLowerCase()} · {NOMBRE_COMIDA[hueco.comida].toLowerCase()}.{' '}
            <Link to={`/plan/dia/${hueco.dia}`}>Volver al día</Link>
          </Alerta>
        </div>
      )}

      {estado.tipo === 'cargando' && <Cargando texto="Cargando recetas…" />}
      {estado.tipo === 'error' && (
        <Alerta tono="error">
          {mensajePlan(estado.error)}{' '}
          <Boton variante="texto" onClick={() => setVersion((v) => v + 1)}>
            Reintentar
          </Boton>
        </Alerta>
      )}
      {estado.tipo === 'listo' && estado.recetas.length === 0 && (
        <EstadoVacio titulo="Todavía no hay recetas" texto="El equipo de curaduría está cargando el recetario. Vuelve pronto." />
      )}

      {estado.tipo === 'listo' && estado.recetas.length > 0 && (
        <>
          <div className="recetario__busqueda">
            <label className="solo-lectores" htmlFor="buscar-receta">
              Buscar receta o ingrediente
            </label>
            <input
              id="buscar-receta"
              className="campo__control"
              type="search"
              placeholder="Receta o ingrediente"
              value={buscar}
              onChange={(e) => setBuscar(e.target.value)}
            />
            <button
              type="button"
              className="recetario__boton-filtros"
              aria-expanded={abiertos}
              aria-controls="filtros-recetario"
              onClick={() => setAbiertos((a) => !a)}
            >
              Filtros{conFiltros ? ' •' : ''}
            </button>
          </div>

          {abiertos && (
            <section id="filtros-recetario" className="filtros-recetario" aria-labelledby="titulo-filtros">
              <div className="filtros-recetario__titulo">
                <h2 id="titulo-filtros">Filtrar recetas</h2>
                <Boton variante="texto" onClick={() => setFiltros(SIN_FILTROS)}>
                  Limpiar
                </Boton>
              </div>
              <GrupoOpciones<Tiempo>
                leyenda="Tiempo de preparación"
                variante="segmentos"
                opciones={[
                  { valor: '20', texto: '≤ 20 min' },
                  { valor: '40', texto: '≤ 40 min' },
                  { valor: 'SIN_LIMITE', texto: 'Sin límite' },
                ]}
                valor={filtros.tiempo}
                alCambiar={(tiempo) => setFiltros((f) => ({ ...f, tiempo }))}
              />
              <div className="campo">
                <label className="campo__etiqueta" htmlFor="filtro-ingredientes">
                  Cantidad de ingredientes: {filtros.ingredientes < MAX_INGREDIENTES ? `hasta ${filtros.ingredientes}` : 'sin límite'}
                </label>
                <input
                  id="filtro-ingredientes"
                  className="recetario__rango"
                  type="range"
                  min={3}
                  max={MAX_INGREDIENTES}
                  value={filtros.ingredientes}
                  onChange={(e) => setFiltros((f) => ({ ...f, ingredientes: Number(e.target.value) }))}
                />
              </div>
              <label className="casilla casilla--tarjeta">
                Aplicar alergias del hogar
                <input
                  type="checkbox"
                  checked={aplicarAlergias}
                  onChange={(e) => setAplicarAlergias(e.target.checked)}
                />
              </label>
              <label className="casilla casilla--tarjeta">
                Solo lo que puedo cocinar hoy
                <input
                  type="checkbox"
                  checked={filtros.soloHoy}
                  onChange={(e) => setFiltros((f) => ({ ...f, soloHoy: e.target.checked }))}
                />
              </label>
              <GrupoOpciones<Costo>
                leyenda="Costo por porción"
                variante="segmentos"
                opciones={[
                  { valor: '3000', texto: '≤ $3k' },
                  { valor: '6000', texto: '≤ $6k' },
                  { valor: 'CUALQUIERA', texto: 'Cualquiera' },
                ]}
                valor={filtros.costo}
                alCambiar={(costo) => setFiltros((f) => ({ ...f, costo }))}
              />
              <Boton ancho onClick={() => setAbiertos(false)}>
                Ver {contar(visibles.length)}
              </Boton>
            </section>
          )}

          <div className="filtros" role="group" aria-label="Mostrar">
            {ATAJOS.map((a) => (
              <button
                key={a.valor}
                type="button"
                className="filtros__boton"
                aria-pressed={atajo === a.valor}
                onClick={() => setAtajo(a.valor)}
              >
                {a.texto}
              </button>
            ))}
          </div>

          {visibles.length === 0 ? (
            <p className="recetario__sin-resultados">
              Ninguna receta coincide. Prueba con otra búsqueda o quita algún filtro.
            </p>
          ) : (
            <ul className="filas">
              {visibles.map((vista) => (
                <li key={vista.receta.id}>
                  <TarjetaReceta vista={vista} ruta={`/recetas/${vista.receta.id}${sufijo}`} />
                </li>
              ))}
            </ul>
          )}
          {ocultasPorAlergia > 0 && (
            <p className="recetario__ocultas">
              {ocultasPorAlergia === 1
                ? 'Ocultamos 1 receta que no es apta por una alergia grave en tu hogar.'
                : `Ocultamos ${ocultasPorAlergia} recetas que no son aptas por alergias graves en tu hogar.`}{' '}
              <Boton variante="texto" onClick={() => setAplicarAlergias(false)}>
                Ver todas
              </Boton>
            </p>
          )}
        </>
      )}
    </>
  )
}

/** Una receta en el listado (wireframe 21). */
export function TarjetaReceta({ vista, ruta }: { vista: RecetaVista; ruta: string }) {
  const { receta } = vista
  return (
    <Link className="fila tarjeta-receta" to={ruta}>
      <span className="fila__texto">
        <span className="fila__nombre">{receta.nombre}</span>
        <span className="fila__detalle">
          {receta.tiempoMinutos} min · {receta.ingredientes.length} ingredientes
        </span>
        {vista.costoPorcion !== undefined && (
          <span className="fila__detalle">{enPesos(vista.costoPorcion)} / porción</span>
        )}
        <span className="tarjeta-receta__marcas">
          {vista.faltan === 0 ? (
            <span className="pildora pildora--llena">tienes todo</span>
          ) : (
            <span className="pildora">
              {vista.faltan === 1 ? 'falta 1' : `faltan ${vista.faltan}`}
            </span>
          )}
          {receta.preparacionPrevia && <span className="pildora">preparación previa</span>}
          {vista.noAptaPara.length > 0 && (
            <span className="pildora pildora--alerta">no apta para {listaPersonas(vista.noAptaPara)}</span>
          )}
          {vista.noAptaPara.length === 0 && vista.ajustePara.length > 0 && (
            <span className="pildora pildora--ajuste">con ajuste para {listaPersonas(vista.ajustePara)}</span>
          )}
        </span>
      </span>
      <span className="fila__flecha" aria-hidden="true">
        ›
      </span>
    </Link>
  )
}

function cumple(vista: RecetaVista, buscar: string, atajo: Atajo, filtros: Filtros): boolean {
  const { receta } = vista
  const texto = normalizar(buscar.trim())
  if (
    texto &&
    !normalizar(receta.nombre).includes(texto) &&
    !vista.ingredientes.some((i) => normalizar(i.nombre).includes(texto))
  ) {
    return false
  }
  if (atajo === 'RAPIDAS' && receta.tiempoMinutos > MINUTOS_RAPIDA) return false
  if (atajo === 'ECONOMICAS' && (vista.costoPorcion ?? Infinity) > COSTO_ECONOMICA) return false
  if (atajo === 'CON_LO_QUE_TENGO' && vista.faltan > 0) return false
  if (filtros.tiempo !== 'SIN_LIMITE' && receta.tiempoMinutos > Number(filtros.tiempo)) return false
  if (filtros.ingredientes < MAX_INGREDIENTES && receta.ingredientes.length > filtros.ingredientes) return false
  if (filtros.costo !== 'CUALQUIERA' && (vista.costoPorcion ?? Infinity) > Number(filtros.costo)) return false
  if (filtros.soloHoy && vista.faltan > 0) return false
  return true
}

/** Sin tildes ni mayúsculas, para que "maní" encuentre "mani". */
function normalizar(texto: string): string {
  return texto.normalize('NFD').replace(/\p{Diacritic}/gu, '').toLowerCase()
}

function contar(total: number): string {
  return `${total} ${total === 1 ? 'receta' : 'recetas'}`
}
