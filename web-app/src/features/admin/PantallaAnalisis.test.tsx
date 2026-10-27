import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { backendFalso } from '../../prueba/backendFalso'
import { montarApp } from '../../prueba/montar'
import { crearServicioFalso, respuestaJson } from '../../prueba/servicioFalso'
import { textoParaInforme } from './analisis'
import type { Analisis, Indicador } from './apiAnalisis'

const P1 = 'Gestión de usuario y autenticación'
const P3 = 'Planificación semanal con recetario'

function indicador(datos: Partial<Indicador> & Pick<Indicador, 'nombre'>): Indicador {
  return { proceso: P3, formula: 'Fórmula', valor: null, unidad: '%', meta: null, cumple: null, muestra: 0, nota: 'Nota.', ...datos }
}

const ANALISIS: Analisis = {
  desde: '2026-09-26',
  hasta: '2026-10-26',
  semanasDesde: '2026-09-07',
  uso: { usuarias: 12, hogares: 10, activas7Dias: 7, aperturas: 80, planesEstaSemana: 6, preparaciones: 25, preparacionesFuera: 4, usosRescate: 9 },
  indicadores: [
    indicador({ proceso: P1, nombre: 'Completitud de perfil del hogar', valor: 91.7, meta: '> 90 %', cumple: true, muestra: 12 }),
    indicador({ proceso: P1, nombre: 'Éxito de autenticación', unidad: null, meta: '> 95 %', nota: 'Lo registra Firebase Authentication.' }),
    indicador({ nombre: 'Tiempo de planificación semanal', valor: 2.4, unidad: 'min', meta: '< 3 min', cumple: true, muestra: 5 }),
    indicador({ nombre: 'Cumplimiento del presupuesto', valor: 75, meta: '> 85 %', cumple: false, muestra: 8 }),
    indicador({ nombre: 'Uso del modo rescate', valor: 30, muestra: 10 }),
  ],
  masPreparadas: [{ nombre: 'Huevos pericos', veces: 7 }],
  masPlaneadas: [],
  sinUsar: ['Tomate de árbol en salsa'],
}

afterEach(() => {
  vi.restoreAllMocks()
})

describe('Análisis del administrador (KAN-54)', () => {
  it('muestra el uso, los indicadores del plan de calidad con su meta y las recetas', async () => {
    backendFalso({
      'GET /yo': () => respuestaJson({ id: 1, correo: 'admin@correo.com', rol: 'ADMIN', perfilCompleto: false }),
      'GET /admin/analisis': () => respuestaJson(ANALISIS),
    })
    montarApp(crearServicioFalso({ uid: 'a', correo: 'admin@correo.com', nombre: null }).servicio, '/admin/analisis')
    // user-event pone su propio portapapeles: se espía después de crearlo.
    const usuario = userEvent.setup()
    const portapapeles = vi.spyOn(navigator.clipboard, 'writeText')

    expect(await screen.findByText('Últimos 30 días · del 26/09/2026 al 26/10/2026')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Análisis' })).toHaveAttribute('aria-current', 'page')
    expect(screen.getByText('Recetas cocinadas').closest('.cifra')).toHaveTextContent('25')
    expect(screen.getByText('4 fuera del plan')).toBeInTheDocument()

    // De los indicadores con meta y datos, 2 de 3 la alcanzan.
    expect(screen.getByText('2 de 3 indicadores medidos alcanzan su meta.')).toBeInTheDocument()
    const presupuesto = screen.getByRole('listitem', { name: 'Cumplimiento del presupuesto' })
    expect(within(presupuesto).getByText('✗ No cumple')).toBeInTheDocument()
    expect(presupuesto).toHaveTextContent('75 % · meta > 85 %')
    const tiempo = screen.getByRole('listitem', { name: 'Tiempo de planificación semanal' })
    expect(within(tiempo).getByText('✓ Cumple')).toBeInTheDocument()
    expect(tiempo).toHaveTextContent('2,4 min')
    expect(within(screen.getByRole('listitem', { name: 'Éxito de autenticación' })).getByText('Fuera de la app')).toBeInTheDocument()
    expect(within(screen.getByRole('listitem', { name: 'Uso del modo rescate' })).getByText('Seguimiento')).toBeInTheDocument()

    expect(screen.getByText('Huevos pericos')).toBeInTheDocument()
    expect(screen.getByText('Todavía no hay planes.')).toBeInTheDocument()
    expect(screen.getByText('Tomate de árbol en salsa')).toBeInTheDocument()

    await usuario.click(screen.getByRole('button', { name: 'Copiar para el informe' }))
    expect(await screen.findByText('Copiamos la tabla. Pégala en el Plan de Calidad, sección 5.')).toBeInTheDocument()
    expect(portapapeles).toHaveBeenCalledWith(textoParaInforme(ANALISIS))

    await usuario.click(screen.getByRole('link', { name: 'Cuentas' }))
    expect(screen.getByRole('link', { name: 'Cuentas' })).toHaveAttribute('aria-current', 'page')
  })

  it('la tabla del informe trae una fila por indicador, sin los símbolos del estado', () => {
    const lineas = textoParaInforme(ANALISIS).split('\n')
    expect(lineas[0]).toBe('Indicadores de calidad · Cocina sin estrés · 26/09/2026 a 26/10/2026')
    expect(lineas).toHaveLength(2 + ANALISIS.indicadores.length)
    expect(lineas[5]).toBe(`${P3}\tCumplimiento del presupuesto\t75 %\t> 85 %\tNo cumple\t8`)
    expect(lineas[6]).toBe(`${P3}\tUso del modo rescate\t30 %\tSeguimiento\tSeguimiento\t10`)
  })
})
