import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { backendFalso } from '../../prueba/backendFalso'
import { montarApp } from '../../prueba/montar'
import { crearServicioFalso, respuestaJson } from '../../prueba/servicioFalso'
import type { ProductoCurado, RecetaCurada, RecetaCuradaResumen } from './apiCuraduria'
import { faltaParaAlmuerzo, problemaDelPaso, tiemposDelPaso } from './recetaCurada'

const CURADORA = { uid: 'c', correo: 'curadora@correo.com', nombre: null }

function producto(id: number, nombre: string, categoria: string, unidadBase: 'g' | 'unidad', cantidad: number, precio: number): ProductoCurado {
  return {
    id,
    nombre,
    categoria,
    tipoCantidad: unidadBase === 'g' ? 'GRANEL' : 'CONTABLE',
    unidadBase,
    unidadCompra: unidadBase === 'g' ? 'Libra' : 'Unidad',
    cantidadUnidadCompra: cantidad,
    precioUnidadCompra: precio,
    esBasico: false,
    activo: true,
  }
}

const PRODUCTOS = [
  producto(1, 'Arroz', 'GRANOS', 'g', 1000, 4000),
  producto(2, 'Pollo', 'PROTEINAS', 'g', 500, 12000),
  producto(3, 'Tomate', 'VERDURAS', 'g', 500, 4000),
  producto(4, 'Huevos', 'PROTEINAS', 'unidad', 30, 18000),
]

const SUDADO: RecetaCurada = {
  id: 40,
  nombre: 'Sudado de pollo',
  descripcion: 'Pollo en su jugo.',
  tiempoMinutos: 60,
  porciones: 4,
  pasos: ['Pica el tomate.', 'Cocina el pollo 20 minutos.'],
  preparacionPrevia: null,
  ingredientes: [
    { productoId: 2, nombre: 'Pollo', categoria: 'PROTEINAS', unidadBase: 'g', cantidad: 500, opcional: false, principal: true, activo: true },
    { productoId: 1, nombre: 'Arroz', categoria: 'GRANOS', unidadBase: 'g', cantidad: 400, opcional: false, principal: false, activo: true },
  ],
  momentos: ['ALMUERZO', 'CENA'],
  platoFuerte: true,
  gustaANinos: false,
  activa: true,
  costo: 13600,
  tieneVideo: false,
}

const RESUMEN: RecetaCuradaResumen = {
  id: 40,
  nombre: 'Sudado de pollo',
  tiempoMinutos: 60,
  porciones: 4,
  momentos: ['ALMUERZO', 'CENA'],
  platoFuerte: true,
  activa: true,
  ingredientes: 2,
  costo: 13600,
  tieneVideo: false,
}

function backend(extra = {}) {
  return backendFalso({
    'GET /yo': () => respuestaJson({ id: 2, correo: CURADORA.correo, rol: 'CURADOR', perfilCompleto: false }),
    'GET /curaduria/productos': () => respuestaJson(PRODUCTOS),
    'GET /curaduria/recetas/catalogo': () => respuestaJson([RESUMEN, { ...RESUMEN, id: 41, nombre: 'Receta vieja', activa: false }]),
    'GET /curaduria/recetas/40': () => respuestaJson(SUDADO),
    ...extra,
  })
}

describe('Recetas del curador (RNF-28)', () => {
  it('reconoce los pasos con dos tiempos y lo que le falta a un almuerzo', () => {
    expect(tiemposDelPaso('Sofríe 2 minutos y cocina 4 minutos.')).toEqual(['2 minutos', '4 minutos'])
    expect(tiemposDelPaso('Cocina media hora.')).toEqual(['media hora'])
    expect(problemaDelPaso('Cocina 10 minutos.')).toBeUndefined()
    expect(problemaDelPaso('Hierve según el empaque.')).toMatch(/no remitas/)
    expect(faltaParaAlmuerzo(['PROTEINAS', 'GRANOS'])).toBeUndefined()
    expect(faltaParaAlmuerzo(['PROTEINAS', 'VERDURAS'])).toMatch(/algo que llene/)
    expect(faltaParaAlmuerzo(['LEGUMBRES', 'TUBERCULOS'])).toBeUndefined()
  })

  it('lista las recetas y oculta las desactivadas hasta que se piden', async () => {
    backend()
    montarApp(crearServicioFalso(CURADORA).servicio, '/curaduria/recetas')
    const usuario = userEvent.setup()

    const fila = await screen.findByRole('link', { name: /Sudado de pollo/ })
    expect(within(fila).getByText(/Almuerzo y cena · 60 min · 2 ingredientes/)).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: /Receta vieja/ })).not.toBeInTheDocument()

    await usuario.click(screen.getByRole('checkbox', { name: 'Ver desactivadas (1)' }))
    expect(screen.getByRole('link', { name: /Receta vieja · desactivada/ })).toBeInTheDocument()
  })

  it('crea una receta con sus ingredientes, pasos y comidas', async () => {
    const { llamadas } = backend({
      'POST /curaduria/recetas': (cuerpo: unknown) => respuestaJson({ ...SUDADO, id: 50, ...(cuerpo as object) }, 201),
    })
    const { router } = montarApp(crearServicioFalso(CURADORA).servicio, '/curaduria/recetas/nueva')
    const usuario = userEvent.setup()

    await usuario.type(await screen.findByLabelText('Nombre'), 'Arroz con pollo de la abuela')
    await usuario.type(screen.getByLabelText('Tiempo total'), '50')
    await usuario.click(screen.getByRole('checkbox', { name: 'Almuerzo' }))
    await usuario.selectOptions(screen.getByLabelText('Ingrediente 1'), 'Pollo')
    await usuario.type(screen.getByLabelText('Cantidad del ingrediente 1'), '500')
    await usuario.selectOptions(screen.getByLabelText('Ingrediente 2'), 'Arroz')
    await usuario.type(screen.getByLabelText('Cantidad del ingrediente 2'), '400')
    await usuario.click(screen.getByRole('button', { name: 'Quitar el ingrediente 3' }))
    expect(screen.getByText(/Costo de referencia: \$\s?13\.600 · \$\s?3\.400 por porción/)).toBeInTheDocument()
    await usuario.type(screen.getByLabelText('Pasos'), 'Dora el pollo 8 minutos.{enter}Agrega el arroz y cocina 20 minutos.')
    await usuario.click(screen.getByRole('button', { name: 'Guardar receta' }))

    expect(await screen.findByText(/quedó guardada/)).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/curaduria/recetas')
    expect(llamadas.find((l) => l.metodo === 'POST')?.cuerpo).toEqual({
      nombre: 'Arroz con pollo de la abuela',
      tiempoMinutos: 50,
      porciones: 4,
      pasos: ['Dora el pollo 8 minutos.', 'Agrega el arroz y cocina 20 minutos.'],
      ingredientes: [
        { productoId: 2, cantidad: 500, opcional: false, principal: false },
        { productoId: 1, cantidad: 400, opcional: false, principal: false },
      ],
      momentos: ['ALMUERZO'],
      platoFuerte: true,
      gustaANinos: false,
    })
  })

  it('no envía un almuerzo incompleto ni un paso con dos tiempos', async () => {
    const { llamadas } = backend()
    montarApp(crearServicioFalso(CURADORA).servicio, '/curaduria/recetas/nueva')
    const usuario = userEvent.setup()

    await usuario.type(await screen.findByLabelText('Nombre'), 'Pericos')
    await usuario.type(screen.getByLabelText('Tiempo total'), '15')
    await usuario.click(screen.getByRole('checkbox', { name: 'Almuerzo' }))
    await usuario.selectOptions(screen.getByLabelText('Ingrediente 1'), 'Huevos')
    await usuario.type(screen.getByLabelText('Cantidad del ingrediente 1'), '4')
    await usuario.selectOptions(screen.getByLabelText('Ingrediente 2'), 'Tomate')
    await usuario.type(screen.getByLabelText('Cantidad del ingrediente 2'), '100')
    await usuario.type(screen.getByLabelText('Pasos'), 'Sofríe el tomate 2 minutos y los huevos 3 minutos.')
    expect(screen.getByText(/Paso 1: Dice dos tiempos \(2 minutos y 3 minutos\)/)).toBeInTheDocument()
    await usuario.click(screen.getByRole('button', { name: 'Guardar receta' }))

    expect(screen.getByText(/Para el almuerzo le falta algo que llene/)).toBeInTheDocument()
    expect(llamadas.some((l) => l.metodo === 'POST')).toBe(false)

    // Solo para desayuno y con un tiempo por paso, ya se puede guardar.
    await usuario.click(screen.getByRole('checkbox', { name: 'Almuerzo' }))
    expect(screen.getByText('Marca al menos una comida.')).toBeInTheDocument()
    await usuario.click(screen.getByRole('checkbox', { name: 'Desayuno' }))
    expect(screen.queryByText(/Para el almuerzo le falta/)).not.toBeInTheDocument()
  })

  it('edita una receta y la puede quitar del recetario', async () => {
    const { llamadas } = backend({
      'PUT /curaduria/recetas/40': (cuerpo: unknown) => respuestaJson({ ...SUDADO, ...(cuerpo as object) }),
      'DELETE /curaduria/recetas/40': () => respuestaJson({ ...SUDADO, activa: false }),
    })
    montarApp(crearServicioFalso(CURADORA).servicio, '/curaduria/recetas/40')
    const usuario = userEvent.setup()

    expect(await screen.findByLabelText('Nombre')).toHaveValue('Sudado de pollo')
    expect(screen.getByLabelText('Pasos')).toHaveValue('Pica el tomate.\nCocina el pollo 20 minutos.')
    expect(screen.getByRole('checkbox', { name: 'Cena' })).toBeChecked()
    await usuario.clear(screen.getByLabelText('Tiempo total'))
    await usuario.type(screen.getByLabelText('Tiempo total'), '55')
    await usuario.click(screen.getByRole('button', { name: 'Guardar receta' }))
    expect(await screen.findByText(/quedó guardada/)).toBeInTheDocument()
    expect(llamadas.find((l) => l.metodo === 'PUT')?.cuerpo).toMatchObject({ tiempoMinutos: 55 })

    await usuario.click(await screen.findByRole('link', { name: /Sudado de pollo/ }))
    await usuario.click(await screen.findByRole('button', { name: 'Quitar del recetario' }))
    await usuario.click(screen.getByRole('button', { name: 'Sí, quitarla' }))
    expect(await screen.findByText(/ya no se ofrece/)).toBeInTheDocument()
  })
})
