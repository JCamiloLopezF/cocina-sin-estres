import { ErrorApi, pedir, SIN_CONEXION } from '../../api/cliente'
import type { ComidaPlan, PlanSemanal, TipoComida } from './tipos'

/**
 * RNF-17: si se pierde la conexión mientras se arma la semana, los cambios no se pierden. Se
 * guardan en este navegador y se envían, en orden, cuando vuelve la señal. Cada asignación
 * lleva su `idCambioCliente`: si el envío llegó pero la respuesta no, reenviarla no la duplica.
 */

export type Pendiente =
  | { id: string; tipo: 'ASIGNAR'; planId: number; dia: number; tipoComida: TipoComida; recetaId: number }
  | { id: string; tipo: 'QUITAR'; planId: number; comidaId: number }

const CLAVE = 'cocina.plan.pendientes'

export function leerPendientes(): Pendiente[] {
  try {
    const guardado = localStorage.getItem(CLAVE)
    return guardado ? (JSON.parse(guardado) as Pendiente[]) : []
  } catch {
    return []
  }
}

function guardarPendientes(pendientes: Pendiente[]): void {
  try {
    if (pendientes.length) {
      localStorage.setItem(CLAVE, JSON.stringify(pendientes))
    } else {
      localStorage.removeItem(CLAVE)
    }
  } catch {
    // Sin almacenamiento (modo privado): el cambio solo dura mientras la pantalla esté abierta.
  }
  avisar()
}

/** Se borran al cerrar sesión: son de esa cuenta. */
export function limpiarPendientes(): void {
  guardarPendientes([])
}

export function esSinConexion(error: unknown): boolean {
  return error instanceof ErrorApi && error.codigo === SIN_CONEXION
}

/** Identificador del cambio; se usa también como `idCambioCliente` (máximo 64 caracteres). */
export function nuevoId(): string {
  return typeof crypto !== 'undefined' && 'randomUUID' in crypto
    ? crypto.randomUUID()
    : `${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 12)}`
}

/** Las comidas que todavía no llegan al servidor tienen id negativo. */
function idProvisional(pendiente: Pendiente): number {
  let suma = 0
  for (const letra of pendiente.id) suma = (suma * 31 + letra.charCodeAt(0)) % 1_000_000_000
  return -(suma + 1)
}

export function encolar(pendiente: Pendiente): void {
  const pendientes = leerPendientes()
  if (pendiente.tipo === 'ASIGNAR') {
    // Una asignación nueva en el mismo hueco reemplaza la anterior que no se ha enviado.
    guardarPendientes([
      ...pendientes.filter(
        (p) => !(p.tipo === 'ASIGNAR' && p.planId === pendiente.planId && p.dia === pendiente.dia && p.tipoComida === pendiente.tipoComida),
      ),
      pendiente,
    ])
    return
  }
  guardarPendientes([...pendientes, pendiente])
}

/** Quitar una comida que todavía no se envió es simplemente olvidarla. */
export function olvidarProvisional(comidaId: number): boolean {
  const pendientes = leerPendientes()
  const restantes = pendientes.filter((p) => !(p.tipo === 'ASIGNAR' && idProvisional(p) === comidaId))
  if (restantes.length === pendientes.length) return false
  guardarPendientes(restantes)
  return true
}

/** La comida como se verá mientras se envía. */
export function comidaProvisional(pendiente: Extract<Pendiente, { tipo: 'ASIGNAR' }>): ComidaPlan {
  return {
    id: idProvisional(pendiente),
    planSemanalId: pendiente.planId,
    dia: pendiente.dia,
    tipoComida: pendiente.tipoComida,
    recetaId: pendiente.recetaId,
    preparada: false,
    pendiente: true,
  }
}

/** El plan con los cambios que faltan por enviar ya aplicados, para que la usuaria los vea. */
export function conPendientes(plan: PlanSemanal): PlanSemanal {
  let comidas = [...plan.comidas]
  for (const p of leerPendientes().filter((p) => p.planId === plan.id)) {
    if (p.tipo === 'ASIGNAR') {
      comidas = [...comidas.filter((c) => !(c.dia === p.dia && c.tipoComida === p.tipoComida)), comidaProvisional(p)]
    } else {
      comidas = comidas.filter((c) => c.id !== p.comidaId)
    }
  }
  return { ...plan, comidas }
}

let enCurso: Promise<ResultadoSincronizacion> | undefined

export interface ResultadoSincronizacion {
  enviados: number
  /** Cambios que el servidor rechazó (por ejemplo, la receta ya no existe): no se reintentan. */
  rechazados: number
  /** Todavía sin conexión: quedan para después. */
  quedan: number
}

/** Envía los cambios guardados, en el orden en que se hicieron. Dos llamadas a la vez comparten el envío. */
export function sincronizar(): Promise<ResultadoSincronizacion> {
  enCurso ??= enviar().finally(() => {
    enCurso = undefined
  })
  return enCurso
}

async function enviar(): Promise<ResultadoSincronizacion> {
  let enviados = 0
  let rechazados = 0
  for (const pendiente of leerPendientes()) {
    try {
      if (pendiente.tipo === 'ASIGNAR') {
        await pedir<ComidaPlan>(`/planes-semanales/${pendiente.planId}/comidas`, {
          metodo: 'PUT',
          cuerpo: {
            dia: pendiente.dia,
            tipoComida: pendiente.tipoComida,
            recetaId: pendiente.recetaId,
            idCambioCliente: pendiente.id,
          },
        })
      } else {
        await pedir<void>(`/planes-semanales/${pendiente.planId}/comidas/${pendiente.comidaId}`, { metodo: 'DELETE' })
      }
      enviados++
    } catch (error) {
      if (esSinConexion(error)) {
        break
      }
      // Un 404 al quitar es que ya no estaba: el resultado es el mismo.
      if (!(pendiente.tipo === 'QUITAR' && error instanceof ErrorApi && error.status === 404)) {
        rechazados++
      }
    }
    guardarPendientes(leerPendientes().filter((p) => p.id !== pendiente.id))
  }
  return { enviados, rechazados, quedan: leerPendientes().length }
}

// Las pantallas se enteran cuando cambia la cola (para el aviso "N cambios por enviar").
const oyentes = new Set<() => void>()

export function alCambiarPendientes(oyente: () => void): () => void {
  oyentes.add(oyente)
  return () => oyentes.delete(oyente)
}

function avisar(): void {
  oyentes.forEach((oyente) => oyente())
}
