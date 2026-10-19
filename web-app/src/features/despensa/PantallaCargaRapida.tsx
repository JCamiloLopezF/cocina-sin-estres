import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router'
import { Alerta, Boton, Cargando, EncabezadoPagina } from '../../shared/componentes'
import { agregarItem, catalogoDisponible } from './apiDespensa'
import { nombreCategoria } from './formato'
import { mensajeDespensa } from './mensajesDespensa'
import type { DatosItem, ProductoDisponible } from './tipos'
import './despensa.css'

type Estado = { tipo: 'cargando' } | { tipo: 'listo'; productos: ProductoDisponible[] } | { tipo: 'error'; error: unknown }

/**
 * Pantalla 11: carga inicial rápida. La usuaria marca lo que suele tener y se agrega con
 * una cantidad de partida (nivel "Medio" a granel; una unidad de compra si se cuenta).
 * Después ajusta las cantidades en la despensa.
 */
export function PantallaCargaRapida() {
  const navegar = useNavigate()
  const [estado, setEstado] = useState<Estado>({ tipo: 'cargando' })
  const [version, setVersion] = useState(0)
  const [marcados, setMarcados] = useState<number[]>([])
  const [guardando, setGuardando] = useState(false)
  const [error, setError] = useState<unknown>()

  useEffect(() => {
    const control = new AbortController()
    catalogoDisponible('', control.signal)
      .then((productos) => setEstado({ tipo: 'listo', productos: productos.filter((p) => !p.itemDespensaId) }))
      .catch((causa: unknown) => {
        if (!control.signal.aborted) {
          setEstado({ tipo: 'error', error: causa })
        }
      })
    return () => control.abort()
  }, [version])

  async function agregar(productos: ProductoDisponible[]) {
    setGuardando(true)
    setError(undefined)
    const elegidos = productos.filter((p) => marcados.includes(p.id))
    try {
      await Promise.all(elegidos.map((p) => agregarItem(p.id, datosIniciales(p))))
      navegar('/listo', { replace: true })
    } catch (causa) {
      setError(causa)
      setGuardando(false)
    }
  }

  const omitir = (
    <Link className="encabezado__accion" to="/listo">
      Omitir
    </Link>
  )

  if (estado.tipo !== 'listo') {
    return (
      <>
        <EncabezadoPagina titulo="Carga rápida" detalle={omitir} />
        {estado.tipo === 'cargando' ? (
          <Cargando texto="Cargando productos…" />
        ) : (
          <Alerta tono="error">
            {mensajeDespensa(estado.error)}{' '}
            <Boton variante="texto" onClick={() => setVersion((v) => v + 1)}>
              Reintentar
            </Boton>
          </Alerta>
        )}
      </>
    )
  }

  const grupos = new Map<string, ProductoDisponible[]>()
  estado.productos.forEach((p) => grupos.set(p.categoria, [...(grupos.get(p.categoria) ?? []), p]))
  const total = marcados.length

  return (
    <>
      <EncabezadoPagina titulo="Carga rápida" detalle={omitir} />
      <p className="carga__intro">Marca lo que sueles tener en casa. Después ajustas las cantidades.</p>

      {[...grupos].map(([categoria, productos]) => (
        <section key={categoria} className="carga__grupo" aria-labelledby={`carga-${categoria}`}>
          <h2 id={`carga-${categoria}`} className="rotulo despensa__categoria">
            {nombreCategoria(categoria)}
          </h2>
          <div className="filtros filtros--envolver" role="group" aria-labelledby={`carga-${categoria}`}>
            {productos.map((p) => (
              <button
                key={p.id}
                type="button"
                className="filtros__boton"
                aria-pressed={marcados.includes(p.id)}
                onClick={() => setMarcados((m) => (m.includes(p.id) ? m.filter((id) => id !== p.id) : [...m, p.id]))}
              >
                {p.nombre}
              </button>
            ))}
          </div>
        </section>
      ))}

      {error !== undefined && <Alerta tono="error">{mensajeDespensa(error)}</Alerta>}
      <Boton ancho cargando={guardando} disabled={total === 0} onClick={() => void agregar(estado.productos)}>
        {total === 0 ? 'Marca al menos un producto' : `Agregar ${total} ${total === 1 ? 'producto' : 'productos'}`}
      </Boton>
    </>
  )
}

function datosIniciales(producto: ProductoDisponible): DatosItem {
  return producto.tipoCantidad === 'GRANEL' ? { nivel: 'MEDIO' } : { cantidad: producto.cantidadUnidadCompra ?? 1 }
}
