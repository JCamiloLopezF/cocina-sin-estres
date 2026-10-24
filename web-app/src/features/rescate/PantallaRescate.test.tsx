import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { backendFalso, CUENTA_USUARIA } from '../../prueba/backendFalso'
import { montarApp } from '../../prueba/montar'
import { crearServicioFalso, LUISA, respuestaJson } from '../../prueba/servicioFalso'
import type { OpcionRescate } from './apiRescate'

const ARROZ_CON_LECHE: OpcionRescate = {
  recetaId: 7,
  nombre: 'Arroz con leche',
  tiempoMinutos: 40,
  estado: 'APTA',
  aprovecha: [{ productoId: 2, nombre: 'Leche', venceEn: '2026-10-25', dias: 1 }],
}
const PERICOS: OpcionRescate = { recetaId: 5, nombre: 'Huevos pericos', tiempoMinutos: 15, estado: 'PLATO_APARTE', aprovecha: [] }

describe('Modo rescate (KAN-25)', () => {
  it('propone lo que sale con la despensa, primero lo que salva algo por vencer', async () => {
    const { llamadas } = backendFalso({
      'GET /yo': () => respuestaJson(CUENTA_USUARIA),
      'GET /rescate': (_cuerpo, url) =>
        respuestaJson({ opciones: url.searchParams.get('maxMinutos') === '20' ? [PERICOS] : [ARROZ_CON_LECHE, PERICOS] }),
    })
    montarApp(crearServicioFalso(LUISA).servicio, '/rescate')
    const usuario = userEvent.setup()

    expect(await screen.findByText('Te salen 2 recetas con lo que tienes.')).toBeInTheDocument()
    const primera = screen.getByRole('article', { name: 'Arroz con leche' })
    expect(within(primera).getByText('La que más te conviene hoy')).toBeInTheDocument()
    expect(within(primera).getByText('Leche · vence mañana')).toBeInTheDocument()
    expect(within(primera).getByRole('link', { name: 'Cocinar ahora' })).toHaveAttribute('href', '/recetas/7/cocinar')
    const pericos = screen.getByRole('article', { name: 'Huevos pericos' })
    expect(within(pericos).getByText('15 min · con un ajuste por alergias')).toBeInTheDocument()

    await usuario.click(screen.getByRole('radio', { name: '20 min' }))
    expect(await screen.findByText('Te sale 1 receta con lo que tienes.')).toBeInTheDocument()
    expect(llamadas.at(-1)?.consulta.get('maxMinutos')).toBe('20')
  })

  it('si no alcanza para nada, invita a revisar la despensa', async () => {
    backendFalso({
      'GET /yo': () => respuestaJson(CUENTA_USUARIA),
      'GET /rescate': () => respuestaJson({ opciones: [] }),
    })
    montarApp(crearServicioFalso(LUISA).servicio, '/rescate')

    expect(await screen.findByRole('heading', { name: 'Con lo que hay no alcanza para una receta' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Revisar mi despensa' })).toHaveAttribute('href', '/despensa')
  })

  it('sin perfil del hogar, pide completarlo primero', async () => {
    backendFalso({
      'GET /yo': () => respuestaJson(CUENTA_USUARIA),
      'GET /rescate': () => respuestaJson({ status: 409, codigo: 'PLAN_SIN_PERFIL_HOGAR' }, 409),
    })
    montarApp(crearServicioFalso(LUISA).servicio, '/rescate')

    expect(await screen.findByRole('heading', { name: 'Primero, tu hogar' })).toBeInTheDocument()
  })
})
