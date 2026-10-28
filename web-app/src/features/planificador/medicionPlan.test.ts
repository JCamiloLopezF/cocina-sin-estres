import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { configurarCliente } from '../../api/cliente'
import { backendFalso } from '../../prueba/backendFalso'
import { seguirPlanificacion, semanaCompleta } from './medicionPlan'
import type { ComidaPlan, PlanSemanal } from './tipos'

const LUNES = '2026-10-26'

function plan(dias: number[]): PlanSemanal {
  const comidas: ComidaPlan[] = dias.flatMap((dia) =>
    (['ALMUERZO', 'CENA'] as const).map((tipoComida, i) => ({
      id: dia * 10 + i,
      planSemanalId: 1,
      dia,
      tipoComida,
      recetaId: 1,
      preparada: false,
    })),
  )
  return { id: 1, semanaInicio: LUNES, comidas }
}

describe('Tiempo de planificación semanal (KAN-54)', () => {
  beforeEach(() => {
    localStorage.clear()
    configurarCliente({ urlBase: 'https://api.ejemplo.com/api/', credenciales: async () => ({}) })
  })
  afterEach(() => localStorage.clear())

  it('en la semana en curso solo pide los días de hoy en adelante', () => {
    const miercoles = new Date(2026, 9, 28, 10)
    expect(semanaCompleta(LUNES, plan([3, 4, 5, 6, 7]), miercoles)).toBe(true)
    expect(semanaCompleta(LUNES, plan([3, 4, 5, 6]), miercoles)).toBe(false)
    // Otra semana: los siete días.
    expect(semanaCompleta('2026-11-02', { ...plan([3, 4, 5, 6, 7]), semanaInicio: '2026-11-02' }, miercoles)).toBe(false)
  })

  it('mide desde que abre la semana incompleta hasta que queda completa, una sola vez', async () => {
    const { llamadas } = backendFalso({ 'POST /metricas/planificacion': () => new Response(null, { status: 204 }) })
    const lunes = new Date(2026, 9, 26, 9, 0, 0)

    seguirPlanificacion(LUNES, null, lunes)
    seguirPlanificacion(LUNES, plan([1, 2, 3]), new Date(2026, 9, 26, 9, 1, 0))
    seguirPlanificacion(LUNES, plan([1, 2, 3, 4, 5, 6, 7]), new Date(2026, 9, 26, 9, 2, 30))
    seguirPlanificacion(LUNES, plan([1, 2, 3, 4, 5, 6, 7]), new Date(2026, 9, 26, 10, 0, 0))
    await Promise.resolve()

    expect(llamadas).toHaveLength(1)
    expect(llamadas[0].cuerpo).toEqual({ semanaInicio: LUNES, segundos: 150 })
  })

  it('si ya estaba completa cuando la abrió, no mide nada', async () => {
    const { llamadas } = backendFalso({})

    seguirPlanificacion(LUNES, plan([1, 2, 3, 4, 5, 6, 7]), new Date(2026, 9, 26, 9))
    await Promise.resolve()

    expect(llamadas).toHaveLength(0)
  })
})
