import { useEffect, useRef, useState, type FormEvent, type ReactNode } from 'react'
import { useNavigate } from 'react-router'
import { ErrorApi, pedir } from '../../api/cliente'
import { mensajeDeError } from '../../api/mensajesError'
import { Alerta, Boton, Cargando, CampoTexto, GrupoOpciones } from '../../shared/componentes'
import { useSesion } from '../auth/contextoSesion'
import { enfocarPrimerError } from '../auth/enfocarPrimerError'
import './perfil.css'

type Periodicidad = 'SEMANAL' | 'QUINCENAL' | 'MENSUAL'

/** Cuerpo de `PUT /api/perfil/hogar` y respuesta de `GET` (PerfilHogarRequest/Response). */
interface PerfilHogar {
  presupuestoMonto: number
  periodicidadIngreso: Periodicidad
  diaPago1: number
  diaPago2?: number | null
  diasSinTiempo: number[]
  restricciones: string[]
  sinRestricciones: boolean
  otrasCondiciones?: string | null
  consentimientoDatos?: boolean
  consentimientoDatosEn?: string | null
  personas: { nombre: string | null; edad: number | null }[]
}

const DIAS = ['Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado', 'Domingo']

/**
 * Restricciones del wireframe 08. El código es el que se guarda (cifrado, ADR-20); el
 * catálogo completo de restricciones llega con su propia tarea (RNF-03).
 */
const ALERGIAS = [
  { codigo: 'lactosa', texto: 'Lactosa' },
  { codigo: 'gluten', texto: 'Gluten' },
  { codigo: 'mani', texto: 'Maní' },
  { codigo: 'huevo', texto: 'Huevo' },
  { codigo: 'mariscos', texto: 'Mariscos' },
]
const CONDICIONES = [
  { codigo: 'hipertension', texto: 'Hipertensión' },
  { codigo: 'diabetes', texto: 'Diabetes' },
  { codigo: 'colesterol', texto: 'Colesterol alto' },
]

const PERIODO: Record<Periodicidad, string> = { SEMANAL: 'semanal', QUINCENAL: 'quincenal', MENSUAL: 'mensual' }

const PASOS = ['Personas y edades', 'Alergias y condiciones de salud', 'Presupuesto y ciclo de ingreso', 'Días sin tiempo para cocinar']

interface Persona {
  nombre: string
  edad: string
}

interface Errores {
  /** Un mensaje por persona, en el mismo orden. */
  edades?: (string | undefined)[]
  presupuesto?: string
  diaPago1?: string
  diaPago2?: string
  restricciones?: string
  consentimiento?: string
}

/**
 * Perfil del hogar en cuatro pasos (wireframes 07 a 10): quiénes comen en casa,
 * alergias y condiciones, presupuesto con su ciclo de ingreso y días sin tiempo.
 *
 * Cada paso se valida al continuar, con lo mismo que valida HogarService (RNF-01), y
 * el perfil se guarda completo al final. Las restricciones son datos de salud: solo se
 * envían con la autorización marcada (Ley 1581, RNF-22).
 */
export function PantallaPerfilHogar() {
  const navegar = useNavigate()
  const { recargarCuenta } = useSesion()
  const formulario = useRef<HTMLFormElement>(null)
  const titulo = useRef<HTMLHeadingElement>(null)
  const [paso, setPaso] = useState(0)
  const [cargando, setCargando] = useState(true)
  const [personas, setPersonas] = useState<Persona[]>([{ nombre: '', edad: '' }])
  const [presupuesto, setPresupuesto] = useState('')
  const [periodicidad, setPeriodicidad] = useState<Periodicidad>('MENSUAL')
  const [diaPago1, setDiaPago1] = useState('')
  const [diaPago2, setDiaPago2] = useState('')
  const [diasSinTiempo, setDiasSinTiempo] = useState<number[]>([])
  const [sinRestricciones, setSinRestricciones] = useState(false)
  const [restricciones, setRestricciones] = useState<string[]>([])
  const [otras, setOtras] = useState('')
  const [verOtras, setVerOtras] = useState(false)
  const [consentimiento, setConsentimiento] = useState(false)
  const [errores, setErrores] = useState<Errores>({})
  const [errorGeneral, setErrorGeneral] = useState<string>()
  const [enviando, setEnviando] = useState(false)

  useEffect(() => {
    const control = new AbortController()
    pedir<PerfilHogar>('/perfil/hogar', { senal: control.signal })
      .then((perfil) => {
        setPersonas(
          perfil.personas.length
            ? perfil.personas.map((p) => ({ nombre: p.nombre ?? '', edad: p.edad?.toString() ?? '' }))
            : [{ nombre: '', edad: '' }],
        )
        setPresupuesto(String(perfil.presupuestoMonto))
        setPeriodicidad(perfil.periodicidadIngreso)
        setDiaPago1(String(perfil.diaPago1))
        setDiaPago2(perfil.diaPago2 ? String(perfil.diaPago2) : '')
        setDiasSinTiempo(perfil.diasSinTiempo ?? [])
        setSinRestricciones(perfil.sinRestricciones)
        setRestricciones(perfil.restricciones ?? [])
        setOtras(perfil.otrasCondiciones ?? '')
        setVerOtras(Boolean(perfil.otrasCondiciones))
        setConsentimiento(Boolean(perfil.consentimientoDatosEn))
      })
      .catch((error: unknown) => {
        // 404: todavía no hay perfil, el asistente arranca vacío.
        if (!control.signal.aborted && !(error instanceof ErrorApi && error.status === 404)) {
          setErrorGeneral(mensajeDeError(error))
        }
      })
      .finally(() => {
        if (!control.signal.aborted) {
          setCargando(false)
        }
      })
    return () => control.abort()
  }, [])

  const rangoDia = periodicidad === 'SEMANAL' ? 'del 1 (lunes) al 7 (domingo)' : 'del 1 al 31'
  const edades = personas.map((p) => Number(p.edad))
  const monto = Number(presupuesto.replace(/[.\s$]/g, '').replace(',', '.'))
  const dia1 = Number(diaPago1)
  const dia2 = Number(diaPago2)

  function cambiarPersona(indice: number, cambio: Partial<Persona>) {
    setPersonas((actuales) => actuales.map((p, i) => (i === indice ? { ...p, ...cambio } : p)))
  }

  function alternar<T>(lista: T[], valor: T): T[] {
    return lista.includes(valor) ? lista.filter((v) => v !== valor) : [...lista, valor]
  }

  /** Errores del paso indicado; vacío si se puede seguir. */
  function validarPaso(indice: number): Errores {
    const nuevos: Errores = {}
    if (indice === 0) {
      const erroresEdad = personas.map((p, i) =>
        p.edad.trim() === '' || !Number.isInteger(edades[i]) || edades[i] < 0 || edades[i] > 120
          ? 'Edad entre 0 y 120.'
          : undefined,
      )
      if (erroresEdad.some(Boolean)) {
        nuevos.edades = erroresEdad
      }
    }
    if (indice === 1) {
      const declaroAlgo = restricciones.length > 0 || otras.trim() !== ''
      if (!sinRestricciones && !declaroAlgo) {
        nuevos.restricciones = 'Marca las restricciones del hogar o indica que nadie tiene.'
      }
      if (declaroAlgo && !sinRestricciones && !consentimiento) {
        nuevos.consentimiento = 'Para guardar datos de salud necesitamos tu autorización.'
      }
    }
    if (indice === 2) {
      if (!presupuesto.trim() || Number.isNaN(monto) || monto <= 0) {
        nuevos.presupuesto = 'Escribe un presupuesto mayor que cero.'
      }
      const maximo = periodicidad === 'SEMANAL' ? 7 : 31
      if (!Number.isInteger(dia1) || dia1 < 1 || dia1 > maximo) {
        nuevos.diaPago1 = `Escribe un día ${rangoDia}.`
      }
      if (periodicidad === 'QUINCENAL' && (!Number.isInteger(dia2) || dia2 < 1 || dia2 > 31 || dia2 <= dia1)) {
        nuevos.diaPago2 = 'Escribe el segundo día de pago, mayor que el primero.'
      }
    }
    return nuevos
  }

  function irA(indice: number) {
    setPaso(indice)
    setErrores({})
    window.scrollTo?.(0, 0)
    requestAnimationFrame(() => titulo.current?.focus())
  }

  function atras() {
    if (paso === 0) {
      navegar('/perfil')
    } else {
      irA(paso - 1)
    }
  }

  async function continuar(evento: FormEvent) {
    evento.preventDefault()
    setErrorGeneral(undefined)
    const nuevos = validarPaso(paso)
    setErrores(nuevos)
    if (Object.values(nuevos).some(Boolean)) {
      enfocarPrimerError(formulario.current)
      return
    }
    if (paso < PASOS.length - 1) {
      irA(paso + 1)
      return
    }
    await guardar()
  }

  async function guardar() {
    const cuerpo: PerfilHogar = {
      presupuestoMonto: monto,
      periodicidadIngreso: periodicidad,
      diaPago1: dia1,
      diaPago2: periodicidad === 'QUINCENAL' ? dia2 : null,
      diasSinTiempo: [...diasSinTiempo].sort(),
      restricciones: sinRestricciones ? [] : restricciones,
      sinRestricciones,
      otrasCondiciones: sinRestricciones ? null : otras.trim() || null,
      consentimientoDatos: !sinRestricciones && consentimiento,
      personas: personas.map((p, i) => ({ nombre: p.nombre.trim() || null, edad: edades[i] })),
    }

    setEnviando(true)
    try {
      await pedir('/perfil/hogar', { metodo: 'PUT', cuerpo })
      await recargarCuenta()
      navegar('/', { replace: true })
    } catch (error) {
      // Este código trae en el detalle qué dato falló, ya redactado para la usuaria (RNF-01).
      setErrorGeneral(
        error instanceof ErrorApi && error.codigo === 'PERFIL_HOGAR_DATOS_INVALIDOS' && error.message
          ? error.message
          : mensajeDeError(error),
      )
      setEnviando(false)
    }
  }

  if (cargando) {
    return <Cargando texto="Cargando tu hogar…" />
  }

  return (
    <form ref={formulario} className="asistente" onSubmit={continuar} noValidate>
      <header className="asistente__encabezado">
        <h1 className="encabezado__titulo" ref={titulo} tabIndex={-1}>
          Tu hogar · paso {paso + 1} de {PASOS.length}
        </h1>
        <ol className="asistente__progreso" aria-label="Pasos">
          {PASOS.map((nombre, i) => (
            <li
              key={nombre}
              className={i <= paso ? 'asistente__marca asistente__marca--hecha' : 'asistente__marca'}
              aria-current={i === paso ? 'step' : undefined}
            >
              <span className="solo-lectores">{nombre}</span>
            </li>
          ))}
        </ol>
      </header>

      {errorGeneral && <Alerta tono="error">{errorGeneral}</Alerta>}

      {paso === 0 && (
        <section className="asistente__paso" aria-label={PASOS[0]}>
          <p>¿Quiénes comen en casa? Con esto ajustamos las porciones de cada receta.</p>
          {personas.map((persona, indice) => (
            <fieldset className="tarjeta-persona" key={indice}>
              <legend className="tarjeta-persona__titulo">{indice === 0 ? 'Tú' : `Persona ${indice + 1}`}</legend>
              {indice > 0 && (
                <Boton
                  variante="texto"
                  className="tarjeta-persona__quitar"
                  onClick={() => setPersonas((p) => p.filter((_, i) => i !== indice))}
                >
                  Quitar<span className="solo-lectores"> persona {indice + 1}</span>
                </Boton>
              )}
              <CampoTexto
                etiqueta="Nombre (opcional)"
                value={persona.nombre}
                maxLength={60}
                onChange={(e) => cambiarPersona(indice, { nombre: e.target.value })}
              />
              <CampoTexto
                etiqueta="Edad"
                inputMode="numeric"
                sufijo="años"
                value={persona.edad}
                error={errores.edades?.[indice]}
                onChange={(e) => cambiarPersona(indice, { edad: e.target.value.replace(/\D/g, '') })}
              />
            </fieldset>
          ))}
          <button
            type="button"
            className="agregar-punteado"
            onClick={() => setPersonas((p) => [...p, { nombre: '', edad: '' }])}
          >
            + Agregar persona
          </button>
          <p className="asistente__total">
            <span>Personas en el hogar</span>
            <strong>{personas.length}</strong>
          </p>
        </section>
      )}

      {paso === 1 && (
        <section className="asistente__paso" aria-label={PASOS[1]}>
          <p>Con esto filtramos el recetario. Son datos sensibles y solo se usan dentro de la app.</p>
          <label className="casilla casilla--tarjeta">
            Nadie en casa tiene alergias ni condiciones de salud
            <input
              type="checkbox"
              checked={sinRestricciones}
              aria-invalid={errores.restricciones ? true : undefined}
              aria-describedby={errores.restricciones ? 'error-restricciones' : undefined}
              onChange={(e) => setSinRestricciones(e.target.checked)}
            />
          </label>
          {errores.restricciones && (
            <span className="campo__error" id="error-restricciones">
              {errores.restricciones}
            </span>
          )}
          {!sinRestricciones && (
            <>
              <Chips
                leyenda="Alergias e intolerancias"
                opciones={ALERGIAS}
                marcadas={restricciones}
                alternar={(codigo) => setRestricciones((r) => alternar(r, codigo))}
                extra={
                  !verOtras && (
                    <li>
                      <button type="button" className="chip-extra" onClick={() => setVerOtras(true)}>
                        + otra
                      </button>
                    </li>
                  )
                }
              />
              <Chips
                leyenda="Condiciones de salud"
                opciones={CONDICIONES}
                marcadas={restricciones}
                alternar={(codigo) => setRestricciones((r) => alternar(r, codigo))}
              />
              {verOtras && (
                <CampoTexto
                  etiqueta="Otra alergia o condición"
                  value={otras}
                  maxLength={200}
                  autoFocus
                  onChange={(e) => setOtras(e.target.value)}
                />
              )}
              <label className="casilla casilla--aviso">
                <input
                  type="checkbox"
                  checked={consentimiento}
                  aria-invalid={errores.consentimiento ? true : undefined}
                  aria-describedby={errores.consentimiento ? 'error-consentimiento' : undefined}
                  onChange={(e) => setConsentimiento(e.target.checked)}
                />
                Autorizo guardar estos datos de salud, cifrados y solo para filtrar recetas (Ley 1581 de 2012).
              </label>
              {errores.consentimiento && (
                <span className="campo__error" id="error-consentimiento">
                  {errores.consentimiento}
                </span>
              )}
            </>
          )}
        </section>
      )}

      {paso === 2 && (
        <section className="asistente__paso" aria-label={PASOS[2]}>
          <CampoTexto
            etiqueta={`Presupuesto ${PERIODO[periodicidad]} de alimentación`}
            ayuda="En pesos, por ejemplo 780000."
            inputMode="numeric"
            sufijo="COP"
            value={presupuesto}
            onChange={(e) => setPresupuesto(e.target.value)}
            error={errores.presupuesto}
          />
          <GrupoOpciones<Periodicidad>
            leyenda="Ciclo de ingreso"
            variante="segmentos"
            opciones={[
              { valor: 'SEMANAL', texto: 'Semanal' },
              { valor: 'QUINCENAL', texto: 'Quincenal' },
              { valor: 'MENSUAL', texto: 'Mensual' },
            ]}
            valor={periodicidad}
            alCambiar={setPeriodicidad}
          />
          <CampoTexto
            etiqueta={periodicidad === 'QUINCENAL' ? 'Primer día de ingreso' : 'Día de ingreso'}
            ayuda={`Un número ${rangoDia}.`}
            inputMode="numeric"
            value={diaPago1}
            onChange={(e) => setDiaPago1(e.target.value.replace(/\D/g, ''))}
            error={errores.diaPago1}
          />
          {periodicidad === 'QUINCENAL' && (
            <CampoTexto
              etiqueta="Segundo día de ingreso"
              inputMode="numeric"
              value={diaPago2}
              onChange={(e) => setDiaPago2(e.target.value.replace(/\D/g, ''))}
              error={errores.diaPago2}
            />
          )}
          <div className="asistente__nota">
            <strong>Así lo usamos</strong>
            <p>
              Las comidas más costosas se ubican en los días siguientes a tu ingreso, y te avisamos si el plan supera el
              presupuesto.
            </p>
          </div>
        </section>
      )}

      {paso === 3 && (
        <section className="asistente__paso" aria-label={PASOS[3]}>
          <fieldset className="asistente__dias">
            <legend>
              Marca los días en los que llegas sin tiempo. Ahí proponemos recetas de menos de 20 minutos.
            </legend>
            {DIAS.map((dia, i) => (
              <label key={dia} className="casilla casilla--tarjeta">
                {dia}
                <input
                  type="checkbox"
                  checked={diasSinTiempo.includes(i + 1)}
                  onChange={() => setDiasSinTiempo((d) => alternar(d, i + 1))}
                />
              </label>
            ))}
          </fieldset>
        </section>
      )}

      <div className="asistente__acciones">
        <Boton variante="secundario" onClick={atras}>
          Atrás
        </Boton>
        <Boton type="submit" cargando={enviando}>
          {paso === PASOS.length - 1 ? 'Guardar perfil' : 'Continuar'}
        </Boton>
      </div>
    </form>
  )
}

interface ChipsProps {
  leyenda: string
  opciones: { codigo: string; texto: string }[]
  marcadas: string[]
  alternar: (codigo: string) => void
  extra?: ReactNode
}

/** Selección múltiple en píldoras (wireframe 08): casillas de verdad con forma de píldora. */
function Chips({ leyenda, opciones, marcadas, alternar, extra }: ChipsProps) {
  return (
    <fieldset className="opciones">
      <legend className="campo__etiqueta">{leyenda}</legend>
      <ul className="opciones__lista">
        {opciones.map((o) => (
          <li key={o.codigo}>
            <label className="opciones__opcion">
              <input type="checkbox" checked={marcadas.includes(o.codigo)} onChange={() => alternar(o.codigo)} />
              <span>{o.texto}</span>
            </label>
          </li>
        ))}
        {extra}
      </ul>
    </fieldset>
  )
}
