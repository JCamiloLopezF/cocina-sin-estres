import { useEffect, useState } from 'react'
import { useNavigate, useParams, useSearchParams } from 'react-router'
import { Alerta, Boton, Cargando, EncabezadoPagina, GrupoOpciones } from '../../shared/componentes'
import { asignarComida, crearPlan, obtenerPlan } from '../planificador/apiPlanificador'
import { mensajePlan } from '../planificador/mensajesPlan'
import {
  COMIDAS,
  conSemana,
  DIAS,
  esComida,
  esDia,
  esProxima,
  INICIAL_DIA,
  lunesDeSemana,
  NOMBRE_COMIDA,
  NOMBRE_DIA,
} from '../planificador/semana'
import type { Dia, PlanSemanal, TipoComida } from '../planificador/tipos'
import { listarRecetas } from './apiRecetario'
import type { Receta } from './tipos'
import './recetario.css'

type Estado =
  | { tipo: 'cargando' }
  | { tipo: 'listo'; receta: Receta; plan: PlanSemanal | null; nombres: Map<number, string> }
  | { tipo: 'error'; error: unknown }

/** Pantalla 24: elegir el día y la comida de esta semana para la receta. */
export function PantallaAgregarAlPlan() {
  const { id } = useParams()
  const navegar = useNavigate()
  const [parametros] = useSearchParams()
  const proxima = esProxima(parametros)
  const [lunes] = useState(() => lunesDeSemana(proxima))
  const [estado, setEstado] = useState<Estado>({ tipo: 'cargando' })
  const [version, setVersion] = useState(0)
  const [dia, setDia] = useState<Dia | undefined>(() => {
    const valor = Number(parametros.get('dia'))
    return esDia(valor) ? valor : undefined
  })
  const [comida, setComida] = useState<TipoComida | undefined>(() => {
    const valor = parametros.get('comida')
    return esComida(valor) ? valor : undefined
  })
  const [intento, setIntento] = useState(false)
  const [guardando, setGuardando] = useState(false)
  const [errorGuardar, setErrorGuardar] = useState<unknown>()

  useEffect(() => {
    const control = new AbortController()
    Promise.all([listarRecetas(control.signal), obtenerPlan(lunes, control.signal)])
      .then(([recetas, plan]) => {
        const receta = recetas.find((r) => r.id === Number(id))
        if (!receta) {
          setEstado({ tipo: 'error', error: new Error('receta') })
          return
        }
        setEstado({ tipo: 'listo', receta, plan, nombres: new Map(recetas.map((r) => [r.id, r.nombre])) })
      })
      .catch((error: unknown) => {
        if (!control.signal.aborted) {
          setEstado({ tipo: 'error', error })
        }
      })
    return () => control.abort()
  }, [id, lunes, version])

  const volver = { ruta: `/recetas/${id}${parametros.toString() ? `?${parametros.toString()}` : ''}`, etiqueta: 'Volver a la receta' }

  if (estado.tipo !== 'listo') {
    return (
      <>
        <EncabezadoPagina titulo="¿Cuándo la preparas?" volver={volver} />
        {estado.tipo === 'cargando' ? (
          <Cargando texto="Cargando tu semana…" />
        ) : (
          <Alerta tono="error">
            {mensajePlan(estado.error)}{' '}
            <Boton variante="texto" onClick={() => setVersion((v) => v + 1)}>
              Reintentar
            </Boton>
          </Alerta>
        )}
      </>
    )
  }

  const { receta, plan, nombres } = estado
  const delDia = dia ? (plan?.comidas ?? []).filter((c) => c.dia === dia) : []
  const ocupada = comida ? delDia.find((c) => c.tipoComida === comida) : undefined

  async function continuar() {
    setIntento(true)
    if (!dia || !comida) {
      return
    }
    setGuardando(true)
    setErrorGuardar(undefined)
    try {
      const planActual = plan ?? (await crearPlan(lunes))
      await asignarComida(planActual.id, dia, comida, receta.id)
      navegar(conSemana(`/plan/dia/${dia}`, proxima), {
        state: { aviso: `${receta.nombre} quedó para el ${NOMBRE_DIA[dia].toLowerCase()} · ${NOMBRE_COMIDA[comida].toLowerCase()}.` },
      })
    } catch (error) {
      setErrorGuardar(error)
      setGuardando(false)
    }
  }

  return (
    <>
      <EncabezadoPagina titulo="¿Cuándo la preparas?" detalle={proxima ? `${receta.nombre} · próxima semana` : receta.nombre} detalleAbajo volver={volver} />
      <div className="agregar-plan">
        <fieldset className="opciones opciones--segmentos opciones--dias" aria-invalid={intento && !dia ? true : undefined}>
          <legend className="campo__etiqueta">Día</legend>
          <div className="opciones__lista">
            {DIAS.map((d) => (
              <label key={d} className="opciones__opcion">
                <input type="radio" name="dia" value={d} checked={dia === d} onChange={() => setDia(d)} />
                <span aria-hidden="true">{INICIAL_DIA[d]}</span>
                <span className="solo-lectores">{NOMBRE_DIA[d]}</span>
              </label>
            ))}
          </div>
          {intento && !dia && <span className="campo__error">Elige el día.</span>}
        </fieldset>

        <GrupoOpciones<TipoComida>
          leyenda="Comida"
          variante="segmentos"
          opciones={COMIDAS.map((c) => ({ valor: c, texto: NOMBRE_COMIDA[c] }))}
          valor={comida}
          alCambiar={setComida}
          error={intento && !comida ? 'Elige la comida.' : undefined}
        />

        {dia && (
          <p className="formulario-item__nota">
            {resumenDia(dia, delDia.map((c) => c.tipoComida), ocupada ? nombres.get(ocupada.recetaId) : undefined, comida)}
          </p>
        )}

        {errorGuardar !== undefined && <Alerta tono="error">{mensajePlan(errorGuardar)}</Alerta>}

        <Boton ancho cargando={guardando} onClick={() => void continuar()}>
          {ocupada ? 'Cambiar la receta' : 'Continuar'}
        </Boton>
      </div>
    </>
  )
}

/** "El martes ya tienes almuerzo asignado. La cena está libre." */
function resumenDia(dia: Dia, ocupadas: TipoComida[], nombreOcupada: string | undefined, comida?: TipoComida): string {
  const nombreDia = NOMBRE_DIA[dia].toLowerCase()
  if (comida && nombreOcupada) {
    return `El ${nombreDia} ya tienes ${NOMBRE_COMIDA[comida].toLowerCase()}: ${nombreOcupada}. Si continúas, la cambiamos por esta receta.`
  }
  if (ocupadas.length === 0) {
    return `El ${nombreDia} está libre.`
  }
  const libres = COMIDAS.filter((c) => !ocupadas.includes(c)).map((c) => NOMBRE_COMIDA[c].toLowerCase())
  const tienes = ocupadas.map((c) => NOMBRE_COMIDA[c].toLowerCase()).join(' y ')
  return libres.length
    ? `El ${nombreDia} ya tienes ${tienes}. Libre: ${libres.join(' y ')}.`
    : `El ${nombreDia} ya tienes todas las comidas asignadas.`
}
