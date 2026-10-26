import { useEffect, useState, type FormEvent } from 'react'
import { Alerta, Boton, CampoTexto } from '../../shared/componentes'
import { useSesion } from '../auth/contextoSesion'
import { primerNombre } from '../cocinar/guion'
import { borrarTienda, crearTienda, listarTiendas, type Tienda } from './apiTiendas'
import { mensajeTienda } from './mensajesPlan'
import { cantidadParaPedir, enlaceWhatsapp, mensajePedido } from './pedidoWhatsapp'
import type { ItemListaCompras } from './tipos'
import type { Semana } from './useSemana'

interface Props {
  semana: Semana
  /** Lo que falta comprar: lo ya comprado no se pide. */
  pendientes: ItemListaCompras[]
}

/**
 * Pedir el mercado a la tienda del barrio por WhatsApp (KAN-53). La app arma el mensaje
 * con lo que falta y lo abre en WhatsApp; la usuaria lo revisa y lo envía ella misma.
 * Puede guardar hasta 5 tiendas con su número.
 */
export function PedirATienda({ semana, pendientes }: Props) {
  const { estado: sesion } = useSesion()
  const firma = primerNombre(sesion.tipo === 'con-sesion' ? sesion.sesion.nombre : undefined)
  const [tiendas, setTiendas] = useState<Tienda[]>([])
  const [agregando, setAgregando] = useState(false)
  const [porQuitar, setPorQuitar] = useState<number>()
  const [aviso, setAviso] = useState<{ tono: 'exito' | 'error'; texto: string }>()

  useEffect(() => {
    const control = new AbortController()
    listarTiendas(control.signal)
      .then(setTiendas)
      // Sin tiendas guardadas igual se puede pedir: WhatsApp deja escoger el contacto.
      .catch(() => undefined)
    return () => control.abort()
  }, [])

  const lineas = pendientes.map((item) => {
    const producto = semana.cocina.productos.get(item.productoId)
    return { producto: producto?.nombre ?? `Producto ${item.productoId}`, cantidad: cantidadParaPedir(item.cantidad, producto) }
  })

  async function quitar(tienda: Tienda) {
    setAviso(undefined)
    try {
      await borrarTienda(tienda.id)
      setTiendas((antes) => antes.filter((t) => t.id !== tienda.id))
      setAviso({ tono: 'exito', texto: `Quitamos ${tienda.nombre} de tus tiendas.` })
    } catch (error) {
      setAviso({ tono: 'error', texto: mensajeTienda(error) })
    } finally {
      setPorQuitar(undefined)
    }
  }

  return (
    <section className="pedido" aria-labelledby="titulo-pedido">
      <h2 id="titulo-pedido" className="rotulo lista__titulo">
        Pedir a la tienda
      </h2>
      {lineas.length === 0 ? (
        <p className="pedido__nota">Ya marcaste todo como comprado: no hay nada que pedir.</p>
      ) : (
        <>
          <p className="pedido__nota">
            Armamos el mensaje con {lineas.length === 1 ? 'el producto que te falta' : `los ${lineas.length} productos que te faltan`}.
            Lo revisas en WhatsApp y lo envías tú.
          </p>
          <ul className="pedido__tiendas">
            {tiendas.map((tienda) => (
              <li key={tienda.id} className="pedido__tienda">
                <a
                  className="boton boton--primario"
                  href={enlaceWhatsapp(mensajePedido(lineas, tienda.nombre, firma), tienda.whatsapp)}
                  target="_blank"
                  rel="noopener noreferrer"
                >
                  Pedir a {tienda.nombre}
                </a>
                {porQuitar === tienda.id ? (
                  <span className="pedido__confirmar">
                    <Boton variante="secundario" onClick={() => void quitar(tienda)}>
                      Sí, quitar
                    </Boton>
                    <Boton variante="texto" onClick={() => setPorQuitar(undefined)}>
                      No
                    </Boton>
                  </span>
                ) : (
                  <Boton variante="texto" aria-label={`Quitar ${tienda.nombre}`} onClick={() => setPorQuitar(tienda.id)}>
                    Quitar
                  </Boton>
                )}
              </li>
            ))}
          </ul>
          {tiendas.length === 0 && (
            <a
              className="boton boton--primario boton--ancho"
              href={enlaceWhatsapp(mensajePedido(lineas, undefined, firma))}
              target="_blank"
              rel="noopener noreferrer"
            >
              Pedir por WhatsApp
            </a>
          )}
        </>
      )}
      {aviso && <Alerta tono={aviso.tono}>{aviso.texto}</Alerta>}
      {agregando ? (
        <FormularioTienda
          alGuardar={(tienda) => {
            setTiendas((antes) => [...antes, tienda].sort((a, b) => a.nombre.localeCompare(b.nombre, 'es')))
            setAgregando(false)
            setAviso({ tono: 'exito', texto: `Guardamos ${tienda.nombre}. Ya puedes pedirle desde aquí.` })
          }}
          cancelar={() => setAgregando(false)}
        />
      ) : (
        tiendas.length < 5 && (
          <Boton
            variante="texto"
            onClick={() => {
              setAviso(undefined)
              setAgregando(true)
            }}
          >
            {tiendas.length === 0 ? '+ Guardar el WhatsApp de mi tienda' : '+ Agregar otra tienda'}
          </Boton>
        )
      )}
    </section>
  )
}

function FormularioTienda({ alGuardar, cancelar }: { alGuardar: (tienda: Tienda) => void; cancelar: () => void }) {
  const [nombre, setNombre] = useState('')
  const [whatsapp, setWhatsapp] = useState('')
  const [guardando, setGuardando] = useState(false)
  const [error, setError] = useState<string>()

  async function enviar(evento: FormEvent) {
    evento.preventDefault()
    if (!nombre.trim() || !whatsapp.trim()) {
      setError('Escribe el nombre de la tienda y su celular.')
      return
    }
    setGuardando(true)
    setError(undefined)
    try {
      alGuardar(await crearTienda(nombre.trim(), whatsapp))
    } catch (causa) {
      setError(mensajeTienda(causa))
    } finally {
      setGuardando(false)
    }
  }

  return (
    <form className="pedido__formulario" aria-label="Nueva tienda" onSubmit={(e) => void enviar(e)}>
      <CampoTexto
        etiqueta="Nombre de la tienda"
        placeholder="Ej.: Tienda de don Jorge"
        maxLength={60}
        value={nombre}
        onChange={(e) => setNombre(e.target.value)}
      />
      <CampoTexto
        etiqueta="Celular con WhatsApp"
        ayuda="10 dígitos, como lo tienes guardado en tu celular"
        inputMode="tel"
        autoComplete="off"
        placeholder="300 123 4567"
        value={whatsapp}
        onChange={(e) => setWhatsapp(e.target.value)}
      />
      {error && <Alerta tono="error">{error}</Alerta>}
      <div className="pedido__acciones">
        <Boton type="submit" cargando={guardando}>
          Guardar tienda
        </Boton>
        <Boton variante="texto" onClick={cancelar}>
          Cancelar
        </Boton>
      </div>
    </form>
  )
}
