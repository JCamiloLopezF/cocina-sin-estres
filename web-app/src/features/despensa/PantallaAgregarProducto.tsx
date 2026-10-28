import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router'
import { Alerta, Boton, Cargando, EncabezadoPagina } from '../../shared/componentes'
import { agregarItem, catalogoDisponible } from './apiDespensa'
import { detalleProducto } from './formato'
import { FormularioItem } from './FormularioItem'
import { mensajeDespensa } from './mensajesDespensa'
import type { AvisoDespensa } from './PantallaDespensa'
import type { ProductoDisponible } from './tipos'
import './despensa.css'

/** Lo que más se busca (wireframe 16). Tocar una sugerencia la escribe en el buscador. */
const SUGERENCIAS = ['Cebolla', 'Arroz', 'Papa', 'Huevos']

type Estado =
  | { tipo: 'cargando' }
  | { tipo: 'listo'; productos: ProductoDisponible[] }
  | { tipo: 'error'; error: unknown }

/** Pantalla 16: buscar en el catálogo y, al elegir, pantallas 17 o 18 según el producto. */
export function PantallaAgregarProducto() {
  const navegar = useNavigate()
  const [buscar, setBuscar] = useState('')
  const [estado, setEstado] = useState<Estado>({ tipo: 'cargando' })
  const [version, setVersion] = useState(0)
  const [elegido, setElegido] = useState<ProductoDisponible>()

  useEffect(() => {
    const control = new AbortController()
    const espera = setTimeout(
      () => {
        catalogoDisponible(buscar, control.signal)
          .then((productos) => setEstado({ tipo: 'listo', productos }))
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
  }, [buscar, version])

  if (elegido) {
    return (
      <>
        <EncabezadoPagina titulo={elegido.nombre} />
        <FormularioItem
          producto={elegido}
          textoEnviar="Guardar"
          ayudaVence="Si la dejas vacía, la calculamos con lo que suele durar el producto."
          alCancelar={() => setElegido(undefined)}
          alEnviar={async (datos) => {
            await agregarItem(elegido.id, datos)
            const aviso: AvisoDespensa = { texto: `Agregaste ${elegido.nombre} a tu despensa.` }
            navegar('/despensa', { state: { aviso } })
          }}
        />
      </>
    )
  }

  return (
    <>
      <EncabezadoPagina titulo="Agregar producto" detalle={<Link to="/despensa">Cerrar</Link>} />
      <div className="despensa__busqueda">
        <label className="solo-lectores" htmlFor="buscar-catalogo">
          Busca en el catálogo
        </label>
        <input
          id="buscar-catalogo"
          className="campo__control"
          type="search"
          placeholder="Busca en el catálogo: arroz, leche…"
          autoComplete="off"
          value={buscar}
          onChange={(e) => setBuscar(e.target.value)}
        />
      </div>

      {estado.tipo === 'cargando' && <Cargando texto="Cargando el catálogo…" />}
      {estado.tipo === 'error' && (
        <Alerta tono="error">
          {mensajeDespensa(estado.error)}{' '}
          <Boton variante="texto" onClick={() => setVersion((v) => v + 1)}>
            Reintentar
          </Boton>
        </Alerta>
      )}
      {estado.tipo === 'listo' &&
        (estado.productos.length === 0 ? (
          <p className="despensa__sin-resultados">No encontramos "{buscar}" en el catálogo.</p>
        ) : (
          <>
          <h2 className="rotulo despensa__seccion" id="resultados-catalogo">
            Resultados del catálogo
          </h2>
          <ul className="filas" aria-labelledby="resultados-catalogo">
            {estado.productos.map((producto) => (
              <li key={producto.id}>
                {producto.itemDespensaId ? (
                  <Link className="fila" to={`/despensa/${producto.itemDespensaId}`}>
                    <span className="fila__texto">
                      <span className="fila__nombre">{producto.nombre}</span>
                      <span className="fila__detalle">Ya está en tu despensa · editar cantidad</span>
                    </span>
                    <span className="fila__flecha" aria-hidden="true">
                      ›
                    </span>
                  </Link>
                ) : (
                  <button type="button" className="fila" onClick={() => setElegido(producto)}>
                    <span className="fila__texto">
                      <span className="fila__nombre">
                        <span className="solo-lectores">Agregar </span>
                        {producto.nombre}
                      </span>
                      <span className="fila__detalle">{detalleProducto(producto)}</span>
                    </span>
                    <span className="fila__mas" aria-hidden="true">
                      +
                    </span>
                  </button>
                )}
              </li>
            ))}
          </ul>
          </>
        ))}

      <Link
        className="despensa__propio"
        to={buscar.trim() ? `/despensa/adicional?nombre=${encodeURIComponent(buscar.trim())}` : '/despensa/adicional'}
      >
        ¿No está en el catálogo? Registra un producto propio
      </Link>

      {!buscar && (
        <>
          <h2 className="rotulo despensa__seccion" id="sugerencias-frecuentes">
            Sugerencias frecuentes
          </h2>
          <ul className="sugerencias" aria-labelledby="sugerencias-frecuentes">
            {SUGERENCIAS.map((nombre) => (
              <li key={nombre}>
                <button type="button" onClick={() => setBuscar(nombre)}>
                  {nombre}
                </button>
              </li>
            ))}
          </ul>
        </>
      )}
    </>
  )
}
