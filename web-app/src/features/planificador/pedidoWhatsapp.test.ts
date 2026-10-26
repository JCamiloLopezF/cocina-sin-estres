import { describe, expect, it } from 'vitest'
import type { ProductoDisponible } from '../despensa/tipos'
import { cantidadParaPedir, enlaceWhatsapp, mensajePedido, plural } from './pedidoWhatsapp'

function producto(unidadBase: 'g' | 'ml' | 'unidad', unidadCompra: string, cantidadUnidadCompra: number): ProductoDisponible {
  return { id: 1, nombre: 'X', categoria: 'GRANOS', tipoCantidad: 'GRANEL', unidadBase, unidadCompra, cantidadUnidadCompra }
}

describe('Pedido a la tienda por WhatsApp (KAN-53)', () => {
  it('pluraliza las presentaciones', () => {
    expect(plural('bolsa', 2)).toBe('bolsas')
    expect(plural('unidad', 3)).toBe('unidades')
    expect(plural('libra', 1)).toBe('libra')
  })

  it('pide en las presentaciones que vende la tienda', () => {
    expect(cantidadParaPedir(300, producto('g', 'Libra', 500))).toBe('1 libra')
    expect(cantidadParaPedir(1200, producto('g', 'Libra', 500))).toBe('3 libras')
    expect(cantidadParaPedir(400, producto('g', 'Bolsa', 1000))).toBe('1 bolsa de 1 kg')
    expect(cantidadParaPedir(600, producto('g', 'Paquete', 500))).toBe('2 paquetes de 500 g')
    expect(cantidadParaPedir(2, producto('unidad', 'Lata', 1))).toBe('2 latas')
    // Los huevos vienen en cubeta de 30: se piden los que faltan.
    expect(cantidadParaPedir(6, producto('unidad', 'Cubeta', 30))).toBe('6 unidades')
    expect(cantidadParaPedir(250, undefined)).toBe('250 unidades')
  })

  it('arma un mensaje amable, de usted, con lo que falta', () => {
    const texto = mensajePedido(
      [
        { producto: 'Pollo', cantidad: '1 libra' },
        { producto: 'Arroz', cantidad: '1 bolsa de 1 kg' },
      ],
      'Tienda de don Jorge',
      'Luisa',
    )
    expect(texto).toBe(
      [
        'Hola, Tienda de don Jorge. Le quiero hacer este pedido:',
        '',
        '- Pollo: 1 libra',
        '- Arroz: 1 bolsa de 1 kg',
        '',
        '¿Me confirma el valor y si me lo pueden traer? Muchas gracias. Luisa',
      ].join('\n'),
    )
    expect(mensajePedido([{ producto: 'Sal', cantidad: '1 bolsa de 500 g' }])).toMatch(/^Hola\. Le quiero/)
  })

  it('el enlace abre WhatsApp con el número y el mensaje', () => {
    expect(enlaceWhatsapp('Hola, ¿tiene pan?', '573001234567')).toBe(
      'https://wa.me/573001234567?text=Hola%2C%20%C2%BFtiene%20pan%3F',
    )
    expect(enlaceWhatsapp('Hola')).toBe('https://wa.me/?text=Hola')
  })
})
