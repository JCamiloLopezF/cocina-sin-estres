import { useEffect, useState } from 'react'
import { Alerta, Boton } from '../../shared/componentes'
import {
  listarProductos,
  listarReemplazos,
  mensajeCuraduria,
  quitarReemplazo,
  usarReemplazo,
  type ProductoCurado,
  type Reemplazo,
} from './apiCuraduria'

/**
 * Reemplazos de un producto para adaptar recetas a una alergia (ADR-24). La app los
 * propone sola (misma categoría, sin el alérgeno); el curador agrega los que la regla no
 * ve, quita los que no tienen sentido o los vuelve a usar.
 */
export function ReemplazosProducto({ productoId, nombre }: { productoId: number; nombre: string }) {
  const [reemplazos, setReemplazos] = useState<Reemplazo[]>()
  const [catalogo, setCatalogo] = useState<ProductoCurado[]>([])
  const [elegido, setElegido] = useState('')
  const [ocupado, setOcupado] = useState<number | 'agregar'>()
  const [error, setError] = useState<string>()

  useEffect(() => {
    const control = new AbortController()
    Promise.all([listarReemplazos(productoId, control.signal), listarProductos(control.signal)])
      .then(([lista, productos]) => {
        setReemplazos(lista)
        setCatalogo(productos)
      })
      .catch((causa: unknown) => {
        if (!control.signal.aborted) setError(mensajeCuraduria(causa))
      })
    return () => control.abort()
  }, [productoId])

  async function cambiar(id: number | 'agregar', accion: () => Promise<Reemplazo[]>) {
    setOcupado(id)
    setError(undefined)
    try {
      setReemplazos(await accion())
      setElegido('')
    } catch (causa) {
      setError(mensajeCuraduria(causa))
    } finally {
      setOcupado(undefined)
    }
  }

  const activos = (reemplazos ?? []).filter((r) => r.activo)
  const quitados = (reemplazos ?? []).filter((r) => !r.activo)
  const ocupados = new Set([productoId, ...activos.map((r) => r.reemplazoId)])
  const disponibles = catalogo.filter((p) => !ocupados.has(p.id))

  return (
    <section className="reemplazos" aria-labelledby="titulo-reemplazos">
      <h2 id="titulo-reemplazos">Reemplazos para alergias</h2>
      <p className="campo__ayuda">
        Si alguien es alérgico a {nombre.toLowerCase()}, en su porción se cambia por uno de estos. La app los propone sola
        (misma categoría y sin el alérgeno); aquí puedes corregirlos.
      </p>
      {error && <Alerta tono="error">{error}</Alerta>}
      {reemplazos === undefined && !error && <p className="campo__ayuda">Cargando reemplazos…</p>}

      {reemplazos !== undefined &&
        (activos.length === 0 ? (
          <p className="reemplazos__vacio">Sin reemplazos: si alguien es alérgico, su porción va sin este ingrediente.</p>
        ) : (
          <ul className="reemplazos__lista" aria-label="En uso">
            {activos.map((r) => (
              <li key={r.reemplazoId} className="reemplazos__fila">
                <span>
                  {r.nombre} <span className="reemplazos__origen">{r.origen === 'SISTEMA' ? 'automático' : 'agregado por curador'}</span>
                </span>
                <Boton
                  variante="secundario"
                  cargando={ocupado === r.reemplazoId}
                  disabled={ocupado !== undefined}
                  aria-label={`Quitar ${r.nombre}`}
                  onClick={() => void cambiar(r.reemplazoId, () => quitarReemplazo(productoId, r.reemplazoId))}
                >
                  Quitar
                </Boton>
              </li>
            ))}
          </ul>
        ))}

      {quitados.length > 0 && (
        <ul className="reemplazos__lista" aria-label="Quitados">
          {quitados.map((r) => (
            <li key={r.reemplazoId} className="reemplazos__fila reemplazos__fila--quitado">
              <span>
                {r.nombre} <span className="reemplazos__origen">quitado: la app no lo vuelve a proponer</span>
              </span>
              <Boton
                variante="texto"
                cargando={ocupado === r.reemplazoId}
                disabled={ocupado !== undefined}
                aria-label={`Volver a usar ${r.nombre}`}
                onClick={() => void cambiar(r.reemplazoId, () => usarReemplazo(productoId, r.reemplazoId))}
              >
                Volver a usar
              </Boton>
            </li>
          ))}
        </ul>
      )}

      {reemplazos !== undefined && disponibles.length > 0 && (
        <div className="reemplazos__agregar">
          <label className="campo__etiqueta" htmlFor="nuevo-reemplazo">
            Agregar un reemplazo
          </label>
          <div className="reemplazos__agregar-fila">
            <select
              id="nuevo-reemplazo"
              className="campo__control campo__control--lista"
              value={elegido}
              onChange={(e) => setElegido(e.target.value)}
            >
              <option value="">Elige un producto</option>
              {disponibles.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.nombre}
                </option>
              ))}
            </select>
            <Boton
              variante="secundario"
              disabled={!elegido || ocupado !== undefined}
              cargando={ocupado === 'agregar'}
              onClick={() => void cambiar('agregar', () => usarReemplazo(productoId, Number(elegido)))}
            >
              Agregar
            </Boton>
          </div>
        </div>
      )}
    </section>
  )
}
