import { describe, expect, it } from 'vitest'
import { cantidadConUnidad, describirVencimiento, detalleItem, leerPesos, nombreCategoria } from './formato'
import type { ItemDespensa } from './tipos'

const base: ItemDespensa = {
  id: 1,
  productoId: 1,
  nombre: 'Huevos',
  categoria: 'PROTEINAS',
  tipoCantidad: 'CONTABLE',
  unidadBase: 'unidad',
  cantidad: 6,
  esEstimada: false,
  estadoVencimiento: 'SIN_FECHA',
  bajoMinimo: false,
}

describe('formato de la despensa', () => {
  it.each([
    [1, 'unidad', '1 unidad'],
    [6, 'unidad', '6 unidades'],
    [500, 'g', '500 g'],
    [1500, 'g', '1,5 kg'],
    [2000, 'ml', '2 l'],
  ] as const)('%s %s → %s', (cantidad, unidad, esperado) => {
    expect(cantidadConUnidad(cantidad, unidad)).toBe(esperado)
  })

  it.each([
    [undefined, ''],
    [-2, 'venció hace 2 días'],
    [-1, 'venció ayer'],
    [0, 'vence hoy'],
    [1, 'vence mañana'],
    [6, 'vence en 6 días'],
  ])('vencimiento con %s días → "%s"', (dias, esperado) => {
    expect(describirVencimiento({ diasParaVencer: dias })).toBe(esperado)
  })

  it('arma la línea del listado con cantidad o nivel, vencimiento y mínimo', () => {
    expect(detalleItem({ ...base, diasParaVencer: 2, estadoVencimiento: 'POR_VENCER' })).toBe(
      '6 unidades · vence en 2 días',
    )
    expect(
      detalleItem({ ...base, tipoCantidad: 'GRANEL', unidadBase: 'g', nivel: 'POCO', bajoMinimo: true }),
    ).toBe('Nivel: poco · bajo el mínimo')
  })

  it('nombra las categorías del catálogo', () => {
    expect(nombreCategoria('GRANOS')).toBe('Arroz, pasta y granos')
    expect(nombreCategoria('BEBIDAS')).toBe('Café y chocolate')
    expect(nombreCategoria('SNACKS')).toBe('Snacks')
  })
})

describe('leerPesos (precio que escribe la usuaria)', () => {
  it('acepta puntos de miles y el signo de pesos', () => {
    expect(leerPesos('12.500')).toBe(12500)
    expect(leerPesos('$ 12.500')).toBe(12500)
    expect(leerPesos('9800')).toBe(9800)
    expect(leerPesos('0')).toBe(0)
  })

  it('sin números no hay precio', () => {
    expect(leerPesos('')).toBeUndefined()
    expect(leerPesos('mucho')).toBeUndefined()
  })
})
