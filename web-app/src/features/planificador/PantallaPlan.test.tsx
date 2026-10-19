import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { backendFalso } from '../../prueba/backendFalso'
import { rutasCocina } from '../../prueba/cocinaFalsa'
import { montarApp } from '../../prueba/montar'
import { crearServicioFalso, LUISA, respuestaJson } from '../../prueba/servicioFalso'
import { aIso, lunesDe } from './semana'
import type { ComidaPlan } from './tipos'

const LUNES = aIso(lunesDe(new Date()))

function comida(id: number, dia: number, tipoComida: ComidaPlan['tipoComida'], recetaId: number, preparada = false): ComidaPlan {
  return { id, planSemanalId: 7, dia, tipoComida, recetaId, preparada }
}

function conPlan(comidas: ComidaPlan[], costo = { acumulado: [3600, 3600, 16000, 16000, 16000, 16000, 16000], presupuesto: 195000 }) {
  return {
    ...rutasCocina(),
    'GET /planes-semanales': () => respuestaJson({ id: 7, semanaInicio: LUNES, comidas }),
    'GET /planes-semanales/7/costo': () =>
      respuestaJson({
        costoAcumuladoPorDia: costo.acumulado,
        presupuestoSemanal: costo.presupuesto,
        excedePresupuesto: costo.acumulado[6] > costo.presupuesto,
      }),
  }
}

describe('Plan semanal (pantallas 27 a 33)', () => {
  it('sin plan invita a armar la semana y la crea', async () => {
    let creado = false
    const { llamadas } = backendFalso({
      ...rutasCocina(),
      'GET /planes-semanales': () =>
        creado
          ? respuestaJson({ id: 7, semanaInicio: LUNES, comidas: [] })
          : respuestaJson({ status: 404, codigo: 'RECURSO_NO_ENCONTRADO' }, 404),
      'POST /planes-semanales': () => {
        creado = true
        return respuestaJson({ id: 7, semanaInicio: LUNES, comidas: [] })
      },
      'GET /planes-semanales/7/costo': () =>
        respuestaJson({ costoAcumuladoPorDia: [0, 0, 0, 0, 0, 0, 0], presupuestoSemanal: 195000, excedePresupuesto: false }),
    })
    montarApp(crearServicioFalso(LUISA).servicio, '/plan')
    const usuario = userEvent.setup()

    expect(await screen.findByRole('heading', { name: 'Todavía no armas esta semana' })).toBeInTheDocument()
    await usuario.click(screen.getByRole('button', { name: 'Armar mi semana' }))

    expect(await screen.findByRole('link', { name: 'Ver lista de compras' })).toBeInTheDocument()
    expect(llamadas.find((l) => l.metodo === 'POST')?.cuerpo).toEqual({ semanaInicio: LUNES })
    expect(screen.getAllByText('día libre')).toHaveLength(7)
  })

  it('"Repetir la semana pasada" copia las comidas del plan anterior', async () => {
    let creado = false
    const { llamadas } = backendFalso({
      ...rutasCocina(),
      'GET /planes-semanales': (_c, url) => {
        if (url.searchParams.get('semanaInicio') !== LUNES) {
          return respuestaJson({ id: 6, semanaInicio: '2000-01-03', comidas: [{ ...comida(60, 1, 'ALMUERZO', 11), planSemanalId: 6 }] })
        }
        return creado
          ? respuestaJson({ id: 7, semanaInicio: LUNES, comidas: [comida(70, 1, 'ALMUERZO', 11)] })
          : respuestaJson({ status: 404, codigo: 'RECURSO_NO_ENCONTRADO' }, 404)
      },
      'POST /planes-semanales': () => {
        creado = true
        return respuestaJson({ id: 7, semanaInicio: LUNES, comidas: [] })
      },
      'PUT /planes-semanales/7/comidas': (cuerpo) => respuestaJson({ id: 70, planSemanalId: 7, ...(cuerpo as object) }),
      'GET /planes-semanales/7/costo': () =>
        respuestaJson({ costoAcumuladoPorDia: Array(7).fill(12400), presupuestoSemanal: 195000, excedePresupuesto: false }),
    })
    montarApp(crearServicioFalso(LUISA).servicio, '/plan')
    const usuario = userEvent.setup()

    await usuario.click(await screen.findByRole('button', { name: 'Repetir la semana pasada' }))
    expect(await screen.findByText('Almuerzo · Arroz con pollo')).toBeInTheDocument()
    expect(llamadas.find((l) => l.metodo === 'PUT')?.cuerpo).toEqual({ dia: 1, tipoComida: 'ALMUERZO', recetaId: 11 })
  })

  it('muestra costo frente al presupuesto, las comidas por día y avisa si se pasa', async () => {
    backendFalso(
      conPlan([comida(70, 1, 'ALMUERZO', 10, true), comida(71, 3, 'CENA', 11)], {
        acumulado: [3600, 3600, 16000, 16000, 16000, 16000, 16000],
        presupuesto: 10000,
      }),
    )
    montarApp(crearServicioFalso(LUISA).servicio, '/plan')

    const costo = await screen.findByRole('region', { name: 'Costo del plan' })
    expect(costo).toHaveTextContent(/16\.000.*10\.000/)
    expect(within(costo).getByText(/ingreso quincenal/)).toBeInTheDocument()
    const aviso = screen.getByRole('alert')
    expect(aviso).toHaveTextContent('Este plan supera tu presupuesto')
    expect(aviso).toHaveTextContent(/Son \$\s?6\.000 de más/)

    const lunes = screen.getByRole('link', { name: /^Lunes/ })
    expect(lunes).toHaveTextContent('Almuerzo · Sopa de lentejas')
    expect(lunes).toHaveTextContent('preparada')
    const miercoles = screen.getByRole('link', { name: /^Miércoles/ })
    expect(miercoles).toHaveTextContent('sin tiempo')
    expect(miercoles).toHaveTextContent(/Cena · Arroz con pollo\$\s?12\.400/)
    expect(screen.getAllByText('día libre')).toHaveLength(5)
  })

  it('en el día se marca una comida como preparada después de confirmar', async () => {
    const { llamadas } = backendFalso({
      ...conPlan([comida(71, 3, 'CENA', 11)]),
      'PUT /planes-semanales/7/comidas/71/marcar-preparada': () => respuestaJson(comida(71, 3, 'CENA', 11, true)),
    })
    montarApp(crearServicioFalso(LUISA).servicio, '/plan/dia/3')
    const usuario = userEvent.setup()

    expect(await screen.findByText('día sin tiempo')).toBeInTheDocument()
    expect(screen.getAllByRole('link', { name: '+ Agregar receta' }).map((a) => a.getAttribute('href'))).toEqual([
      '/recetas?dia=3&comida=DESAYUNO',
      '/recetas?dia=3&comida=ALMUERZO',
    ])
    expect(screen.getByRole('link', { name: 'Ver opciones rápidas' })).toHaveAttribute(
      'href',
      '/recetas?dia=3&comida=ALMUERZO&tiempo=20',
    )

    await usuario.click(screen.getByRole('button', { name: 'Preparada' }))
    expect(screen.getByText('¿Preparaste esta comida?')).toBeInTheDocument()
    await usuario.click(screen.getByRole('button', { name: 'Sí, la preparé' }))
    expect(await screen.findByText('¡Bien! Marcamos Arroz con pollo como preparada.')).toBeInTheDocument()
    expect(llamadas.some((l) => l.ruta === '/planes-semanales/7/comidas/71/marcar-preparada')).toBe(true)
  })

  it('quitar una comida se puede deshacer', async () => {
    const { llamadas } = backendFalso({
      ...conPlan([comida(71, 3, 'CENA', 11)]),
      'DELETE /planes-semanales/7/comidas/71': () => new Response(null, { status: 204 }),
      'PUT /planes-semanales/7/comidas': (cuerpo) => respuestaJson({ id: 72, planSemanalId: 7, ...(cuerpo as object) }),
    })
    montarApp(crearServicioFalso(LUISA).servicio, '/plan/dia/3')
    const usuario = userEvent.setup()

    await usuario.click(await screen.findByRole('button', { name: 'Quitar' }))
    expect(await screen.findByText(/Quitamos Arroz con pollo del miércoles/)).toBeInTheDocument()
    await usuario.click(screen.getByRole('button', { name: 'Deshacer' }))
    expect(await screen.findByText('Arroz con pollo volvió a tu plan.')).toBeInTheDocument()
    expect(llamadas.find((l) => l.metodo === 'PUT')?.cuerpo).toEqual({ dia: 3, tipoComida: 'CENA', recetaId: 11 })
  })

  it('sin perfil del hogar lleva a completarlo', async () => {
    backendFalso({
      ...rutasCocina(),
      'GET /planes-semanales': () => respuestaJson({ status: 409, codigo: 'PLAN_SIN_PERFIL_HOGAR' }, 409),
    })
    montarApp(crearServicioFalso(LUISA).servicio, '/plan')
    expect(await screen.findByRole('link', { name: 'Completar perfil del hogar' })).toBeInTheDocument()
  })
})

describe('Lista de compras (pantalla 31)', () => {
  it('agrupa por prioridad, nombra los productos y recuerda lo comprado', async () => {
    backendFalso({
      ...conPlan([comida(71, 3, 'CENA', 11)]),
      'GET /planes-semanales/7/lista-compras': () =>
        respuestaJson({
          id: 1,
          planSemanalId: 7,
          costoTotal: 11600,
          items: [
            { id: 1, productoId: 5, cantidad: 500, costoEstimado: 9800, prioridad: 1, comprado: false },
            { id: 2, productoId: 4, cantidad: 400, costoEstimado: 1800, prioridad: 2, comprado: false },
          ],
        }),
    })
    montarApp(crearServicioFalso(LUISA).servicio, '/plan/lista')
    const usuario = userEvent.setup()

    expect(await screen.findByText('2 productos')).toBeInTheDocument()
    expect(screen.getByRole('region', { name: 'Costo estimado' })).toHaveTextContent(/11\.600/)
    const indispensable = within(screen.getByRole('region', { name: 'Indispensable' }))
    expect(indispensable.getByText('Pollo · 500 g')).toBeInTheDocument()
    expect(indispensable.getByText('Arroz con pollo')).toBeInTheDocument()
    expect(within(screen.getByRole('region', { name: 'Postergable' })).getByText('Arroz · 400 g')).toBeInTheDocument()

    await usuario.click(screen.getByRole('checkbox', { name: /Pollo/ }))
    expect(screen.getByText('1 de 2 marcados como comprados')).toBeInTheDocument()
    expect(JSON.parse(localStorage.getItem('cocina-sin-estres:comprados:7') ?? '[]')).toEqual([5])
  })

  it('si la despensa alcanza, dice que no falta nada', async () => {
    backendFalso({
      ...conPlan([comida(71, 3, 'CENA', 10)]),
      'GET /planes-semanales/7/lista-compras': () => respuestaJson({ planSemanalId: 7, costoTotal: 0, items: [] }),
    })
    montarApp(crearServicioFalso(LUISA).servicio, '/plan/lista')
    expect(await screen.findByRole('heading', { name: 'No te falta nada' })).toBeInTheDocument()
  })
})
