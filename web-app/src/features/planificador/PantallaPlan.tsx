import { useEffect, useState } from 'react'
import { Link } from 'react-router'
import { Alerta, Boton, Cargando, EncabezadoPagina, EstadoVacio } from '../../shared/componentes'
import { enPesos } from '../despensa/formato'
import { ajustarPresupuesto, asignarComida, confirmarBorrador, crearPlan, generarPlan, obtenerPlan } from './apiPlanificador'
import { AvisoSinConexion } from './AvisoSinConexion'
import { seguirPlanificacion } from './medicionPlan'
import { mensajePlan, planSinHogar } from './mensajesPlan'
import { aIso, COMIDAS, conSemana, DIAS, diaDe, fechaDelDia, NOMBRE_COMIDA, NOMBRE_DIA, rangoSemana, tituloDia } from './semana'
import type { Dia, PlanGenerado, TipoComida } from './tipos'
import { useSemana, type Semana } from './useSemana'
import './planificador.css'

const NOMBRE_PERIODICIDAD = { SEMANAL: 'semanal', QUINCENAL: 'quincenal', MENSUAL: 'mensual' } as const

/** Comidas que llena "Armar mi semana": el desayuno es opcional y la usuaria lo pide. */
const SIN_DESAYUNO: TipoComida[] = ['ALMUERZO', 'CENA']
const CON_DESAYUNO: TipoComida[] = ['DESAYUNO', 'ALMUERZO', 'CENA']
const CLAVE_DESAYUNOS = 'cocina.plan.desayunos'

/** Si la usuaria quiere que también le propongamos desayunos (se recuerda en este navegador). */
function leerDesayunos(): boolean {
  try {
    return localStorage.getItem(CLAVE_DESAYUNOS) === 'si'
  } catch {
    return false
  }
}

function guardarDesayunos(valor: boolean) {
  try {
    localStorage.setItem(CLAVE_DESAYUNOS, valor ? 'si' : 'no')
  } catch {
    // Sin almacenamiento (modo privado): solo dura mientras la pantalla esté abierta.
  }
}

type Accion = 'nueva' | 'repetir' | 'vacia' | 'completar' | 'ajustar' | 'confirmar'

/** "Pollo" · "Pollo y leche" · "Pollo, leche y queso". */
function enLista(nombres: string[]): string {
  const minusculas = nombres.map((n) => n.toLowerCase())
  return minusculas.length <= 1 ? (minusculas[0] ?? '') : `${minusculas.slice(0, -1).join(', ')} y ${minusculas[minusculas.length - 1]}`
}

/** Lo que se le cuenta a la usuaria después de que el sistema armó o ajustó la semana. */
function contarResultado(resultado: PlanGenerado, accion: 'armar' | 'ajustar'): string {
  const n = resultado.cambios.length
  // RNF-02: el costo del plan es lo que falta comprar con lo que ya hay en la despensa.
  const costo = `${enPesos(resultado.costoPorComprar ?? resultado.costoEstimado)} por comprar de ${enPesos(resultado.presupuestoSemanal)}`
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
  const diasDelPago = resultado.diasDespuesDelPago ?? []
  if (diasDelPago.length) {
    partes.push(`Dejamos las comidas más costosas para ${enLista(diasDelPago.map((d) => `el ${NOMBRE_DIA[d]}`))}, cuando recién te pagan.`)
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
  const { estado, recargar, proxima } = useSemana()
  const [armando, setArmando] = useState<Accion>()
  const [conDesayuno, setConDesayuno] = useState(leerDesayunos)
  const [errorArmar, setErrorArmar] = useState<unknown>()
  const [resultado, setResultado] = useState<string>()
  // "Hoy" solo se marca en la semana actual.
  const [hoy] = useState(() => diaDe(new Date()))
  const titulo = proxima ? 'Próxima semana' : 'Mi semana'

  // Tiempo de planificación semanal (KAN-54): desde que abre la semana sin completar.
  useEffect(() => {
    if (estado.tipo === 'listo') seguirPlanificacion(estado.semana.lunes, estado.semana.plan)
  }, [estado])

  if (estado.tipo === 'cargando') {
    return (
      <>
        <EncabezadoPagina titulo={titulo} />
        <SelectorSemana proxima={proxima} />
        <Cargando texto="Cargando tu semana…" />
      </>
    )
  }
  if (estado.tipo === 'error') {
    return (
      <>
        <EncabezadoPagina titulo={titulo} />
        {planSinHogar(estado.error) ? <PrimeroElHogar /> : <ErrorCarga error={estado.error} reintentar={recargar} />}
      </>
    )
  }

  const { semana } = estado
  const { plan } = semana

  const comidasALlenar = conDesayuno ? CON_DESAYUNO : SIN_DESAYUNO

  function cambiarDesayunos(valor: boolean) {
    setConDesayuno(valor)
    guardarDesayunos(valor)
  }

  const opcionDesayunos = (
    <label className="casilla casilla--tarjeta">
      Proponme también desayunos
      <input type="checkbox" checked={conDesayuno} onChange={(e) => cambiarDesayunos(e.target.checked)} />
    </label>
  )

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
        setResultado(contarResultado(await generarPlan(nuevo.id, comidasALlenar), 'armar'))
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
      const respuesta = accion === 'completar' ? await generarPlan(plan.id, comidasALlenar) : await ajustarPresupuesto(plan.id)
      setResultado(contarResultado(respuesta, accion === 'completar' ? 'armar' : 'ajustar'))
      recargar()
    } catch (error) {
      setErrorArmar(error)
    } finally {
      setArmando(undefined)
    }
  }

  /** La usuaria se queda con el borrador que armó la app. */
  async function confirmar() {
    if (!plan) return
    setArmando('confirmar')
    setErrorArmar(undefined)
    try {
      await confirmarBorrador(plan.id)
      setResultado('Listo: la semana queda así. La puedes seguir cambiando cuando quieras.')
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
        <EncabezadoPagina titulo={titulo} detalle={rangoSemana(semana.lunes)} />
        <SelectorSemana proxima={proxima} />
        <AvisoSinConexion />
        <EstadoVacio
          titulo={proxima ? 'Todavía no armas la próxima semana' : 'Todavía no armas esta semana'}
          texto={`Te proponemos ${conDesayuno ? 'desayuno, almuerzo' : 'almuerzo'} y cena para los siete días según lo que hay en tu despensa, tu presupuesto, las alergias de cada persona y los días en que no tienes tiempo. Después cambias lo que quieras.`}
          acciones={
            <>
              {opcionDesayunos}
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
              {/* Junto a los botones: arriba de la página quedaba fuera de la vista (auditoría). */}
              {avisoError}
              <Boton variante="texto" cargando={armando === 'vacia'} disabled={armando !== undefined} onClick={() => void armar('vacia')}>
                Prefiero armarla yo
              </Boton>
            </>
          }
        />
      </>
    )
  }

  // Los días que ya pasaron no se planean (el backend tampoco los llena).
  const faltan = DIAS.filter((dia) => proxima || dia >= hoy).reduce(
    (total, dia) => total + comidasALlenar.filter((tipo) => !plan.comidas.some((c) => c.dia === dia && c.tipoComida === tipo)).length,
    0,
  )

  return (
    <>
      <EncabezadoPagina titulo={titulo} detalle={rangoSemana(semana.lunes)} />
      <SelectorSemana proxima={proxima} />
      <AvisoSinConexion />
      {plan.borrador && (
        <section className="borrador-plan" aria-labelledby="titulo-borrador">
          <h2 id="titulo-borrador">La armamos por ti</h2>
          <p>
            Es un borrador con lo que hay en tu despensa, tu presupuesto y las alergias de tu hogar. Revisa cada día y
            cambia lo que quieras; cuando te guste, confírmalo.
          </p>
          <Boton ancho cargando={armando === 'confirmar'} disabled={armando !== undefined} onClick={() => void confirmar()}>
            Me sirve, la dejo así
          </Boton>
        </section>
      )}
      {resultado && (
        <div className="plan__resultado">
          <Alerta tono="exito">{resultado}</Alerta>
        </div>
      )}
      {avisoError}
      <ResumenCosto semana={semana} ajustando={armando === 'ajustar'} alAjustar={() => void automatizar('ajustar')} />

      {(faltan > 0 || !conDesayuno) && (
        <section className="completar-plan" aria-labelledby="titulo-completar">
          <h2 id="titulo-completar">
            {faltan === 0
              ? 'Tu semana tiene almuerzo y cena'
              : faltan === 1
                ? 'Falta 1 comida en tu semana'
                : `Faltan ${faltan} comidas en tu semana`}
          </h2>
          <p>
            {faltan === 0
              ? 'Si quieres, también te proponemos desayunos.'
              : `Te proponemos recetas para ${conDesayuno ? 'el desayuno, el almuerzo y la cena' : 'el almuerzo y la cena'} que están vacíos. Lo que ya elegiste no cambia.`}
          </p>
          {opcionDesayunos}
          {faltan > 0 && (
            <Boton ancho variante="secundario" cargando={armando === 'completar'} disabled={armando !== undefined} onClick={() => void automatizar('completar')}>
              Completar los días vacíos
            </Boton>
          )}
        </section>
      )}

      <ul className="semana">
        {DIAS.map((dia) => (
          <li key={dia}>
            <TarjetaDia semana={semana} dia={dia} hoy={!proxima && hoy === dia} proxima={proxima} />
          </li>
        ))}
      </ul>

      <Link className="boton boton--primario boton--ancho plan__lista" to={conSemana('/plan/lista', proxima)}>
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
  const valor = costo.costoAcumuladoPorDia[costo.costoAcumuladoPorDia.length - 1] ?? 0
  // RNF-02: el costo del plan es lo que falta comprar; el valor de las comidas va aparte.
  const total = costo.costoPorComprar ?? valor
  const presupuesto = costo.presupuestoSemanal
  const porcentaje = presupuesto > 0 ? Math.min(100, (total / presupuesto) * 100) : 0
  return (
    <>
      <section className="costo-plan" aria-label="Costo del plan">
        <p className="costo-plan__fila">
          <span>Por comprar</span>
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
        {costo.costoPorComprar !== undefined && valor > total && (
          <p className="costo-plan__nota">
            Las comidas de la semana valen {enPesos(valor)}; {enPesos(valor - total)} ya los tienes en la despensa.
          </p>
        )}
        {(costo.costoAlergias ?? 0) > 0 && (
          <p className="costo-plan__nota">
            Incluye {enPesos(costo.costoAlergias ?? 0)} por la porción de quien tiene alergia.
          </p>
        )}
        {semana.periodicidad && (
          <p className="costo-plan__nota">
            Tu presupuesto {NOMBRE_PERIODICIDAD[semana.periodicidad]} de alimentación, repartido por semana.
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
            Para el plan de la semana te falta comprar {enPesos(total)} y tu presupuesto semanal es de{' '}
            {enPesos(presupuesto)}. Son {enPesos(total - presupuesto)} de más.
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

/** Esta semana o la siguiente (donde queda el borrador que arma la app desde el viernes). */
function SelectorSemana({ proxima }: { proxima: boolean }) {
  return (
    <nav className="selector-semana" aria-label="Semana">
      <Link className="selector-semana__opcion" to="/plan" aria-current={proxima ? undefined : 'page'}>
        Esta semana
      </Link>
      <Link className="selector-semana__opcion" to="/plan?semana=proxima" aria-current={proxima ? 'page' : undefined}>
        Próxima semana
      </Link>
    </nav>
  )
}

function TarjetaDia({ semana, dia, hoy, proxima }: { semana: Semana; dia: Dia; hoy: boolean; proxima: boolean }) {
  const comidas = (semana.plan?.comidas ?? [])
    .filter((c) => c.dia === dia)
    .sort((a, b) => COMIDAS.indexOf(a.tipoComida) - COMIDAS.indexOf(b.tipoComida))
  const sinTiempo = semana.diasSinTiempo.includes(dia)
  const marcas = [hoy && 'hoy', sinTiempo && 'sin tiempo'].filter(Boolean).join(' · ')
  const clases = ['dia', hoy && 'dia--hoy', comidas.length === 0 && 'dia--libre'].filter(Boolean).join(' ')

  return (
    <Link className={clases} to={conSemana(`/plan/dia/${dia}`, proxima)}>
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
