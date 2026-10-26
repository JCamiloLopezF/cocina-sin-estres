import { useEffect, useMemo, useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router'
import { Alerta, Boton, Cargando, EncabezadoPagina } from '../../shared/componentes'
import { enPesos, nombreCategoria } from '../despensa/formato'
import { listarProductos, mensajeCuraduria, type ProductoCurado } from './apiCuraduria'
import './curaduria.css'

type Estado = { tipo: 'cargando' } | { tipo: 'listo'; productos: ProductoCurado[] } | { tipo: 'error'; error: unknown }

const UNIDAD = { g: 'g', ml: 'ml', unidad: 'und' } as const

/** Pantalla 43: catálogo de productos con precios de referencia (rol CURADOR). */
export function PantallaCuraduria() {
  const ubicacion = useLocation()
  const navegar = useNavigate()
  const [estado, setEstado] = useState<Estado>({ tipo: 'cargando' })
  const [version, setVersion] = useState(0)
  const [buscar, setBuscar] = useState('')
  const [categoria, setCategoria] = useState('')
  const [aviso] = useState<string | undefined>(() => (ubicacion.state as { aviso?: string } | null)?.aviso)

  // El aviso se muestra una sola vez: se limpia del historial para que no vuelva al recargar.
  useEffect(() => {
    if ((ubicacion.state as { aviso?: string } | null)?.aviso) {
      navegar(ubicacion.pathname, { replace: true, state: null })
    }
  }, [ubicacion, navegar])

  useEffect(() => {
    const control = new AbortController()
    listarProductos(control.signal)
      .then((productos) => setEstado({ tipo: 'listo', productos }))
      .catch((error: unknown) => {
        if (!control.signal.aborted) {
          setEstado({ tipo: 'error', error })
        }
      })
    return () => control.abort()
  }, [version])

  const productos = useMemo(() => (estado.tipo === 'listo' ? estado.productos : []), [estado])
  const categorias = [...new Set(productos.map((p) => p.categoria))]
  const texto = buscar.trim().toLowerCase()
  const visibles = productos.filter(
    (p) => (!categoria || p.categoria === categoria) && (!texto || p.nombre.toLowerCase().includes(texto)),
  )

  return (
    <>
      <EncabezadoPagina
        titulo="Catálogo"
        detalle="Productos y precios de referencia que ven todas las usuarias."
        detalleAbajo
      />
      {aviso && <Alerta tono="exito">{aviso}</Alerta>}

      <div className="curaduria__barra">
        <Link className="boton boton--secundario" to="/curaduria/recetas">
          Recetas
        </Link>
        <Link className="boton boton--primario" to="/curaduria/productos/nuevo">
          + Nuevo producto
        </Link>
      </div>

      {estado.tipo === 'cargando' && <Cargando texto="Cargando el catálogo…" />}
      {estado.tipo === 'error' && (
        <Alerta tono="error">
          {mensajeCuraduria(estado.error)}{' '}
          <Boton variante="texto" onClick={() => setVersion((v) => v + 1)}>
            Reintentar
          </Boton>
        </Alerta>
      )}

      {estado.tipo === 'listo' && (
        <>
          <div className="curaduria__filtros">
            <label className="solo-lectores" htmlFor="buscar-producto">
              Buscar producto
            </label>
            <input
              id="buscar-producto"
              className="campo__control"
              type="search"
              placeholder="Buscar producto"
              value={buscar}
              onChange={(e) => setBuscar(e.target.value)}
            />
            <label className="solo-lectores" htmlFor="filtro-categoria">
              Categoría
            </label>
            <select
              id="filtro-categoria"
              className="campo__control campo__control--lista"
              value={categoria}
              onChange={(e) => setCategoria(e.target.value)}
            >
              <option value="">Todas las categorías</option>
              {categorias.map((c) => (
                <option key={c} value={c}>
                  {nombreCategoria(c)}
                </option>
              ))}
            </select>
          </div>

          <p className="curaduria__conteo">
            {visibles.length} de {productos.length} productos activos
          </p>
          <ul className="filas">
            {visibles.map((p) => (
              <li key={p.id}>
                <Link className="fila" to={`/curaduria/productos/${p.id}`} aria-label={`Editar ${p.nombre}`}>
                  <span className="fila__texto">
                    <span className="fila__nombre">{p.nombre}</span>
                    <span className="fila__detalle">
                      {nombreCategoria(p.categoria)} · {p.tipoCantidad === 'GRANEL' ? 'granel' : 'contable'} ·{' '}
                      {UNIDAD[p.unidadBase]}
                    </span>
                  </span>
                  <span className="curaduria__precio">
                    {enPesos(p.precioUnidadCompra)}
                    <span className="fila__detalle"> / {p.unidadCompra.toLowerCase()}</span>
                  </span>
                </Link>
              </li>
            ))}
          </ul>
          {visibles.length === 0 && <p className="curaduria__conteo">Ningún producto coincide.</p>}
        </>
      )}
    </>
  )
}
