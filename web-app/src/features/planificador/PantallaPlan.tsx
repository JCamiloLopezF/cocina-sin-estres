import { useState } from 'react'
import { Link } from 'react-router'
import { Alerta, Boton, Cargando, EncabezadoPagina, EstadoVacio } from '../../shared/componentes'
import { enPesos } from '../despensa/formato'
import { asignarComida, crearPlan, obtenerPlan } from './apiPlanificador'
import { mensajePlan, planSinHogar } from './mensajesPlan'
import { aIso, COMIDAS, DIAS, diaDe, fechaDelDia, NOMBRE_COMIDA, rangoSemana, tituloDia } from './semana'
import type { Dia } from './tipos'
import { useSemana, type Semana } from './useSemana'
import './planificador.css'

const NOMBRE_PERIODICIDAD = { SEMANAL: 'semanal', QUINCENAL: 'quincenal', MENSUAL: 'mensual' } as const

/** Pantallas 27, 28 y 30: la semana sin armar, el plan semanal y el aviso de presupuesto. */
export function PantallaPlan() {
  const { estado, recargar } = useSemana()
  const [armando, setArmando] = useState<'nueva' | 'repetir'>()
  const [errorArmar, setErrorArmar] = useState<unknown>()
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

  async function armar(modo: 'nueva' | 'repetir') {
    setArmando(modo)
    setErrorArmar(undefined)
    try {
      const anterior = modo === 'repetir' ? await obtenerPlan(aIso(fechaDelDia(semana.lunes, -6))) : null
      if (modo === 'repetir' && !anterior?.comidas.length) {
        setErrorArmar('La semana pasada no tenías comidas planeadas. Arma esta semana desde cero.')
        return
      }
      const nuevo = await crearPlan(semana.lunes)
      for (const comida of anterior?.comidas ?? []) {
        await asignarComida(nuevo.id, comida.dia, comida.tipoComida, comida.recetaId)
      }
      recargar()
    } catch (error) {
      setErrorArmar(error)
    } finally {
      setArmando(undefined)
    }
  }

  if (!plan) {
    return (
      <>
        <EncabezadoPagina titulo="Mi semana" detalle={rangoSemana(semana.lunes)} />
        <EstadoVacio
          titulo="Todavía no armas esta semana"
          texto="Elige recetas para el almuerzo y la cena. El desayuno es opcional y puedes dejar días libres."
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
            </>
          }
        />
        {errorArmar !== undefined && (
          <Alerta tono={typeof errorArmar === 'string' ? 'info' : 'error'}>
            {typeof errorArmar === 'string' ? errorArmar : mensajePlan(errorArmar)}
          </Alerta>
        )}
      </>
    )
  }

  return (
    <>
      <EncabezadoPagina titulo="Mi semana" detalle={rangoSemana(semana.lunes)} />
      <ResumenCosto semana={semana} />

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
function ResumenCosto({ semana }: { semana: Semana }) {
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
            <strong>Qué puedes hacer:</strong> cambiar la receta más costosa por una más económica o dejar un día libre.
          </p>
          <Link className="boton boton--primario boton--ancho" to="/recetas?atajo=ECONOMICAS">
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
