import { useEffect, useState, type CSSProperties } from 'react'
import { Link, useLocation, useNavigate, useSearchParams } from 'react-router'
import { Alerta, Boton, Cargando, EncabezadoPagina, EstadoVacio } from '../../shared/componentes'
import { agregarItem, datosDe, listarDespensa } from './apiDespensa'
import { detalleItem, nombreCategoria, NOMBRE_NIVEL } from './formato'
import { faltaHogar, mensajeDespensa } from './mensajesDespensa'
import type { Filtro, ItemDespensa } from './tipos'
import './despensa.css'

const FILTROS: { valor: Filtro; texto: string }[] = [
  { valor: 'TODO', texto: 'Todo' },
  { valor: 'POR_VENCER', texto: 'Por vencer' },
  { valor: 'BAJO_MINIMO', texto: 'Bajo mínimo' },
]

/** Lo que llega en el estado de la navegación después de agregar, editar o quitar. */
export interface AvisoDespensa {
  texto: string
  /** Si se quitó un producto, lo necesario para deshacerlo (RNF-13). */
  eliminado?: ItemDespensa
}

type Estado =
  | { tipo: 'cargando' }
  | { tipo: 'listo'; items: ItemDespensa[] }
  | { tipo: 'error'; error: unknown }

/** Pantallas 14, 15 y 20: la despensa con búsqueda y filtros. */
export function PantallaDespensa() {
  const ubicacion = useLocation()
  const navegar = useNavigate()
  // El inicio puede abrir la despensa ya filtrada: /despensa?filtro=POR_VENCER
  const [parametros] = useSearchParams()
  const [filtro, setFiltro] = useState<Filtro>(() => filtroDesde(parametros.get('filtro')))
  const [buscar, setBuscar] = useState('')
  const [estado, setEstado] = useState<Estado>({ tipo: 'cargando' })
  const [version, setVersion] = useState(0)
  const [aviso, setAviso] = useState<AvisoDespensa | undefined>(
    () => (ubicacion.state as { aviso?: AvisoDespensa } | null)?.aviso,
  )
  const [deshaciendo, setDeshaciendo] = useState(false)

  // El aviso se muestra una sola vez: se limpia del historial para que no vuelva al recargar.
  useEffect(() => {
    if ((ubicacion.state as { aviso?: AvisoDespensa } | null)?.aviso) {
      navegar(`${ubicacion.pathname}${ubicacion.search}`, { replace: true, state: null })
    }
  }, [ubicacion, navegar])

  useEffect(() => {
    const control = new AbortController()
    // Espera un momento a que la usuaria termine de escribir antes de buscar.
    const espera = setTimeout(
      () => {
        listarDespensa(filtro, buscar, control.signal)
          .then((items) => setEstado({ tipo: 'listo', items }))
          .catch((error: unknown) => {
            if (!control.signal.aborted) {
              setEstado({ tipo: 'error', error })
            }
          })
      },
      buscar ? 250 : 0,
    )
    return () => {
      clearTimeout(espera)
      control.abort()
    }
  }, [filtro, buscar, version])

  async function deshacer(item: ItemDespensa) {
    setDeshaciendo(true)
    try {
      await agregarItem(item.productoId, datosDe(item))
      setAviso({ texto: `${item.nombre} volvió a tu despensa.` })
      setVersion((v) => v + 1)
    } catch (error) {
      setAviso({ texto: mensajeDespensa(error) })
    } finally {
      setDeshaciendo(false)
    }
  }

  if (estado.tipo === 'error' && faltaHogar(estado.error)) {
    return (
      <>
        <EncabezadoPagina titulo="Mi despensa" />
        <EstadoVacio
          titulo="Primero, tu hogar"
          texto="Cuéntanos quiénes comen en casa y cuál es tu presupuesto. Después podrás registrar lo que tienes en la despensa."
          acciones={
            <Link className="boton boton--primario boton--ancho" to="/perfil/hogar">
              Completar perfil del hogar
            </Link>
          }
        />
      </>
    )
  }

  const items = estado.tipo === 'listo' ? estado.items : []
  const sinNada = estado.tipo === 'listo' && items.length === 0 && filtro === 'TODO' && !buscar

  return (
    <>
      <EncabezadoPagina
        titulo="Mi despensa"
        detalle={estado.tipo === 'listo' && !sinNada ? contar(items.length, filtro, buscar) : undefined}
      />

      {aviso && (
        <div className="despensa__aviso">
          <Alerta tono="exito">
            {aviso.texto}{' '}
            {aviso.eliminado && (
              <Boton variante="texto" cargando={deshaciendo} onClick={() => void deshacer(aviso.eliminado!)}>
                Deshacer
              </Boton>
            )}
          </Alerta>
        </div>
      )}

      {sinNada ? (
        <EstadoVacio
          titulo="Aún no hay nada aquí"
          texto="Registra lo que tienes en casa para que podamos sugerirte recetas y armar tu lista de compras."
          acciones={
            <Link className="boton boton--primario boton--ancho" to="/despensa/agregar">
              Agregar el primer producto
            </Link>
          }
        />
      ) : (
        <>
          <div className="despensa__busqueda">
            <label className="solo-lectores" htmlFor="buscar-despensa">
              Buscar en mi despensa
            </label>
            <input
              id="buscar-despensa"
              className="campo__control"
              type="search"
              placeholder="Buscar en mi despensa"
              value={buscar}
              onChange={(e) => setBuscar(e.target.value)}
            />
          </div>
          <div className="filtros" role="group" aria-label="Mostrar">
            {FILTROS.map((f) => (
              <button
                key={f.valor}
                type="button"
                className="filtros__boton"
                aria-pressed={filtro === f.valor}
                onClick={() => setFiltro(f.valor)}
              >
                {f.texto}
              </button>
            ))}
          </div>

          {estado.tipo === 'cargando' && <Cargando texto="Cargando tu despensa…" />}
          {estado.tipo === 'error' && (
            <Alerta tono="error">
              {mensajeDespensa(estado.error)}{' '}
              <Boton variante="texto" onClick={() => setVersion((v) => v + 1)}>
                Reintentar
              </Boton>
            </Alerta>
          )}
          {estado.tipo === 'listo' &&
            (items.length === 0 ? (
              <p className="despensa__sin-resultados">{sinResultados(filtro, buscar)}</p>
            ) : (
              <ListaDespensa items={items} agrupar={filtro === 'TODO'} />
            ))}

          <Link className="boton boton--primario boton--ancho despensa__agregar" to="/despensa/agregar">
            + Agregar producto
          </Link>
        </>
      )}
    </>
  )
}

function ListaDespensa({ items, agrupar }: { items: ItemDespensa[]; agrupar: boolean }) {
  if (!agrupar) {
    return <Filas items={items} />
  }
  const grupos = new Map<string, ItemDespensa[]>()
  items.forEach((item) => grupos.set(item.categoria, [...(grupos.get(item.categoria) ?? []), item]))
  return (
    <>
      {[...grupos].map(([categoria, delGrupo]) => (
        <section key={categoria} className="despensa__grupo" aria-labelledby={`grupo-${categoria}`}>
          <h2 id={`grupo-${categoria}`} className="rotulo despensa__categoria">
            {nombreCategoria(categoria)}
          </h2>
          <Filas items={delGrupo} />
        </section>
      ))}
    </>
  )
}

const LLENO_NIVEL = { AGOTADO: '0%', POCO: '25%', MEDIO: '55%', LLENO: '100%' } as const

/** Hay que usarlo ya: vencido o vence en 3 días o menos. */
function esUrgente(item: ItemDespensa): boolean {
  return item.estadoVencimiento === 'VENCIDO' || (item.estadoVencimiento === 'POR_VENCER' && (item.diasParaVencer ?? 99) <= 3)
}

function Filas({ items }: { items: ItemDespensa[] }) {
  return (
    <ul className="filas">
      {items.map((item) => {
        const urgente = esUrgente(item)
        const marca = urgente ? (item.estadoVencimiento === 'VENCIDO' ? 'vencido' : 'usar ya') : item.bajoMinimo ? 'comprar' : null
        return (
          <li key={item.id}>
            <Link className={marca ? 'fila fila--destacada' : 'fila'} to={`/despensa/${item.id}`}>
              <span className="fila__texto">
                <span className="fila__nombre">{item.nombre}</span>
                <span className="fila__detalle">{detalleItem(item)}</span>
              </span>
              {item.tipoCantidad === 'GRANEL' && item.nivel && !marca && (
                <span
                  className="nivel"
                  aria-hidden="true"
                  title={NOMBRE_NIVEL[item.nivel]}
                  style={{ '--lleno': LLENO_NIVEL[item.nivel] } as CSSProperties}
                />
              )}
              {marca ? (
                <span className="etiqueta">{marca}</span>
              ) : (
                <span className="fila__flecha" aria-hidden="true">
                  ›
                </span>
              )}
            </Link>
          </li>
        )
      })}
    </ul>
  )
}

function filtroDesde(texto: string | null): Filtro {
  return texto === 'POR_VENCER' || texto === 'BAJO_MINIMO' ? texto : 'TODO'
}

function contar(total: number, filtro: Filtro, buscar: string): string {
  const palabra = total === 1 ? 'producto' : 'productos'
  if (buscar || filtro !== 'TODO') {
    return `${total} ${total === 1 ? 'resultado' : 'resultados'}`
  }
  return `${total} ${palabra}`
}

function sinResultados(filtro: Filtro, buscar: string): string {
  if (buscar) {
    return `No tienes nada que coincida con "${buscar}".`
  }
  if (filtro === 'POR_VENCER') {
    return 'Nada vence en los próximos 7 días.'
  }
  return 'Ningún producto está en su mínimo.'
}
