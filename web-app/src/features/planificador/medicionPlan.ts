import { pedir } from '../../api/cliente'
import { aIso, diaDe, lunesDe } from './semana'
import type { PlanSemanal } from './tipos'

/**
 * Medición del tiempo de planificación semanal para el Plan de Calidad (KAN-54, meta
 * < 3 min): desde que la usuaria abre una semana que no está completa hasta que tiene
 * almuerzo y cena en todos los días que faltan. Se manda una sola vez por semana.
 */

const PREFIJO = 'cocina.plan.abierta.'
const ENVIADO = 'enviado'

/** Almuerzo y cena en cada día que todavía se puede planear (en la semana en curso, de hoy en adelante). */
export function semanaCompleta(lunes: string, plan: PlanSemanal | null, hoy = new Date()): boolean {
  if (!plan) return false
  const primerDia = aIso(lunesDe(hoy)) === lunes ? diaDe(hoy) : 1
  const puestas = new Set(plan.comidas.map((c) => `${c.dia}-${c.tipoComida}`))
  for (let dia = primerDia; dia <= 7; dia++) {
    if (!puestas.has(`${dia}-ALMUERZO`) || !puestas.has(`${dia}-CENA`)) return false
  }
  return true
}

/** Se llama cada vez que se carga la semana. */
export function seguirPlanificacion(lunes: string, plan: PlanSemanal | null, ahora = new Date()) {
  const clave = PREFIJO + lunes
  try {
    const guardado = localStorage.getItem(clave)
    if (!semanaCompleta(lunes, plan, ahora)) {
      if (guardado === null) localStorage.setItem(clave, String(ahora.getTime()))
      return
    }
    // Ya estaba completa cuando la abrió, o ya se mandó.
    if (guardado === null || guardado === ENVIADO) return
    localStorage.setItem(clave, ENVIADO)
    const segundos = Math.max(1, Math.round((ahora.getTime() - Number(guardado)) / 1000))
    if (!Number.isFinite(segundos)) return
    void pedir('/metricas/planificacion', { metodo: 'POST', cuerpo: { semanaInicio: lunes, segundos } }).catch(() => undefined)
  } catch {
    // Sin almacenamiento no se mide.
  }
}
