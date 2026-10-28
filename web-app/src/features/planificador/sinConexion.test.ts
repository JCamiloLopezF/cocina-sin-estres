import { beforeEach, describe, expect, it, vi } from 'vitest'
import { configurarCliente, limpiarRespaldos } from '../../api/cliente'
import { backendFalso } from '../../prueba/backendFalso'
import { respuestaJson } from '../../prueba/servicioFalso'
import { asignarComida, obtenerPlan, quitarComida } from './apiPlanificador'
import { leerPendientes, sincronizar } from './sinConexion'

const LUNES = '2026-10-26'
const PLAN = {
  id: 7,
  semanaInicio: LUNES,
  comidas: [{ id: 70, planSemanalId: 7, dia: 1, tipoComida: 'ALMUERZO', recetaId: 10, preparada: false }],
}

function sinSenal() {
  vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('Failed to fetch')))
}

describe('Plan sin conexión (RNF-17)', () => {
  beforeEach(() => {
    configurarCliente({ urlBase: 'http://localhost/api', credenciales: async () => ({}) })
    limpiarRespaldos()
  })

  it('un cambio sin señal se guarda, se ve en el plan y se envía al volver, una sola vez', async () => {
    // Con señal se ve el plan: queda guardado para usarlo sin conexión.
    backendFalso({ 'GET /planes-semanales': () => respuestaJson(PLAN) })
    expect((await obtenerPlan(LUNES))?.comidas).toHaveLength(1)

    sinSenal()
    const provisional = await asignarComida(7, 2, 'CENA', 11)
    expect(provisional).toMatchObject({ dia: 2, tipoComida: 'CENA', recetaId: 11, pendiente: true })
    expect(provisional.id).toBeLessThan(0)
    expect(leerPendientes()).toHaveLength(1)

    // Sin señal el plan sale de lo guardado, con el cambio pendiente.
    const sinRed = await obtenerPlan(LUNES)
    expect(sinRed?.comidas.map((c) => [c.dia, c.tipoComida, c.recetaId])).toEqual([
      [1, 'ALMUERZO', 10],
      [2, 'CENA', 11],
    ])
    expect(await sincronizar()).toEqual({ enviados: 0, rechazados: 0, quedan: 1 })

    // Vuelve la señal: se envía con su idCambioCliente y la cola queda vacía.
    const { llamadas } = backendFalso({
      'PUT /planes-semanales/7/comidas': (cuerpo) => respuestaJson({ id: 71, planSemanalId: 7, preparada: false, ...(cuerpo as object) }),
    })
    expect(await sincronizar()).toEqual({ enviados: 1, rechazados: 0, quedan: 0 })
    expect(llamadas).toHaveLength(1)
    expect(llamadas[0].cuerpo).toMatchObject({ dia: 2, tipoComida: 'CENA', recetaId: 11 })
    expect((llamadas[0].cuerpo as { idCambioCliente: string }).idCambioCliente).toBeTruthy()
    expect(leerPendientes()).toEqual([])
  })

  it('quitar una comida que no se ha enviado la olvida; quitar una guardada espera la señal', async () => {
    sinSenal()
    const provisional = await asignarComida(7, 3, 'ALMUERZO', 12)
    await quitarComida(7, provisional.id)
    expect(leerPendientes()).toEqual([])

    await quitarComida(7, 70)
    expect(leerPendientes()).toMatchObject([{ tipo: 'QUITAR', planId: 7, comidaId: 70 }])

    // Si al volver ya no existía (404), el resultado es el mismo: se descarta sin error.
    backendFalso({})
    expect(await sincronizar()).toEqual({ enviados: 0, rechazados: 0, quedan: 0 })
  })

  it('un cambio que el servidor rechaza no se reintenta para siempre', async () => {
    sinSenal()
    await asignarComida(7, 4, 'CENA', 999)
    backendFalso({
      'PUT /planes-semanales/7/comidas': () =>
        respuestaJson({ status: 400, codigo: 'RECETA_NO_DISPONIBLE', detail: 'La receta no existe.' }, 400),
    })
    expect(await sincronizar()).toEqual({ enviados: 0, rechazados: 1, quedan: 0 })
  })
})
