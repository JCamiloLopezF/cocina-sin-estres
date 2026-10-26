import { useEffect, useState, type FormEvent } from 'react'
import { Link } from 'react-router'
import { Alerta, Boton, CampoTexto, Cargando, EncabezadoPagina, EstadoVacio } from '../../shared/componentes'
import { cantidadConUnidad, enPesos, fechaCorta, leerPesos } from '../despensa/formato'
import { cambiarCostoItem, listaCompras, marcarComprado } from './apiPlanificador'
import { mensajePlan, planSinHogar } from './mensajesPlan'
import { ErrorCarga, PrimeroElHogar } from './PantallaPlan'
import { PedirATienda } from './PedirATienda'
import type { ItemListaCompras, ListaCompras } from './tipos'
import { conSemana } from './semana'
import { useSemana, type Semana } from './useSemana'
import './planificador.css'

type EstadoLista = { tipo: 'cargando' } | { tipo: 'listo'; lista: ListaCompras } | { tipo: 'error'; error: unknown }

/**
 * Pantalla 31: lista de compras del plan, priorizada. El backend ya descuenta lo que
 * hay en la despensa y deja por fuera los ingredientes opcionales. Lo que la usuaria
 * marca como comprado pasa solo a la despensa, con su vencimiento (ADR-24). Si en la
 * tienda pagó otro precio, lo corrige en el producto y el total se ajusta (KAN-26).
 */
export function PantallaListaCompras() {
  const { estado, recargar, proxima } = useSemana()
  const [estadoLista, setEstadoLista] = useState<EstadoLista>({ tipo: 'cargando' })
  const [version, setVersion] = useState(0)
  const planId = estado.tipo === 'listo' ? estado.semana.plan?.id : undefined
  const [compartido, setCompartido] = useState(false)
  const [ocupado, setOcupado] = useState<number>()
  const [editando, setEditando] = useState<number>()
  const [aviso, setAviso] = useState<{ tono: 'exito' | 'error'; texto: string }>()

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

  const volver = { ruta: conSemana('/plan', proxima), etiqueta: 'Volver a mi semana' }
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
            <Link className="boton boton--primario boton--ancho" to={conSemana('/plan', proxima)}>
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
  const marcados = items.filter((i) => i.comprado).length

  /** Comprado → entra a la despensa; desmarcado → sale. Lo hace el backend (ADR-24). */
  async function alternar(item: ItemListaCompras) {
    setOcupado(item.id)
    setAviso(undefined)
    try {
      const { item: actualizado, venceEn } = await marcarComprado(planId!, item.id, !item.comprado)
      setEstadoLista({
        tipo: 'listo',
        lista: { ...lista, items: lista.items.map((i) => (i.id === actualizado.id ? actualizado : i)) },
      })
      const nombre = semana.cocina.productos.get(item.productoId)?.nombre ?? 'El producto'
      setAviso({
        tono: 'exito',
        texto: actualizado.comprado
          ? `${nombre} pasó a tu despensa${venceEn ? `; vence el ${fechaCorta(venceEn)}` : ''}.`
          : `Quitamos ${nombre.toLowerCase()} de tu despensa.`,
      })
    } catch (error) {
      setAviso({ tono: 'error', texto: mensajePlan(error) })
    } finally {
      setOcupado(undefined)
    }
  }

  /** Guarda lo que pagó de verdad (o vuelve al estimado con `null`); el backend devuelve el total nuevo. */
  async function guardarCosto(item: ItemListaCompras, costoReal: number | null) {
    setOcupado(item.id)
    setAviso(undefined)
    try {
      const actualizada = await cambiarCostoItem(planId!, item.id, costoReal)
      setEstadoLista({ tipo: 'listo', lista: actualizada })
      setEditando(undefined)
      const nombre = semana.cocina.productos.get(item.productoId)?.nombre ?? 'el producto'
      setAviso({
        tono: 'exito',
        texto:
          costoReal === null
            ? `Volvimos al precio estimado de ${nombre.toLowerCase()}.`
            : `Anotamos ${enPesos(costoReal)} por ${nombre.toLowerCase()}. El total quedó en ${enPesos(actualizada.costoTotal)}.`,
      })
    } catch (error) {
      setAviso({ tono: 'error', texto: mensajePlan(error) })
    } finally {
      setOcupado(undefined)
    }
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
            <Link className="boton boton--primario boton--ancho" to={conSemana('/plan', proxima)}>
              Ver mi semana
            </Link>
          }
        />
      </>
    )
  }

  const indispensables = items.filter((i) => i.prioridad <= 1)
  const postergables = items.filter((i) => i.prioridad > 1)
  const corregidos = items.filter((i) => i.costoReal != null).length
  const edicion: Edicion = {
    editando,
    editar: (item) => {
      setAviso(undefined)
      setEditando(item?.id)
    },
    guardar: (item, valor) => void guardarCosto(item, valor),
  }

  return (
    <>
      <EncabezadoPagina
        titulo="Lista de compras"
        detalle={`${items.length} ${items.length === 1 ? 'producto' : 'productos'}`}
        volver={volver}
      />
      <section className="total-lista" aria-label="Total de la lista">
        <p className="total-lista__fila">
          <span>{corregidos ? 'Total con tus precios' : 'Costo estimado'}</span>
          <strong>{enPesos(lista.costoTotal)}</strong>
        </p>
        <p className="total-lista__nota">
          {marcados} de {items.length} comprados · lo que marques pasa solo a tu despensa
        </p>
        <p className="total-lista__nota">
          {corregidos
            ? `Corregiste el precio de ${corregidos} ${corregidos === 1 ? 'producto' : 'productos'}; el resto es estimado.`
            : '¿Pagaste otro precio? Toca el valor del producto para corregirlo.'}
        </p>
      </section>
      {aviso && (
        <div className="lista__aviso">
          <Alerta tono={aviso.tono}>{aviso.texto}</Alerta>
        </div>
      )}

      <Grupo
        titulo="Indispensable"
        items={indispensables}
        semana={semana}
        ocupado={ocupado}
        alternar={(i) => void alternar(i)}
        edicion={edicion}
      />
      <Grupo
        titulo="Para reponer"
        items={postergables}
        semana={semana}
        ocupado={ocupado}
        alternar={(i) => void alternar(i)}
        edicion={edicion}
      />

      <PedirATienda semana={semana} pendientes={items.filter((i) => !i.comprado)} />

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

/** Corregir el precio de un producto: uno a la vez. */
interface Edicion {
  editando?: number
  editar: (item?: ItemListaCompras) => void
  guardar: (item: ItemListaCompras, costoReal: number | null) => void
}

interface GrupoProps {
  titulo: string
  items: ItemListaCompras[]
  semana: Semana
  ocupado?: number
  alternar: (item: ItemListaCompras) => void
  edicion: Edicion
}

function Grupo({ titulo, items, semana, ocupado, alternar, edicion }: GrupoProps) {
  if (items.length === 0) {
    return null
  }
  return (
    <section className="lista__grupo" aria-label={titulo}>
      <h2 className="rotulo lista__titulo">{titulo}</h2>
      <ul className="filas">
        {items.map((item) => {
          const comprado = item.comprado
          const nombre = semana.cocina.productos.get(item.productoId)?.nombre ?? `Producto ${item.productoId}`
          const corregido = item.costoReal != null
          return (
            <li key={item.productoId}>
              <div className={comprado ? 'compra compra--hecha' : 'compra'}>
                <label className="compra__marca">
                  <input
                    type="checkbox"
                    checked={comprado}
                    disabled={ocupado === item.id}
                    onChange={() => alternar(item)}
                  />
                  <span className="fila__texto">
                    <span className="fila__nombre compra__nombre">{nombreItem(semana, item)}</span>
                    <span className="fila__detalle">{motivo(semana, item)}</span>
                  </span>
                </label>
                <button
                  type="button"
                  className="compra__costo"
                  aria-label={`Corregir el precio de ${nombre}: ${enPesos(item.costoReal ?? item.costoEstimado)}`}
                  aria-expanded={edicion.editando === item.id}
                  onClick={() => edicion.editar(edicion.editando === item.id ? undefined : item)}
                >
                  <span className="compra__valor">{enPesos(item.costoReal ?? item.costoEstimado)}</span>
                  <span className="compra__estimado">{corregido ? `estimado ${enPesos(item.costoEstimado)}` : 'estimado'}</span>
                </button>
              </div>
              {edicion.editando === item.id && (
                <EditarCosto
                  item={item}
                  nombre={nombre}
                  ocupado={ocupado === item.id}
                  guardar={(valor) => edicion.guardar(item, valor)}
                  cancelar={() => edicion.editar(undefined)}
                />
              )}
            </li>
          )
        })}
      </ul>
    </section>
  )
}

interface EditarCostoProps {
  item: ItemListaCompras
  nombre: string
  ocupado: boolean
  guardar: (costoReal: number | null) => void
  cancelar: () => void
}

/** Formulario para escribir lo que pagó en la tienda (KAN-26). */
function EditarCosto({ item, nombre, ocupado, guardar, cancelar }: EditarCostoProps) {
  const [texto, setTexto] = useState(() => (item.costoReal != null ? String(Math.round(item.costoReal)) : ''))
  const [error, setError] = useState<string>()

  function enviar(evento: FormEvent) {
    evento.preventDefault()
    const valor = leerPesos(texto)
    if (valor === undefined) {
      setError('Escribe cuánto pagaste, en pesos.')
      return
    }
    guardar(valor)
  }

  return (
    <form className="compra-editar" onSubmit={enviar} aria-label={`Precio de ${nombre}`}>
      <CampoTexto
        etiqueta={`¿Cuánto pagaste por ${nombre.toLowerCase()}?`}
        ayuda={`Estimado: ${enPesos(item.costoEstimado)}`}
        inputMode="numeric"
        autoComplete="off"
        placeholder="Ej.: 12.500"
        sufijo="COP"
        value={texto}
        error={error}
        onChange={(e) => {
          setTexto(e.target.value)
          setError(undefined)
        }}
        autoFocus
      />
      <div className="compra-editar__acciones">
        <Boton type="submit" cargando={ocupado}>
          Guardar
        </Boton>
        {item.costoReal != null ? (
          <Boton variante="secundario" onClick={() => guardar(null)} disabled={ocupado}>
            Usar el estimado
          </Boton>
        ) : (
          <Boton variante="texto" onClick={cancelar}>
            Cancelar
          </Boton>
        )}
      </div>
    </form>
  )
}

/** "Zanahoria · 3 unidades". Lo contable se redondea hacia arriba: no se compra media unidad. */
function nombreItem(semana: Semana, item: ItemListaCompras): string {
  const producto = semana.cocina.productos.get(item.productoId)
  const unidad = producto?.unidadBase ?? 'unidad'
  const cantidad = unidad === 'unidad' ? Math.ceil(item.cantidad) : Math.round(item.cantidad)
  return `${producto?.nombre ?? `Producto ${item.productoId}`} · ${cantidadConUnidad(cantidad, unidad)}`
}

/** Por qué está en la lista: las recetas que lo piden, reponer el mínimo o una alergia. */
function motivo(semana: Semana, item: ItemListaCompras): string {
  if (item.motivo === 'REPONER') return 'Quedó por debajo del mínimo de tu despensa'
  if (item.motivo === 'ALERGIA') return 'Para la porción de quien tiene alergia'
  return recetasQueLoUsan(semana, item.productoId)
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
