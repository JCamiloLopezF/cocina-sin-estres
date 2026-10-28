import { useEffect, useState, type FormEvent } from 'react'
import { useNavigate, useParams } from 'react-router'
import { Alerta, Boton, CampoTexto, Cargando, EncabezadoPagina, GrupoOpciones } from '../../shared/componentes'
import { nombreCategoria } from '../despensa/formato'
import type { TipoCantidad, UnidadBase } from '../despensa/tipos'
import {
  actualizarProducto,
  CATEGORIAS,
  crearProducto,
  desactivarProducto,
  mensajeCuraduria,
  obtenerProducto,
  type DatosProducto,
  type ProductoCurado,
} from './apiCuraduria'
import { ReemplazosProducto } from './ReemplazosProducto'
import './curaduria.css'

interface Formulario {
  nombre: string
  categoria: string
  tipoCantidad: TipoCantidad
  unidadBase: UnidadBase
  unidadCompra: string
  cantidadUnidadCompra: string
  precioUnidadCompra: string
  capacidadReferencia: string
  esBasico: boolean
  vidaUtilDias: string
}

type Errores = Partial<Record<keyof Formulario, string>>

const VACIO: Formulario = {
  nombre: '',
  categoria: 'GRANOS',
  tipoCantidad: 'CONTABLE',
  unidadBase: 'unidad',
  unidadCompra: '',
  cantidadUnidadCompra: '',
  precioUnidadCompra: '',
  capacidadReferencia: '',
  esBasico: false,
  vidaUtilDias: '',
}

const NOMBRE_UNIDAD: Record<UnidadBase, string> = { g: 'gramos', ml: 'mililitros', unidad: 'unidades' }

function desde(p: ProductoCurado): Formulario {
  return {
    nombre: p.nombre,
    categoria: p.categoria,
    tipoCantidad: p.tipoCantidad,
    unidadBase: p.unidadBase,
    unidadCompra: p.unidadCompra,
    cantidadUnidadCompra: String(p.cantidadUnidadCompra),
    precioUnidadCompra: String(p.precioUnidadCompra),
    capacidadReferencia: p.capacidadReferencia != null ? String(p.capacidadReferencia) : '',
    esBasico: p.esBasico,
    vidaUtilDias: p.vidaUtilDias != null ? String(p.vidaUtilDias) : '',
  }
}

/** Pantalla 44: crear o editar un producto del catálogo global con su precio de referencia. */
export function PantallaProductoCurado() {
  const { id } = useParams()
  const nuevo = id === undefined
  const navegar = useNavigate()
  const [formulario, setFormulario] = useState<Formulario>(VACIO)
  const [cargando, setCargando] = useState(!nuevo)
  const [errorCarga, setErrorCarga] = useState<unknown>()
  const [errores, setErrores] = useState<Errores>({})
  const [errorGeneral, setErrorGeneral] = useState<string>()
  const [guardando, setGuardando] = useState(false)
  const [confirmando, setConfirmando] = useState(false)

  useEffect(() => {
    if (nuevo) {
      return
    }
    const control = new AbortController()
    obtenerProducto(Number(id), control.signal)
      .then((p) => {
        setFormulario(desde(p))
        setCargando(false)
      })
      .catch((error: unknown) => {
        if (!control.signal.aborted) {
          setErrorCarga(error)
          setCargando(false)
        }
      })
    return () => control.abort()
  }, [id, nuevo])

  const cambiar = <K extends keyof Formulario>(campo: K, valor: Formulario[K]) =>
    setFormulario((f) => ({ ...f, [campo]: valor }))
  const granel = formulario.tipoCantidad === 'GRANEL'
  const unidad = NOMBRE_UNIDAD[formulario.unidadBase]
  const volver = { ruta: '/curaduria', etiqueta: 'Volver al catálogo' }

  async function guardar(evento: FormEvent) {
    evento.preventDefault()
    const nuevos = validar(formulario)
    setErrores(nuevos)
    setErrorGeneral(undefined)
    if (Object.values(nuevos).some(Boolean)) {
      return
    }
    const datos: DatosProducto = {
      nombre: formulario.nombre.trim(),
      categoria: formulario.categoria,
      tipoCantidad: formulario.tipoCantidad,
      unidadBase: formulario.unidadBase,
      unidadCompra: formulario.unidadCompra.trim(),
      cantidadUnidadCompra: Number(formulario.cantidadUnidadCompra),
      precioUnidadCompra: Number(formulario.precioUnidadCompra),
      capacidadReferencia: granel ? Number(formulario.capacidadReferencia) : undefined,
      esBasico: formulario.esBasico,
      vidaUtilDias: formulario.vidaUtilDias.trim() ? Number(formulario.vidaUtilDias) : null,
    }
    setGuardando(true)
    try {
      const guardado = nuevo ? await crearProducto(datos) : await actualizarProducto(Number(id), datos)
      navegar('/curaduria', { state: { aviso: `${guardado.nombre} quedó guardado en el catálogo.` } })
    } catch (error) {
      setErrorGeneral(mensajeCuraduria(error))
      setGuardando(false)
    }
  }

  async function desactivar() {
    setGuardando(true)
    try {
      await desactivarProducto(Number(id))
      navegar('/curaduria', { state: { aviso: `${formulario.nombre} ya no se ofrece en el catálogo.` } })
    } catch (error) {
      setErrorGeneral(mensajeCuraduria(error))
      setGuardando(false)
    }
  }

  if (cargando || errorCarga !== undefined) {
    return (
      <>
        <EncabezadoPagina titulo="Editar producto" volver={volver} />
        {cargando ? <Cargando texto="Cargando el producto…" /> : <Alerta tono="error">{mensajeCuraduria(errorCarga)}</Alerta>}
      </>
    )
  }

  return (
    <>
      <EncabezadoPagina titulo={nuevo ? 'Nuevo producto' : formulario.nombre || 'Editar producto'} volver={volver} />
      <form className="formulario-producto" onSubmit={guardar} noValidate>
        {errorGeneral && <Alerta tono="error">{errorGeneral}</Alerta>}
        <CampoTexto
          etiqueta="Nombre"
          name="nombre"
          autoComplete="off"
          value={formulario.nombre}
          onChange={(e) => cambiar('nombre', e.target.value)}
          error={errores.nombre}
        />
        <div className="campo">
          <label className="campo__etiqueta" htmlFor="categoria-producto">
            Categoría
          </label>
          <select
            id="categoria-producto"
            className="campo__control campo__control--lista"
            value={formulario.categoria}
            onChange={(e) => cambiar('categoria', e.target.value)}
          >
            {[...new Set([...CATEGORIAS, formulario.categoria])].map((c) => (
              <option key={c} value={c}>
                {nombreCategoria(c)}
              </option>
            ))}
          </select>
        </div>
        <GrupoOpciones<TipoCantidad>
          leyenda="Tipo de cantidad"
          ayuda="Contable se cuenta; a granel se registra por nivel (lleno, medio, poco)."
          variante="segmentos"
          opciones={[
            { valor: 'CONTABLE', texto: 'Contable' },
            { valor: 'GRANEL', texto: 'A granel' },
          ]}
          valor={formulario.tipoCantidad}
          alCambiar={(tipo) =>
            setFormulario((f) => ({
              ...f,
              tipoCantidad: tipo,
              // Lo contable va en unidades; lo que va a granel, en gramos o mililitros.
              unidadBase: tipo === 'CONTABLE' ? 'unidad' : f.unidadBase === 'unidad' ? 'g' : f.unidadBase,
            }))
          }
        />
        <GrupoOpciones<UnidadBase>
          leyenda="Unidad base"
          variante="segmentos"
          opciones={[
            { valor: 'unidad', texto: 'Unidades' },
            { valor: 'g', texto: 'Gramos' },
            { valor: 'ml', texto: 'Mililitros' },
          ]}
          valor={formulario.unidadBase}
          alCambiar={(u) => cambiar('unidadBase', u)}
          error={errores.unidadBase}
        />
        {granel && (
          <CampoTexto
            etiqueta="Cantidad que equivale a «Lleno»"
            ayuda={`En ${unidad}. Sirve para estimar cuánto hay según el nivel.`}
            inputMode="decimal"
            value={formulario.capacidadReferencia}
            onChange={(e) => cambiar('capacidadReferencia', e.target.value)}
            error={errores.capacidadReferencia}
            sufijo={formulario.unidadBase}
          />
        )}

        <fieldset className="formulario-producto__precio">
          <legend className="campo__etiqueta">Precio de referencia</legend>
          <CampoTexto
            etiqueta="Presentación de compra"
            ayuda="Por ejemplo: Libra, Bolsa, Cubeta, Unidad."
            value={formulario.unidadCompra}
            onChange={(e) => cambiar('unidadCompra', e.target.value)}
            error={errores.unidadCompra}
          />
          <CampoTexto
            etiqueta="Cuánto trae"
            ayuda={`En ${unidad}.`}
            inputMode="decimal"
            value={formulario.cantidadUnidadCompra}
            onChange={(e) => cambiar('cantidadUnidadCompra', e.target.value)}
            error={errores.cantidadUnidadCompra}
            sufijo={formulario.unidadBase}
          />
          <CampoTexto
            etiqueta="Precio"
            inputMode="numeric"
            value={formulario.precioUnidadCompra}
            onChange={(e) => cambiar('precioUnidadCompra', e.target.value)}
            error={errores.precioUnidadCompra}
            sufijo="COP"
          />
        </fieldset>

        <CampoTexto
          etiqueta="Vida útil (opcional)"
          ayuda="Cuántos días dura desde que se compra, bien guardado. Con esto, lo que la usuaria compra entra a su despensa con fecha de vencimiento."
          inputMode="numeric"
          value={formulario.vidaUtilDias}
          onChange={(e) => cambiar('vidaUtilDias', e.target.value.replace(/\D/g, ''))}
          error={errores.vidaUtilDias}
          sufijo="días"
        />

        <label className="casilla casilla--tarjeta">
          Producto básico (se sugiere en la carga rápida)
          <input type="checkbox" checked={formulario.esBasico} onChange={(e) => cambiar('esBasico', e.target.checked)} />
        </label>

        <div className="formulario-item__acciones">
          <Boton variante="secundario" onClick={() => navegar('/curaduria')} disabled={guardando}>
            Cancelar
          </Boton>
          <Boton type="submit" cargando={guardando}>
            Guardar producto
          </Boton>
        </div>

        {!nuevo &&
          (confirmando ? (
            <div className="confirmar">
              <p>¿Quitar {formulario.nombre} del catálogo?</p>
              <span>Deja de ofrecerse a las usuarias; quienes ya lo tienen en su despensa lo conservan.</span>
              <div className="formulario-item__acciones">
                <Boton variante="secundario" onClick={() => setConfirmando(false)}>
                  No
                </Boton>
                <Boton cargando={guardando} onClick={() => void desactivar()}>
                  Sí, quitarlo
                </Boton>
              </div>
            </div>
          ) : (
            <Boton variante="texto" onClick={() => setConfirmando(true)}>
              Quitar del catálogo
            </Boton>
          ))}
      </form>
      {!nuevo && <ReemplazosProducto productoId={Number(id)} nombre={formulario.nombre} />}
    </>
  )
}

function positivo(texto: string, entero = false): boolean {
  const valor = Number(texto.replace(',', '.'))
  return texto.trim() !== '' && Number.isFinite(valor) && valor > 0 && (!entero || Number.isInteger(valor))
}

function validar(f: Formulario): Errores {
  return {
    nombre: !f.nombre.trim() ? 'Escribe el nombre.' : f.nombre.trim().length > 120 ? 'Máximo 120 caracteres.' : undefined,
    unidadBase:
      f.tipoCantidad === 'GRANEL' && f.unidadBase === 'unidad'
        ? 'A granel se mide en gramos o mililitros.'
        : f.tipoCantidad === 'CONTABLE' && f.unidadBase !== 'unidad'
          ? 'Lo contable se cuenta en unidades.'
          : undefined,
    capacidadReferencia:
      f.tipoCantidad === 'GRANEL' && !positivo(f.capacidadReferencia) ? 'Indica cuánto es «Lleno», mayor que cero.' : undefined,
    unidadCompra: f.unidadCompra.trim() ? undefined : 'Escribe la presentación de compra.',
    cantidadUnidadCompra: positivo(f.cantidadUnidadCompra) ? undefined : 'Indica cuánto trae, mayor que cero.',
    precioUnidadCompra:
      f.precioUnidadCompra.trim() !== '' && Number(f.precioUnidadCompra) >= 0 ? undefined : 'Escribe el precio en pesos.',
    vidaUtilDias:
      f.vidaUtilDias.trim() === '' || (Number(f.vidaUtilDias) >= 1 && Number(f.vidaUtilDias) <= 3650)
        ? undefined
        : 'Entre 1 y 3650 días, o déjalo vacío.',
  }
}
