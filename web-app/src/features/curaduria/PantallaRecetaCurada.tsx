import { useEffect, useMemo, useState, type FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import { Alerta, Boton, CampoTexto, Cargando, EncabezadoPagina } from '../../shared/componentes'
import { enPesos, nombreCategoria } from '../despensa/formato'
import { COMIDAS, NOMBRE_COMIDA } from '../planificador/semana'
import {
  actualizarReceta,
  CATEGORIAS,
  crearReceta,
  desactivarReceta,
  listarProductos,
  mensajeCuraduria,
  obtenerRecetaCurada,
  reactivarReceta,
  type DatosReceta,
  type ProductoCurado,
  type RecetaCurada,
} from './apiCuraduria'
import {
  costoDe,
  numero,
  pasosDesde,
  problemaDelPaso,
  validarReceta,
  type ErroresReceta,
  type FilaIngrediente,
  type FormularioReceta,
} from './recetaCurada'
import './curaduria.css'

let siguienteClave = 1
const fila = (datos: Partial<FilaIngrediente> = {}): FilaIngrediente => ({
  clave: siguienteClave++,
  productoId: undefined,
  cantidad: '',
  opcional: false,
  principal: false,
  ...datos,
})

const VACIO = (): FormularioReceta => ({
  nombre: '',
  descripcion: '',
  tiempoMinutos: '',
  porciones: '4',
  pasos: '',
  preparacionPrevia: '',
  ingredientes: [fila(), fila(), fila()],
  momentos: [],
  platoFuerte: true,
  gustaANinos: false,
})

function desde(r: RecetaCurada): FormularioReceta {
  return {
    nombre: r.nombre,
    descripcion: r.descripcion ?? '',
    tiempoMinutos: String(r.tiempoMinutos),
    porciones: String(r.porciones),
    pasos: r.pasos.join('\n'),
    preparacionPrevia: r.preparacionPrevia ?? '',
    ingredientes: r.ingredientes.map((i) =>
      fila({ productoId: i.productoId, cantidad: String(i.cantidad), opcional: i.opcional, principal: i.principal }),
    ),
    momentos: r.momentos,
    platoFuerte: r.platoFuerte,
    gustaANinos: r.gustaANinos,
  }
}

const UNIDAD = { g: 'g', ml: 'ml', unidad: 'und.' } as const

/**
 * Crear o editar una receta (RNF-28: agregar una receta en menos de 10 minutos sin tocar
 * código). Avisa mientras se escribe lo que la API va a pedir: un tiempo por paso y que un
 * almuerzo sea un plato completo.
 */
export function PantallaRecetaCurada() {
  const { id } = useParams()
  const nueva = id === undefined
  const navegar = useNavigate()
  const [formulario, setFormulario] = useState<FormularioReceta>(VACIO)
  const [receta, setReceta] = useState<RecetaCurada>()
  const [productos, setProductos] = useState<ProductoCurado[]>([])
  const [cargando, setCargando] = useState(true)
  const [errorCarga, setErrorCarga] = useState<unknown>()
  const [errores, setErrores] = useState<ErroresReceta>({})
  const [intento, setIntento] = useState(false)
  const [errorGeneral, setErrorGeneral] = useState<string>()
  const [guardando, setGuardando] = useState(false)
  const [confirmando, setConfirmando] = useState(false)

  useEffect(() => {
    const control = new AbortController()
    Promise.all([listarProductos(control.signal), nueva ? Promise.resolve(undefined) : obtenerRecetaCurada(Number(id), control.signal)])
      .then(([catalogo, existente]) => {
        setProductos(catalogo)
        if (existente) {
          setReceta(existente)
          setFormulario(desde(existente))
        }
        setCargando(false)
      })
      .catch((error: unknown) => {
        if (!control.signal.aborted) {
          setErrorCarga(error)
          setCargando(false)
        }
      })
    return () => control.abort()
  }, [id, nueva])

  // Los productos de la receta que el curador ya desactivó también se muestran, para poder cambiarlos.
  const porId = useMemo(() => {
    const mapa = new Map(productos.map((p) => [p.id, p]))
    receta?.ingredientes
      .filter((i) => !mapa.has(i.productoId))
      .forEach((i) =>
        mapa.set(i.productoId, {
          id: i.productoId,
          nombre: `${i.nombre} (desactivado)`,
          categoria: i.categoria,
          tipoCantidad: i.unidadBase === 'unidad' ? 'CONTABLE' : 'GRANEL',
          unidadBase: i.unidadBase,
          unidadCompra: '',
          cantidadUnidadCompra: 0,
          precioUnidadCompra: 0,
          esBasico: false,
          activo: false,
        }),
      )
    return mapa
  }, [productos, receta])
  const porCategoria = useMemo(
    () =>
      [...new Set([...CATEGORIAS, ...productos.map((p) => p.categoria)])]
        .map((categoria) => ({
          categoria,
          productos: productos.filter((p) => p.categoria === categoria).sort((a, b) => a.nombre.localeCompare(b.nombre, 'es')),
        }))
        .filter((g) => g.productos.length > 0),
    [productos],
  )

  const cambiar = <K extends keyof FormularioReceta>(campo: K, valor: FormularioReceta[K]) => {
    const nuevo = { ...formulario, [campo]: valor }
    setFormulario(nuevo)
    // Después del primer intento de guardar, los errores se quitan apenas se corrigen.
    if (intento) setErrores(validarReceta(nuevo, porId))
  }
  const cambiarFila = (clave: number, cambio: Partial<FilaIngrediente>) =>
    cambiar(
      'ingredientes',
      formulario.ingredientes.map((i) => (i.clave === clave ? { ...i, ...cambio } : i)),
    )

  const volver = { ruta: '/curaduria/recetas', etiqueta: 'Volver a las recetas' }
  const costo = costoDe(formulario.ingredientes, porId)
  const porciones = numero(formulario.porciones)
  const pasos = pasosDesde(formulario.pasos)
  const problemas = pasos.map((p, i) => ({ numero: i + 1, problema: problemaDelPaso(p) })).filter((p) => p.problema)

  async function guardar(evento: FormEvent) {
    evento.preventDefault()
    setIntento(true)
    const nuevos = validarReceta(formulario, porId)
    setErrores(nuevos)
    setErrorGeneral(undefined)
    if (Object.values(nuevos).some(Boolean)) return
    const datos: DatosReceta = {
      nombre: formulario.nombre.trim(),
      descripcion: formulario.descripcion.trim() || undefined,
      tiempoMinutos: numero(formulario.tiempoMinutos),
      porciones: numero(formulario.porciones),
      pasos,
      preparacionPrevia: formulario.preparacionPrevia.trim() || undefined,
      ingredientes: formulario.ingredientes
        .filter((i) => i.productoId !== undefined)
        .map((i) => ({ productoId: i.productoId as number, cantidad: numero(i.cantidad), opcional: i.opcional, principal: i.principal })),
      momentos: COMIDAS.filter((c) => formulario.momentos.includes(c)),
      platoFuerte: formulario.platoFuerte,
      gustaANinos: formulario.gustaANinos,
    }
    setGuardando(true)
    try {
      const guardada = nueva ? await crearReceta(datos) : await actualizarReceta(Number(id), datos)
      navegar('/curaduria/recetas', {
        state: { aviso: `${guardada.nombre} quedó guardada. Ya aparece en el recetario de las usuarias.` },
      })
    } catch (error) {
      setErrorGeneral(mensajeCuraduria(error))
      setGuardando(false)
    }
  }

  async function cambiarActiva() {
    if (!receta) return
    setGuardando(true)
    try {
      const resultado = receta.activa ? await desactivarReceta(receta.id) : await reactivarReceta(receta.id)
      navegar('/curaduria/recetas', {
        state: {
          aviso: resultado.activa
            ? `${resultado.nombre} vuelve a estar en el recetario.`
            : `${resultado.nombre} ya no se ofrece. Los planes que la tienen la conservan.`,
        },
      })
    } catch (error) {
      setErrorGeneral(mensajeCuraduria(error))
      setGuardando(false)
      setConfirmando(false)
    }
  }

  if (cargando || errorCarga !== undefined) {
    return (
      <>
        <EncabezadoPagina titulo={nueva ? 'Nueva receta' : 'Editar receta'} volver={volver} />
        {cargando ? <Cargando texto="Cargando la receta…" /> : <Alerta tono="error">{mensajeCuraduria(errorCarga)}</Alerta>}
      </>
    )
  }

  return (
    <>
      <EncabezadoPagina
        titulo={nueva ? 'Nueva receta' : formulario.nombre || 'Editar receta'}
        detalle={receta && !receta.activa ? 'Desactivada: no aparece en el recetario.' : undefined}
        detalleAbajo
        volver={volver}
      />
      <form className="formulario-producto formulario-receta" onSubmit={guardar} noValidate>
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
          <label className="campo__etiqueta" htmlFor="descripcion-receta">
            Descripción (opcional)
          </label>
          <span className="campo__ayuda">Una o dos frases: qué es y con qué se sirve.</span>
          <textarea
            id="descripcion-receta"
            className="campo__control formulario-receta__texto"
            rows={2}
            maxLength={500}
            value={formulario.descripcion}
            onChange={(e) => cambiar('descripcion', e.target.value)}
          />
        </div>
        <div className="formulario-receta__par">
          <CampoTexto
            etiqueta="Tiempo total"
            inputMode="numeric"
            value={formulario.tiempoMinutos}
            onChange={(e) => cambiar('tiempoMinutos', e.target.value.replace(/\D/g, ''))}
            error={errores.tiempoMinutos}
            sufijo="min"
          />
          <CampoTexto
            etiqueta="Porciones"
            ayuda="Para cuántas personas son las cantidades."
            inputMode="numeric"
            value={formulario.porciones}
            onChange={(e) => cambiar('porciones', e.target.value.replace(/\D/g, ''))}
            error={errores.porciones}
          />
        </div>

        <fieldset className="formulario-producto__precio" aria-invalid={errores.momentos ? true : undefined}>
          <legend className="campo__etiqueta">¿En qué comidas va?</legend>
          <span className="campo__ayuda">
            Un almuerzo es un plato completo: una proteína y algo que llene (arroz, pasta, papa, yuca, plátano o arepa).
          </span>
          <div className="filtros filtros--envolver">
            {COMIDAS.map((comida) => (
              <label key={comida} className="casilla casilla--tarjeta">
                {NOMBRE_COMIDA[comida]}
                <input
                  type="checkbox"
                  checked={formulario.momentos.includes(comida)}
                  onChange={(e) =>
                    cambiar(
                      'momentos',
                      e.target.checked ? [...formulario.momentos, comida] : formulario.momentos.filter((m) => m !== comida),
                    )
                  }
                />
              </label>
            ))}
          </div>
          {errores.momentos && <span className="campo__error">{errores.momentos}</span>}
          <label className="casilla casilla--tarjeta">
            Es plato fuerte (no un jugo ni un acompañamiento)
            <input type="checkbox" checked={formulario.platoFuerte} onChange={(e) => cambiar('platoFuerte', e.target.checked)} />
          </label>
          <label className="casilla casilla--tarjeta">
            Suele gustarles a los niños
            <input type="checkbox" checked={formulario.gustaANinos} onChange={(e) => cambiar('gustaANinos', e.target.checked)} />
          </label>
        </fieldset>

        <fieldset className="formulario-producto__precio">
          <legend className="campo__etiqueta">Ingredientes</legend>
          <span className="campo__ayuda">
            Cantidades para {porciones > 0 ? `${porciones} ${porciones === 1 ? 'porción' : 'porciones'}` : 'las porciones de arriba'}.
            El agua no se lista: va en los pasos.
          </span>
          <ul className="ingredientes-curados">
            {formulario.ingredientes.map((ingrediente, indice) => {
              const producto = ingrediente.productoId !== undefined ? porId.get(ingrediente.productoId) : undefined
              return (
                <li key={ingrediente.clave} className="ingredientes-curados__fila">
                  <div className="campo">
                    <label className="campo__etiqueta" htmlFor={`producto-${ingrediente.clave}`}>
                      Ingrediente {indice + 1}
                    </label>
                    <select
                      id={`producto-${ingrediente.clave}`}
                      className="campo__control campo__control--lista"
                      value={ingrediente.productoId ?? ''}
                      onChange={(e) => cambiarFila(ingrediente.clave, { productoId: e.target.value ? Number(e.target.value) : undefined })}
                    >
                      <option value="">Elige un producto</option>
                      {producto && !producto.activo && <option value={producto.id}>{producto.nombre}</option>}
                      {porCategoria.map((grupo) => (
                        <optgroup key={grupo.categoria} label={nombreCategoria(grupo.categoria)}>
                          {grupo.productos.map((p) => (
                            <option key={p.id} value={p.id}>
                              {p.nombre}
                            </option>
                          ))}
                        </optgroup>
                      ))}
                    </select>
                  </div>
                  <CampoTexto
                    etiqueta={`Cantidad del ingrediente ${indice + 1}`}
                    inputMode="decimal"
                    value={ingrediente.cantidad}
                    onChange={(e) => cambiarFila(ingrediente.clave, { cantidad: e.target.value })}
                    sufijo={producto ? UNIDAD[producto.unidadBase] : undefined}
                  />
                  <div className="ingredientes-curados__marcas">
                    <label className="casilla">
                      <input
                        type="checkbox"
                        checked={ingrediente.opcional}
                        onChange={(e) => cambiarFila(ingrediente.clave, { opcional: e.target.checked })}
                      />
                      Opcional
                    </label>
                    <label className="casilla">
                      <input
                        type="checkbox"
                        checked={ingrediente.principal}
                        onChange={(e) => cambiarFila(ingrediente.clave, { principal: e.target.checked })}
                      />
                      Define el plato
                    </label>
                    <Boton
                      variante="texto"
                      aria-label={`Quitar el ingrediente ${indice + 1}`}
                      onClick={() => cambiar('ingredientes', formulario.ingredientes.filter((i) => i.clave !== ingrediente.clave))}
                    >
                      Quitar
                    </Boton>
                  </div>
                </li>
              )
            })}
          </ul>
          {errores.ingredientes && <span className="campo__error">{errores.ingredientes}</span>}
          <Boton variante="secundario" onClick={() => cambiar('ingredientes', [...formulario.ingredientes, fila()])}>
            + Agregar ingrediente
          </Boton>
          {costo > 0 && (
            <p className="curaduria__conteo">
              Costo de referencia: {enPesos(costo)}
              {porciones > 0 ? ` · ${enPesos(costo / porciones)} por porción` : ''}
            </p>
          )}
        </fieldset>

        <div className="campo">
          <label className="campo__etiqueta" htmlFor="pasos-receta">
            Pasos
          </label>
          <span className="campo__ayuda" id="pasos-ayuda">
            Un paso por línea, sin numerar. Cada paso dice como máximo un tiempo («cocina 20 minutos»): el Modo cocinar lo
            ofrece como temporizador.
          </span>
          <textarea
            id="pasos-receta"
            className="campo__control formulario-receta__texto"
            rows={10}
            value={formulario.pasos}
            aria-describedby="pasos-ayuda"
            aria-invalid={errores.pasos ? true : undefined}
            onChange={(e) => cambiar('pasos', e.target.value)}
          />
          {problemas.length > 0 ? (
            <ul className="formulario-receta__problemas">
              {problemas.map((p) => (
                <li key={p.numero} className="campo__error">
                  Paso {p.numero}: {p.problema}
                </li>
              ))}
            </ul>
          ) : (
            errores.pasos && <span className="campo__error">{errores.pasos}</span>
          )}
          {pasos.length > 0 && (
            <span className="campo__ayuda">
              {pasos.length} {pasos.length === 1 ? 'paso' : 'pasos'}
            </span>
          )}
        </div>

        <div className="campo">
          <label className="campo__etiqueta" htmlFor="previa-receta">
            Preparación previa (opcional)
          </label>
          <span className="campo__ayuda">Lo que se hace antes, por ejemplo: «Deja los fríjoles en remojo la noche anterior». Genera un aviso.</span>
          <textarea
            id="previa-receta"
            className="campo__control formulario-receta__texto"
            rows={2}
            maxLength={300}
            value={formulario.preparacionPrevia}
            onChange={(e) => cambiar('preparacionPrevia', e.target.value)}
          />
        </div>

        <div className="formulario-item__acciones">
          <Boton variante="secundario" onClick={() => navegar('/curaduria/recetas')} disabled={guardando}>
            Cancelar
          </Boton>
          <Boton type="submit" cargando={guardando}>
            Guardar receta
          </Boton>
        </div>

        {receta && (
          <>
            <p className="curaduria__conteo">
              {receta.tieneVideo ? 'Tiene video. ' : 'Todavía no tiene video. '}
              <Link to="/curaduria/recetas/videos">Videos y etiquetas</Link>
            </p>
            {confirmando ? (
              <div className="confirmar">
                <p>{receta.activa ? `¿Quitar ${receta.nombre} del recetario?` : `¿Volver a ofrecer ${receta.nombre}?`}</p>
                <span>
                  {receta.activa
                    ? 'Deja de ofrecerse y «Armar mi semana» no la vuelve a proponer; los planes que ya la tienen la conservan.'
                    : 'Aparece otra vez en el recetario y en «Armar mi semana».'}
                </span>
                <div className="formulario-item__acciones">
                  <Boton variante="secundario" onClick={() => setConfirmando(false)}>
                    No
                  </Boton>
                  <Boton cargando={guardando} onClick={() => void cambiarActiva()}>
                    {receta.activa ? 'Sí, quitarla' : 'Sí, volver a ofrecerla'}
                  </Boton>
                </div>
              </div>
            ) : (
              <Boton variante="texto" onClick={() => setConfirmando(true)}>
                {receta.activa ? 'Quitar del recetario' : 'Volver a ofrecerla'}
              </Boton>
            )}
          </>
        )}
      </form>
    </>
  )
}
