import { useEffect, useState } from 'react'
import { Link } from 'react-router'
import { Alerta, Boton, Cargando, EncabezadoPagina, EstadoVacio } from '../../shared/componentes'
import { cantidadConUnidad, enPesos } from '../despensa/formato'
import { listaCompras } from './apiPlanificador'
import { planSinHogar } from './mensajesPlan'
import { ErrorCarga, PrimeroElHogar } from './PantallaPlan'
import type { ItemListaCompras, ListaCompras } from './tipos'
import { useSemana, type Semana } from './useSemana'
import './planificador.css'

type EstadoLista = { tipo: 'cargando' } | { tipo: 'listo'; lista: ListaCompras } | { tipo: 'error'; error: unknown }

/**
 * Pantalla 31: lista de compras del plan, priorizada. El backend ya descuenta lo que
 * hay en la despensa y deja por fuera los ingredientes opcionales. Lo que la usuaria
 * marca como comprado se recuerda en este teléfono.
 */
export function PantallaListaCompras() {
  const { estado, recargar } = useSemana()
  const [estadoLista, setEstadoLista] = useState<EstadoLista>({ tipo: 'cargando' })
  const [version, setVersion] = useState(0)
  const planId = estado.tipo === 'listo' ? estado.semana.plan?.id : undefined
  // Marcas de comprado de este plan; se leen del teléfono la primera vez.
  const [marcas, setMarcas] = useState<{ planId: number; ids: number[] }>()
  const comprados = planId === undefined ? [] : marcas?.planId === planId ? marcas.ids : leerComprados(planId)
  const [compartido, setCompartido] = useState(false)

  useEffect(() => {
    if (planId === undefined) {
      return
    }
    const control = new AbortController()
    listaCompras(planId, control.signal)
      .then((lista) => setEstadoLista({ tipo: 'listo', lista }))
      .catch((error: unknown) => {
        if (!control.signal.aborted) {
          setEstadoLista({ tipo: 'error', error })
        }
      })
    return () => control.abort()
  }, [planId, version])

  const volver = { ruta: '/plan', etiqueta: 'Volver a mi semana' }
  if (estado.tipo !== 'listo') {
    return (
      <>
        <EncabezadoPagina titulo="Lista de compras" volver={volver} />
        {estado.tipo === 'cargando' ? (
          <Cargando texto="Armando tu lista…" />
        ) : planSinHogar(estado.error) ? (
          <PrimeroElHogar />
        ) : (
          <ErrorCarga error={estado.error} reintentar={recargar} />
        )}
      </>
    )
  }

  const { semana } = estado
  if (!semana.plan) {
    return (
      <>
        <EncabezadoPagina titulo="Lista de compras" volver={volver} />
        <EstadoVacio
          titulo="Primero, tu semana"
          texto="La lista sale de las recetas que elijas para la semana, descontando lo que ya tienes en la despensa."
          acciones={
            <Link className="boton boton--primario boton--ancho" to="/plan">
              Armar mi semana
            </Link>
          }
        />
      </>
    )
  }
  if (estadoLista.tipo !== 'listo') {
    return (
      <>
        <EncabezadoPagina titulo="Lista de compras" volver={volver} />
        {estadoLista.tipo === 'cargando' ? (
          <Cargando texto="Armando tu lista…" />
        ) : (
          <ErrorCarga error={estadoLista.error} reintentar={() => setVersion((v) => v + 1)} />
        )}
      </>
    )
  }

  const { lista } = estadoLista
  const items = lista.items
  const marcados = items.filter((i) => comprados.includes(i.productoId)).length

  function alternar(productoId: number) {
    const nuevos = comprados.includes(productoId) ? comprados.filter((p) => p !== productoId) : [...comprados, productoId]
    setMarcas({ planId: planId!, ids: nuevos })
    guardarComprados(planId!, nuevos)
  }

  async function compartir() {
    const texto = textoLista(semana, items)
    try {
      if (navigator.share) {
        await navigator.share({ title: 'Lista de compras', text: texto })
      } else {
        await navigator.clipboard.writeText(texto)
        setCompartido(true)
      }
    } catch {
      // La usuaria cerró el menú de compartir: no es un error.
    }
  }

  if (items.length === 0) {
    const sinComidas = (semana.plan.comidas ?? []).length === 0
    return (
      <>
        <EncabezadoPagina titulo="Lista de compras" volver={volver} />
        <EstadoVacio
          titulo={sinComidas ? 'Tu semana está vacía' : 'No te falta nada'}
          texto={
            sinComidas
              ? 'Agrega recetas a tu semana y aquí verás lo que tienes que comprar.'
              : 'Con lo que tienes en la despensa alcanzas para todas las comidas del plan.'
          }
          acciones={
            <Link className="boton boton--primario boton--ancho" to="/plan">
              Ver mi semana
            </Link>
          }
        />
      </>
    )
  }

  const indispensables = items.filter((i) => i.prioridad <= 1)
  const postergables = items.filter((i) => i.prioridad > 1)

  return (
    <>
      <EncabezadoPagina
        titulo="Lista de compras"
        detalle={`${items.length} ${items.length === 1 ? 'producto' : 'productos'}`}
        volver={volver}
      />
      <section className="total-lista" aria-label="Costo estimado">
        <p className="total-lista__fila">
          <span>Costo estimado</span>
          <strong>{enPesos(lista.costoTotal)}</strong>
        </p>
        <p className="total-lista__nota">
          {marcados} de {items.length} marcados como comprados
        </p>
      </section>

      <Grupo titulo="Indispensable" items={indispensables} semana={semana} comprados={comprados} alternar={alternar} />
      <Grupo titulo="Postergable" items={postergables} semana={semana} comprados={comprados} alternar={alternar} />

      {compartido && <Alerta tono="exito">Copiamos la lista. Pégala donde quieras enviarla.</Alerta>}
      <div className="lista__acciones">
        <Boton variante="secundario" onClick={() => void compartir()}>
          Compartir
        </Boton>
        <Link className="boton boton--primario" to="/despensa">
          Ir a mi despensa
        </Link>
      </div>
    </>
  )
}

interface GrupoProps {
  titulo: string
  items: ItemListaCompras[]
  semana: Semana
  comprados: number[]
  alternar: (productoId: number) => void
}

function Grupo({ titulo, items, semana, comprados, alternar }: GrupoProps) {
  if (items.length === 0) {
    return null
  }
  return (
    <section className="lista__grupo" aria-label={titulo}>
      <h2 className="rotulo lista__titulo">{titulo}</h2>
      <ul className="filas">
        {items.map((item) => {
          const comprado = comprados.includes(item.productoId)
          return (
            <li key={item.productoId}>
              <label className={comprado ? 'compra compra--hecha' : 'compra'}>
                <input type="checkbox" checked={comprado} onChange={() => alternar(item.productoId)} />
                <span className="fila__texto">
                  <span className="fila__nombre compra__nombre">{nombreItem(semana, item)}</span>
                  <span className="fila__detalle">{recetasQueLoUsan(semana, item.productoId)}</span>
                </span>
                <span className="compra__costo">{enPesos(item.costoEstimado)}</span>
              </label>
            </li>
          )
        })}
      </ul>
    </section>
  )
}

/** "Zanahoria · 3 unidades". Lo contable se redondea hacia arriba: no se compra media unidad. */
function nombreItem(semana: Semana, item: ItemListaCompras): string {
  const producto = semana.cocina.productos.get(item.productoId)
  const unidad = producto?.unidadBase ?? 'unidad'
  const cantidad = unidad === 'unidad' ? Math.ceil(item.cantidad) : Math.round(item.cantidad)
  return `${producto?.nombre ?? `Producto ${item.productoId}`} · ${cantidadConUnidad(cantidad, unidad)}`
}

/** "Sopa de lentejas · Arroz con pollo": las recetas del plan que piden el producto. */
function recetasQueLoUsan(semana: Semana, productoId: number): string {
  const nombres = [...semana.recetas.values()]
    .filter((v) => v.ingredientes.some((i) => i.productoId === productoId && !i.opcional))
    .map((v) => v.receta.nombre)
  return nombres.join(' · ')
}

function textoLista(semana: Semana, items: ItemListaCompras[]): string {
  return ['Lista de compras · Cocina sin estrés', ...items.map((i) => `- ${nombreItem(semana, i)}`)].join('\n')
}

function clave(planId: number) {
  return `cocina-sin-estres:comprados:${planId}`
}

function leerComprados(planId: number): number[] {
  try {
    const guardado = JSON.parse(localStorage.getItem(clave(planId)) ?? '[]') as unknown
    return Array.isArray(guardado) ? guardado.filter((v): v is number => typeof v === 'number') : []
  } catch {
    return []
  }
}

function guardarComprados(planId: number, comprados: number[]) {
  try {
    localStorage.setItem(clave(planId), JSON.stringify(comprados))
  } catch {
    // Sin almacenamiento (modo privado): las marcas duran mientras la pantalla esté abierta.
  }
}
