import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import { Alerta, Boton, Cargando, EncabezadoPagina, EstadoVacio } from '../../shared/componentes'
import { actualizarItem, datosDe, eliminarItem, obtenerItem } from './apiDespensa'
import { cantidadConUnidad, describirCantidad, describirVencimiento, fechaCorta, NOMBRE_NIVEL } from './formato'
import { FormularioItem } from './FormularioItem'
import { mensajeDespensa } from './mensajesDespensa'
import type { AvisoDespensa } from './PantallaDespensa'
import type { ItemDespensa } from './tipos'
import './despensa.css'

type Estado = { tipo: 'cargando' } | { tipo: 'listo'; item: ItemDespensa } | { tipo: 'error'; error: unknown }

const pesos = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 })

/** Pantalla 19: detalle de un producto de la despensa, con editar y quitar. */
export function PantallaItemDespensa() {
  const { id } = useParams()
  const navegar = useNavigate()
  const [estado, setEstado] = useState<Estado>({ tipo: 'cargando' })
  const [editando, setEditando] = useState(false)
  const [confirmando, setConfirmando] = useState(false)
  const [quitando, setQuitando] = useState(false)
  const [errorQuitar, setErrorQuitar] = useState<string>()

  useEffect(() => {
    const control = new AbortController()
    obtenerItem(Number(id), control.signal)
      .then((item) => setEstado({ tipo: 'listo', item }))
      .catch((error: unknown) => {
        if (!control.signal.aborted) {
          setEstado({ tipo: 'error', error })
        }
      })
    return () => control.abort()
  }, [id])

  if (estado.tipo === 'cargando') {
    return <Cargando />
  }
  if (estado.tipo === 'error') {
    return (
      <>
        <EncabezadoPagina titulo="Mi despensa" />
        <EstadoVacio
          titulo="No encontramos ese producto"
          texto={mensajeDespensa(estado.error)}
          acciones={<Link to="/despensa">Volver a mi despensa</Link>}
        />
      </>
    )
  }

  const { item } = estado

  async function quitar() {
    setQuitando(true)
    setErrorQuitar(undefined)
    try {
      await eliminarItem(item.id)
      const aviso: AvisoDespensa = { texto: `Eliminaste ${item.nombre} de tu despensa.`, eliminado: item }
      navegar('/despensa', { state: { aviso } })
    } catch (error) {
      setErrorQuitar(mensajeDespensa(error))
      setQuitando(false)
    }
  }

  if (editando) {
    return (
      <>
        <EncabezadoPagina titulo={item.nombre} detalle="Editar" />
        <FormularioItem
          producto={item}
          inicial={datosDe(item)}
          permitirAgotado
          textoEnviar="Guardar cambios"
          alCancelar={() => setEditando(false)}
          alEnviar={async (datos) => {
            const actualizado = await actualizarItem(item.id, datos)
            setEstado({ tipo: 'listo', item: actualizado })
            setEditando(false)
          }}
        />
      </>
    )
  }

  const vencimiento = describirVencimiento(item)
  const minimo =
    item.tipoCantidad === 'GRANEL'
      ? item.nivelMinimo && NOMBRE_NIVEL[item.nivelMinimo]
      : item.cantidadMinima != null && cantidadConUnidad(item.cantidadMinima, item.unidadBase)
  const usarPronto = item.estadoVencimiento === 'POR_VENCER' || item.estadoVencimiento === 'VENCIDO'

  return (
    <>
      <EncabezadoPagina titulo={item.nombre} volver={{ ruta: '/despensa', etiqueta: 'Volver a mi despensa' }} />
      {usarPronto && (
        <p className="detalle__aviso">
          {mayuscula(vencimiento)} · {item.estadoVencimiento === 'VENCIDO' ? 'revísalo antes de usarlo' : 'úsalo pronto'}
        </p>
      )}
      {item.bajoMinimo && (
        <p className="detalle__aviso detalle__aviso--suave">Llegó a tu mínimo · tenlo en cuenta en la próxima compra</p>
      )}
      <dl className="detalle">
        <div className="detalle__dato">
          <dt>Cantidad</dt>
          <dd>
            {describirCantidad(item)}
            {item.esEstimada && <span className="detalle__nota"> (estimada)</span>}
          </dd>
        </div>
        <div className="detalle__dato">
          <dt>Vencimiento</dt>
          <dd>{item.fechaVencimiento ? fechaCorta(item.fechaVencimiento) : 'Sin fecha'}</dd>
        </div>
        <div className="detalle__dato">
          <dt>Nivel mínimo</dt>
          <dd>{minimo || 'Sin aviso'}</dd>
        </div>
        {item.ubicacion && (
          <div className="detalle__dato">
            <dt>Ubicación</dt>
            <dd>{item.ubicacion}</dd>
          </div>
        )}
        {item.precioUnidadCompra != null && item.unidadCompra && (
          <div className="detalle__dato">
            <dt>Precio de referencia</dt>
            <dd>
              {pesos.format(item.precioUnidadCompra)} / {item.unidadCompra.toLowerCase()}
            </dd>
          </div>
        )}
      </dl>

      <div className="detalle__acciones">
        <Boton ancho onClick={() => setEditando(true)}>
          Editar cantidad
        </Boton>
        {confirmando ? (
          <div className="confirmar" role="alertdialog" aria-labelledby="confirmar-eliminar">
            <p id="confirmar-eliminar">¿Eliminar {item.nombre} de tu despensa?</p>
            {errorQuitar && <Alerta tono="error">{errorQuitar}</Alerta>}
            <div className="formulario-item__acciones">
              <Boton variante="secundario" onClick={() => setConfirmando(false)}>
                No, dejarlo
              </Boton>
              <Boton cargando={quitando} onClick={() => void quitar()}>
                Sí, eliminar
              </Boton>
            </div>
          </div>
        ) : (
          <Boton variante="secundario" ancho onClick={() => setConfirmando(true)}>
            Eliminar de mi despensa
          </Boton>
        )}
      </div>
    </>
  )
}

function mayuscula(texto: string): string {
  return texto.charAt(0).toUpperCase() + texto.slice(1)
}
