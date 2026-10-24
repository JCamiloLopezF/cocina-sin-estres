import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { backendFalso, CUENTA_USUARIA } from '../../prueba/backendFalso'
import { montarApp } from '../../prueba/montar'
import { crearServicioFalso, LUISA, respuestaJson } from '../../prueba/servicioFalso'
import type { Aviso, PreferenciaAviso } from './apiAvisos'

const POR_VENCER: Aviso = {
  id: 1,
  tipo: 'POR_VENCER',
  titulo: 'Leche vence mañana',
  mensaje: 'Úsalo primero en tus comidas para que no se pierda.',
  enlace: '/despensa?filtro=POR_VENCER',
  accion: 'Ver en la despensa',
  leido: false,
  creadoEn: new Date().toISOString(),
}
const SEMANA: Aviso = {
  id: 2,
  tipo: 'SEMANA_SIN_PLANIFICAR',
  titulo: 'Te armamos la próxima semana',
  mensaje: 'Es un borrador con 14 comidas que cuida las alergias y el presupuesto de tu hogar.',
  enlace: '/plan?semana=proxima',
  accion: 'Revisar la semana',
  leido: false,
  creadoEn: new Date().toISOString(),
}
const PREFERENCIAS: PreferenciaAviso[] = [
  { tipo: 'BAJO_MINIMO', titulo: 'Productos que se acaban', descripcion: 'Cuando llega al mínimo.', activo: true },
  { tipo: 'POR_VENCER', titulo: 'Productos por vencer', descripcion: 'Vence en 3 días.', activo: true },
]

function rutas(extra = {}) {
  return {
    'GET /yo': () => respuestaJson(CUENTA_USUARIA),
    'GET /items-despensa/resumen': () => respuestaJson({ total: 8, porVencer: 2, bajoMinimo: 1 }),
    ...extra,
  }
}

describe('Avisos (KAN-27, ADR-17)', () => {
  it('el inicio muestra los avisos del día; "Ya lo vi" lo quita y lo marca como leído', async () => {
    const { llamadas } = backendFalso(
      rutas({
        'GET /avisos': () => respuestaJson([POR_VENCER, SEMANA]),
        'PUT /avisos/1/leido': () => new Response(null, { status: 204 }),
        'PUT /avisos/2/leido': () => new Response(null, { status: 204 }),
      }),
    )
    montarApp(crearServicioFalso(LUISA).servicio, '/')
    const usuario = userEvent.setup()

    const region = await screen.findByRole('region', { name: 'Avisos de hoy' })
    expect(within(region).getAllByRole('article')).toHaveLength(2)
    expect(within(region).getByRole('link', { name: /Revisar la semana/ })).toHaveAttribute('href', '/plan?semana=proxima')
    // Con los avisos del motor no se repite la tarjeta de "por vencer" de la despensa.
    expect(screen.queryByText('2 productos están por vencer')).not.toBeInTheDocument()

    await usuario.click(screen.getByRole('button', { name: 'Ya lo vi: Leche vence mañana' }))
    expect(screen.queryByText('Leche vence mañana')).not.toBeInTheDocument()
    expect(llamadas.some((l) => l.metodo === 'PUT' && l.ruta === '/avisos/1/leido')).toBe(true)

    await usuario.click(screen.getByRole('link', { name: /Revisar la semana/ }))
    expect(llamadas.some((l) => l.metodo === 'PUT' && l.ruta === '/avisos/2/leido')).toBe(true)
  })

  it('si el motor de avisos falla, el inicio sigue mostrando lo que está por vencer', async () => {
    backendFalso(rutas({ 'GET /avisos': () => respuestaJson({ status: 500, codigo: 'ERROR_INTERNO' }, 500) }))
    montarApp(crearServicioFalso(LUISA).servicio, '/')

    expect(await screen.findByText('2 productos están por vencer')).toBeInTheDocument()
    expect(screen.queryByRole('region', { name: 'Avisos de hoy' })).not.toBeInTheDocument()
    expect(screen.getByRole('link', { name: /Qué cocino hoy con lo que hay/ })).toHaveAttribute('href', '/rescate')
  })

  it('el centro de avisos lista lo reciente, marca todo como leído y apaga un tipo', async () => {
    let preferencias = PREFERENCIAS
    const { llamadas } = backendFalso(
      rutas({
        'GET /avisos/recientes': () => respuestaJson([POR_VENCER, { ...SEMANA, leido: true }]),
        'GET /avisos/preferencias': () => respuestaJson(preferencias),
        'PUT /avisos/leidos': () => new Response(null, { status: 204 }),
        'PUT /avisos/preferencias/POR_VENCER': (cuerpo: unknown) => {
          preferencias = preferencias.map((p) =>
            p.tipo === 'POR_VENCER' ? { ...p, activo: (cuerpo as { activo: boolean }).activo } : p,
          )
          return respuestaJson(preferencias)
        },
      }),
    )
    montarApp(crearServicioFalso(LUISA).servicio, '/avisos')
    const usuario = userEvent.setup()

    expect(await screen.findByText('1 sin leer')).toBeInTheDocument()
    expect(screen.getAllByText('Hoy')).toHaveLength(2)
    // El leído no tiene "Ya lo vi".
    expect(screen.getAllByRole('button', { name: /Ya lo vi/ })).toHaveLength(1)

    await usuario.click(screen.getByRole('button', { name: 'Marcar todo como leído' }))
    expect(await screen.findByText('Estás al día')).toBeInTheDocument()
    expect(llamadas.some((l) => l.metodo === 'PUT' && l.ruta === '/avisos/leidos')).toBe(true)

    const interruptor = screen.getByRole('switch', { name: /Productos por vencer/ })
    expect(interruptor).toBeChecked()
    await usuario.click(interruptor)
    expect(await screen.findByText('Listo, ya no te avisamos de «productos por vencer».')).toBeInTheDocument()
    expect(screen.getByRole('switch', { name: /Productos por vencer/ })).not.toBeChecked()
    expect(llamadas.find((l) => l.ruta === '/avisos/preferencias/POR_VENCER')?.cuerpo).toEqual({ activo: false })
  })

  it('sin avisos, el centro lo explica', async () => {
    backendFalso(
      rutas({
        'GET /avisos/recientes': () => respuestaJson([]),
        'GET /avisos/preferencias': () => respuestaJson(PREFERENCIAS),
      }),
    )
    montarApp(crearServicioFalso(LUISA).servicio, '/avisos')

    expect(await screen.findByRole('heading', { name: 'Todavía no hay avisos' })).toBeInTheDocument()
    expect(screen.getByText('Estás al día')).toBeInTheDocument()
  })
})
