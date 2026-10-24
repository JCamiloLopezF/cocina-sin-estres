import { useState } from 'react'
import { Link } from 'react-router'
import { Alerta, Boton, Cargando, EncabezadoPagina, EstadoVacio } from '../../shared/componentes'
import { enPesos } from '../despensa/formato'
import { ajustarPresupuesto, asignarComida, crearPlan, generarPlan, obtenerPlan } from './apiPlanificador'
import { mensajePlan, planSinHogar } from './mensajesPlan'
import { aIso, COMIDAS, DIAS, diaDe, fechaDelDia, NOMBRE_COMIDA, rangoSemana, tituloDia } from './semana'
import type { Dia, PlanGenerado } from './tipos'
import { useSemana, type Semana } from './useSemana'
import './planificador.css'

const NOMBRE_PERIODICIDAD = { SEMANAL: 'semanal', QUINCENAL: 'quincenal', MENSUAL: 'mensual' } as const

/** Comidas que llena "Armar mi semana" (el desayuno es opcional). */
const COMIDAS_A_LLENAR = ['ALMUERZO', 'CENA'] as const

type Accion = 'nueva' | 'repetir' | 'vacia' | 'completar' | 'ajustar'

/** "Pollo" · "Pollo y leche" · "Pollo, leche y queso". */
function enLista(nombres: string[]): string {
  const minusculas = nombres.map((n) => n.toLowerCase())
  return minusculas.length <= 1 ? (minusculas[0] ?? '') : `${minusculas.slice(0, -1).join(', ')} y ${minusculas[minusculas.length - 1]}`
}

/** Lo que se le cuenta a la usuaria después de que el sistema armó o ajustó la semana. */
function contarResultado(resultado: PlanGenerado, accion: 'armar' | 'ajustar'): string {
  const n = resultado.cambios.length
  const costo = `${enPesos(resultado.costoEstimado)} de ${enPesos(resultado.presupuestoSemanal)}`
  if (accion === 'ajustar') {
    if (n === 0) {
      return 'No encontramos recetas más baratas que encajen. Prueba dejando un día libre.'
    }
    const cambio = n === 1 ? 'Cambiamos 1 comida por una opción más económica' : `Cambiamos ${n} comidas por opciones más económicas`
    return `${cambio}: el plan quedó en ${costo}.${resultado.excedePresupuesto ? ' Todavía pasa tu presupuesto.' : ''}`
  }
  if (n === 0) {
    return 'No encontramos recetas que encajen en los días vacíos. Puedes elegir una en el recetario.'
  }
  const partes = [n === 1 ? 'Te propusimos 1 comida.' : `Te propusimos ${n} comidas.`]
  if (resultado.aprovecha.length) {
    partes.push(`Usamos primero lo que está por vencer: ${enLista(resultado.aprovecha)}.`)
  }
  if (resultado.conAdaptaciones) {
    partes.push(
      resultado.conAdaptaciones === 1
        ? '1 comida trae un cambio para quien tiene alergias: míralo en ese día.'
        : `${resultado.conAdaptaciones} comidas traen un cambio para quien tiene alergias: míralo en cada día.`,
    )
  }
  if (resultado.paraNinos) {
    partes.push(
      resultado.paraNinos === 1
        ? 'Como en casa hay niños, incluimos 1 receta de las que suelen gustarles.'
        : `Como en casa hay niños, incluimos ${resultado.paraNinos} recetas de las que suelen gustarles.`,
    )
  }
  partes.push(
    resultado.excedePresupuesto
      ? 'Aun así el plan pasa tu presupuesto: prueba "Ajustar al presupuesto".'
      : `El plan quedó en ${costo}.`,
  )
  partes.push('Cambia lo que quieras desde cada día.')
  return partes.join(' ')
}

/** Pantallas 27, 28 y 30: la semana sin armar, el plan semanal y el aviso de presupuesto. */
export function PantallaPlan() {
  const { estado, recargar } = useSemana()
  const [armando, setArmando] = useState<Accion>()
  const [errorArmar, setErrorArmar] = useState<unknown>()
  const [resultado, setResultado] = useState<string>()
  // La semana que se muestra siempre es la actual.
  const [hoy] = useState(() => diaDe(new Date()))

  if (estado.tipo === 'cargando') {
    return (
      <>
        <EncabezadoPagina titulo="Mi semana" />
        <Cargando texto="Cargando tu semana…" />
      </>
    )
  }
  if (estado.tipo === 'error') {
    return (
      <>
        <EncabezadoPagina titulo="Mi semana" />
        {planSinHogar(estado.error) ? <PrimeroElHogar /> : <ErrorCarga error={estado.error} reintentar={recargar} />}
      </>
    )
  }

  const { semana } = estado
  const { plan } = semana

  async function armar(modo: 'nueva' | 'repetir' | 'vacia') {
    setArmando(modo)
    setErrorArmar(undefined)
    setResultado(undefined)
    try {
      const anterior = modo === 'repetir' ? await obtenerPlan(aIso(fechaDelDia(semana.lunes, -6))) : null
      if (modo === 'repetir' && !anterior?.comidas.length) {
        setErrorArmar('La semana pasada no tenías comidas planeadas. Pídenos que armemos esta semana.')
        return
      }
      const nuevo = await crearPlan(semana.lunes)
      for (const comida of anterior?.comidas ?? []) {
        await asignarComida(nuevo.id, comida.dia, comida.tipoComida, comida.recetaId)
      }
      if (modo === 'nueva') {
        setResultado(contarResultado(await generarPlan(nuevo.id), 'armar'))
      }
      recargar()
    } catch (error) {
      setErrorArmar(error)
    } finally {
      setArmando(undefined)
    }
  }

  /** Completar los días vacíos o ajustar al presupuesto, sobre el plan que ya existe. */
  async function automatizar(accion: 'completar' | 'ajustar') {
    if (!plan) return
    setArmando(accion)
    setErrorArmar(undefined)
    setResultado(undefined)
    try {
      const respuesta = accion === 'completar' ? await generarPlan(plan.id) : await ajustarPresupuesto(plan.id)
      setResultado(contarResultado(respuesta, accion === 'completar' ? 'armar' : 'ajustar'))
      recargar()
    } catch (error) {
      setErrorArmar(error)
    } finally {
      setArmando(undefined)
    }
  }

  const avisoError = errorArmar !== undefined && (
    <Alerta tono={typeof errorArmar === 'string' ? 'info' : 'error'}>
      {typeof errorArmar === 'string' ? errorArmar : mensajePlan(errorArmar)}
    </Alerta>
  )

  if (!plan) {
    return (
      <>
        <EncabezadoPagina titulo="Mi semana" detalle={rangoSemana(semana.lunes)} />
        <EstadoVacio
          titulo="Todavía no armas esta semana"
          texto="Te proponemos almuerzo y cena para los siete días según lo que hay en tu despensa, tu presupuesto, las alergias de cada persona y los días en que no tienes tiempo. Después cambias lo que quieras."
          acciones={
            <>
              <Boton ancho cargando={armando === 'nueva'} disabled={armando !== undefined} onClick={() => void armar('nueva')}>
                Armar mi semana
              </Boton>
              <Boton
                ancho
                variante="secundario"
                cargando={armando === 'repetir'}
                disabled={armando !== undefined}
                onClick={() => void armar('repetir')}
              >
                Repetir la semana pasada
              </Boton>
              <Boton variante="texto" cargando={armando === 'vacia'} disabled={armando !== undefined} onClick={() => void armar('vacia')}>
                Prefiero armarla yo
              </Boton>
            </>
          }
        />
        {avisoError}
      </>
    )
  }

  const faltan = DIAS.reduce(
    (total, dia) => total + COMIDAS_A_LLENAR.filter((tipo) => !plan.comidas.some((c) => c.dia === dia && c.tipoComida === tipo)).length,
    0,
  )

  return (
    <>
      <EncabezadoPagina titulo="Mi semana" detalle={rangoSemana(semana.lunes)} />
      {resultado && (
        <div className="plan__resultado">
          <Alerta tono="exito">{resultado}</Alerta>
        </div>
      )}
      {avisoError}
      <ResumenCosto semana={semana} ajustando={armando === 'ajustar'} alAjustar={() => void automatizar('ajustar')} />

      {faltan > 0 && (
        <section className="completar-plan" aria-labelledby="titulo-completar">
          <h2 id="titulo-completar">
            {faltan === 1 ? 'Falta 1 comida en tu semana' : `Faltan ${faltan} comidas en tu semana`}
          </h2>
          <p>Te proponemos recetas para el almuerzo y la cena que están vacíos. Lo que ya elegiste no cambia.</p>
          <Boton ancho variante="secundario" cargando={armando === 'completar'} disabled={armando !== undefined} onClick={() => void automatizar('completar')}>
            Completar los días vacíos
          </Boton>
        </section>
      )}

      <ul className="semana">
        {DIAS.map((dia) => (
          <li key={dia}>
            <TarjetaDia semana={semana} dia={dia} hoy={hoy === dia} />
          </li>
        ))}
      </ul>

      <Link className="boton boton--primario boton--ancho plan__lista" to="/plan/lista">
        Ver lista de compras
      </Link>
    </>
  )
}

/** Costo del plan contra el presupuesto semanal (ADR-14) y aviso si lo supera (pantalla 30). */
function ResumenCosto({ semana, ajustando, alAjustar }: { semana: Semana; ajustando: boolean; alAjustar: () => void }) {
  const { costo } = semana
  if (!costo) {
    return null
  }
  const total = costo.costoAcumuladoPorDia[costo.costoAcumuladoPorDia.length - 1] ?? 0
  const presupuesto = costo.presupuestoSemanal
  const porcentaje = presupuesto > 0 ? Math.min(100, (total / presupuesto) * 100) : 0
  return (
    <>
      <section className="costo-plan" aria-label="Costo del plan">
        <p className="costo-plan__fila">
          <span>Costo del plan</span>
          <strong>
            {enPesos(total)} / {enPesos(presupuesto)}
          </strong>
        </p>
        <div
          className={costo.excedePresupuesto ? 'costo-plan__barra costo-plan__barra--excede' : 'costo-plan__barra'}
          role="meter"
          aria-label="Parte del presupuesto semanal usada"
          aria-valuemin={0}
          aria-valuemax={100}
          aria-valuenow={Math.round(porcentaje)}
        >
          <span style={{ width: `${porcentaje}%` }} />
        </div>
        {semana.periodicidad && (
          <p className="costo-plan__nota">
            Presupuesto semanal calculado con tu ingreso {NOMBRE_PERIODICIDAD[semana.periodicidad]}.
          </p>
        )}
      </section>
      {costo.excedePresupuesto && (
        <section className="aviso-presupuesto" role="alert" aria-labelledby="titulo-presupuesto">
          <span className="aviso-presupuesto__icono" aria-hidden="true">
            $
          </span>
          <h2 id="titulo-presupuesto">Este plan supera tu presupuesto</h2>
          <p>
            El plan de la semana va en {enPesos(total)} y tu presupuesto semanal es de {enPesos(presupuesto)}. Son{' '}
            {enPesos(total - presupuesto)} de más.
          </p>
          <p className="aviso-presupuesto__consejo">
            <strong>Qué puedes hacer:</strong> deja que cambiemos las recetas más costosas por otras más económicas, o
            elígelas tú.
          </p>
          <Boton ancho cargando={ajustando} onClick={alAjustar}>
            Ajustar al presupuesto
          </Boton>
          <Link className="boton boton--secundario boton--ancho" to="/recetas?atajo=ECONOMICAS">
            Ver alternativas económicas
          </Link>
        </section>
      )}
    </>
  )
}

function TarjetaDia({ semana, dia, hoy }: { semana: Semana; dia: Dia; hoy: boolean }) {
  const comidas = (semana.plan?.comidas ?? [])
    .filter((c) => c.dia === dia)
    .sort((a, b) => COMIDAS.indexOf(a.tipoComida) - COMIDAS.indexOf(b.tipoComida))
  const sinTiempo = semana.diasSinTiempo.includes(dia)
  const marcas = [hoy && 'hoy', sinTiempo && 'sin tiempo'].filter(Boolean).join(' · ')
  const clases = ['dia', hoy && 'dia--hoy', comidas.length === 0 && 'dia--libre'].filter(Boolean).join(' ')

  return (
    <Link className={clases} to={`/plan/dia/${dia}`}>
      <span className="dia__titulo">
        <strong>{tituloDia(semana.lunes, dia)}</strong>
        {marcas && <span className="dia__marcas">{marcas}</span>}
        {comidas.length === 0 && <span className="dia__marcas">día libre</span>}
      </span>
      {comidas.map((c) => {
        const vista = semana.recetas.get(c.recetaId)
        return (
          <span key={c.id} className="dia__comida">
            <span>
              {NOMBRE_COMIDA[c.tipoComida]} · {semana.nombres.get(c.recetaId) ?? 'Receta'}
            </span>
            <span className={c.preparada ? 'dia__estado dia__estado--hecho' : 'dia__estado'}>
              {c.preparada ? 'preparada' : vista?.costo !== undefined ? enPesos(vista.costo) : 'pendiente'}
            </span>
          </span>
        )
      })}
    </Link>
  )
}

export function PrimeroElHogar() {
  return (
    <EstadoVacio
      titulo="Primero, tu hogar"
      texto="Cuéntanos quiénes comen en casa y cuál es tu presupuesto. Así podemos calcular cuánto te cuesta la semana."
      acciones={
        <Link className="boton boton--primario boton--ancho" to="/perfil/hogar">
          Completar perfil del hogar
        </Link>
      }
    />
  )
}

export function ErrorCarga({ error, reintentar }: { error: unknown; reintentar: () => void }) {
  return (
    <Alerta tono="error">
      {mensajePlan(error)}{' '}
      <Boton variante="texto" onClick={reintentar}>
        Reintentar
      </Boton>
    </Alerta>
  )
}
