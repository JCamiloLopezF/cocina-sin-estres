import { useId, useRef, useState, type FormEvent } from 'react'
import { Alerta, Boton, CampoTexto, GrupoOpciones, type Opcion } from '../../shared/componentes'
import { enfocarPrimerError } from '../auth/enfocarPrimerError'
import { NOMBRE_NIVEL } from './formato'
import { mensajeDespensa } from './mensajesDespensa'
import type { DatosItem, Nivel, TipoCantidad, UnidadBase } from './tipos'

const UNIDAD: Record<UnidadBase, string> = { g: 'gramos', ml: 'mililitros', unidad: 'unidades' }
const MINIMOS: { valor: OpcionMinimo; texto: string }[] = [
  { valor: 'SIN_MINIMO', texto: 'No avisarme' },
  { valor: 'AGOTADO', texto: 'Agotado' },
  { valor: 'POCO', texto: 'Poco' },
  { valor: 'MEDIO', texto: 'Medio' },
]
const SIN_MINIMO = 'SIN_MINIMO'
type OpcionMinimo = Nivel | typeof SIN_MINIMO

interface Props {
  producto: { nombre: string; tipoCantidad: TipoCantidad; unidadBase: UnidadBase }
  inicial?: DatosItem
  /** En edición se puede marcar "Agotado"; al agregar no tiene sentido. */
  permitirAgotado?: boolean
  textoEnviar: string
  alEnviar: (datos: DatosItem) => Promise<void>
  alCancelar: () => void
}

interface Errores {
  cantidad?: string
  nivel?: string
  cantidadMinima?: string
  ubicacion?: string
}

/**
 * Cantidad, mínimo, vencimiento y ubicación de un producto (pantallas 17 y 18).
 *
 * Aplica la regla de cantidad mixta (ADR-11): un producto contable se cuenta y uno a
 * granel se registra por nivel. Lo valida aquí para dar el error al lado del campo; el
 * backend lo vuelve a validar.
 */
export function FormularioItem({ producto, inicial, permitirAgotado = false, textoEnviar, alEnviar, alCancelar }: Props) {
  const esGranel = producto.tipoCantidad === 'GRANEL'
  const formulario = useRef<HTMLFormElement>(null)
  const [cantidad, setCantidad] = useState(inicial?.cantidad?.toString() ?? (esGranel ? '' : '1'))
  const [nivel, setNivel] = useState<Nivel | undefined>(inicial?.nivel)
  const [cantidadMinima, setCantidadMinima] = useState(inicial?.cantidadMinima?.toString() ?? '')
  const [nivelMinimo, setNivelMinimo] = useState<OpcionMinimo>(inicial?.nivelMinimo ?? SIN_MINIMO)
  const [vence, setVence] = useState(inicial?.fechaVencimiento ?? '')
  const [ubicacion, setUbicacion] = useState(inicial?.ubicacion ?? '')
  const [errores, setErrores] = useState<Errores>({})
  const [errorGeneral, setErrorGeneral] = useState<string>()
  const [enviando, setEnviando] = useState(false)

  const niveles: Opcion<Nivel>[] = (['AGOTADO', 'POCO', 'MEDIO', 'LLENO'] as Nivel[])
    .filter((n) => permitirAgotado || n !== 'AGOTADO')
    .map((n) => ({ valor: n, texto: NOMBRE_NIVEL[n] }))
  const id = useId()

  function sumar(paso: number) {
    const actual = Number(cantidad.replace(',', '.')) || 0
    setCantidad(String(Math.max(0, actual + paso)))
  }

  async function enviar(evento: FormEvent) {
    evento.preventDefault()
    setErrorGeneral(undefined)
    const nuevos: Errores = {}
    const datos: DatosItem = {
      fechaVencimiento: vence || undefined,
      ubicacion: ubicacion.trim() || undefined,
    }

    if (esGranel) {
      if (!nivel) {
        nuevos.nivel = 'Elige cuánto te queda.'
      }
      datos.nivel = nivel
      datos.nivelMinimo = nivelMinimo === SIN_MINIMO ? undefined : nivelMinimo
    } else {
      const valor = Number(cantidad.replace(',', '.'))
      if (cantidad.trim() === '' || Number.isNaN(valor) || valor < 0) {
        nuevos.cantidad = 'Escribe cuánto tienes; puede ser 0.'
      }
      datos.cantidad = valor
      if (cantidadMinima.trim() !== '') {
        const minimo = Number(cantidadMinima.replace(',', '.'))
        if (Number.isNaN(minimo) || minimo < 0) {
          nuevos.cantidadMinima = 'El mínimo debe ser 0 o más.'
        }
        datos.cantidadMinima = minimo
      }
    }
    if (ubicacion.trim().length > 40) {
      nuevos.ubicacion = 'Máximo 40 caracteres.'
    }

    setErrores(nuevos)
    if (Object.values(nuevos).some(Boolean)) {
      enfocarPrimerError(formulario.current)
      return
    }

    setEnviando(true)
    try {
      await alEnviar(datos)
    } catch (error) {
      setErrorGeneral(mensajeDespensa(error))
      setEnviando(false)
    }
  }

  // Wireframe 17: en un producto contable la fecha va antes del mínimo.
  const campoFecha = (
    <CampoTexto
      etiqueta="Fecha de vencimiento (opcional)"
      type="date"
      name="vence"
      value={vence}
      onChange={(e) => setVence(e.target.value)}
    />
  )

  return (
    <form ref={formulario} className="formulario-item" onSubmit={enviar} noValidate>
      <p className="rotulo">
        {esGranel ? 'Producto a granel · por nivel' : `Producto contable · ${UNIDAD[producto.unidadBase]}`}
      </p>
      {errorGeneral && <Alerta tono="error">{errorGeneral}</Alerta>}

      {esGranel ? (
        <>
          <GrupoOpciones
            leyenda="¿Cuánto te queda?"
            leyendaOculta
            variante="segmentos"
            opciones={niveles}
            valor={nivel}
            alCambiar={setNivel}
            error={errores.nivel}
          />
          <p className="formulario-item__nota">Para productos a granel usamos niveles en lugar de cantidades exactas.</p>
          <div className="campo">
            <label className="campo__etiqueta" htmlFor={`${id}-minimo`}>
              Avisarme cuando quede en
            </label>
            <select
              id={`${id}-minimo`}
              className="campo__control campo__control--lista"
              value={nivelMinimo}
              onChange={(e) => setNivelMinimo(e.target.value as OpcionMinimo)}
            >
              {MINIMOS.map((m) => (
                <option key={m.valor} value={m.valor}>
                  {m.texto}
                </option>
              ))}
            </select>
          </div>
        </>
      ) : (
        <>
          <div className="campo">
            <div className="contador">
              <Boton variante="secundario" aria-label="Quitar uno" onClick={() => sumar(-1)}>
                −
              </Boton>
              <label className="solo-lectores" htmlFor={`${id}-cantidad`}>
                Cantidad en {UNIDAD[producto.unidadBase]}
              </label>
              <input
                id={`${id}-cantidad`}
                className="contador__valor"
                type="text"
                inputMode="decimal"
                name="cantidad"
                value={cantidad}
                aria-invalid={errores.cantidad ? true : undefined}
                aria-describedby={errores.cantidad ? `${id}-cantidad-error` : undefined}
                onChange={(e) => setCantidad(e.target.value)}
              />
              <Boton aria-label="Agregar uno" onClick={() => sumar(1)}>
                +
              </Boton>
              <span className="contador__unidad" aria-hidden="true">
                {UNIDAD[producto.unidadBase]}
              </span>
            </div>
            {errores.cantidad && (
              <span className="campo__error" id={`${id}-cantidad-error`}>
                {errores.cantidad}
              </span>
            )}
          </div>
          {campoFecha}
          <CampoTexto
            etiqueta="Nivel mínimo (opcional)"
            ayuda={`Te avisamos cuando bajes de aquí (en ${UNIDAD[producto.unidadBase]}).`}
            type="text"
            inputMode="decimal"
            name="cantidadMinima"
            sufijo={UNIDAD[producto.unidadBase]}
            value={cantidadMinima}
            onChange={(e) => setCantidadMinima(e.target.value)}
            error={errores.cantidadMinima}
          />
        </>
      )}

      {esGranel && campoFecha}
      <CampoTexto
        etiqueta="Ubicación (opcional)"
        ayuda="Por ejemplo: Alacena, Nevera, Congelador."
        name="ubicacion"
        list="ubicaciones-despensa"
        maxLength={40}
        value={ubicacion}
        onChange={(e) => setUbicacion(e.target.value)}
        error={errores.ubicacion}
      />
      <datalist id="ubicaciones-despensa">
        <option value="Alacena" />
        <option value="Nevera" />
        <option value="Congelador" />
      </datalist>

      <div className="formulario-item__acciones">
        <Boton variante="secundario" onClick={alCancelar}>
          Cancelar
        </Boton>
        <Boton type="submit" cargando={enviando}>
          {textoEnviar}
        </Boton>
      </div>
    </form>
  )
}
