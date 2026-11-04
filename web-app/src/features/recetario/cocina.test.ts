import { describe, expect, it } from 'vitest'
import { ARROZ_CON_POLLO, CATALOGO } from '../../prueba/cocinaFalsa'
import { recordarIngredientes } from '../perfil/restricciones'
import { analizarReceta, avisoAntesDeCocinar, type Cocina } from './cocina'

function cocina(personas: Cocina['personas'], vencidos: number[] = []): Cocina {
  return {
    productos: new Map(CATALOGO.map((p) => [p.id, p])),
    existencias: new Map(),
    vencidos: new Set(vencidos),
    personas,
  }
}

describe('Recetario: alergias a un ingrediente y avisos antes de cocinar', () => {
  it('una alergia a un ingrediente que no está en la lista también protege (auditoría del 4 de noviembre)', () => {
    recordarIngredientes(CATALOGO)
    const vista = analizarReceta(
      ARROZ_CON_POLLO,
      cocina([
        { nombre: 'Samuel', restricciones: ['producto:5'], alergiasGraves: ['producto:5'] },
        { nombre: 'Ana', restricciones: ['producto:4'], alergiasGraves: [] },
      ]),
    )

    expect(vista.noAptaPara).toEqual([{ persona: 'Samuel', restricciones: ['Pollo'] }])
    expect(vista.ajustePara).toEqual([{ persona: 'Ana', restricciones: ['Arroz'] }])
  })

  it('distingue lo que falta de lo que está vencido', () => {
    const vista = analizarReceta(ARROZ_CON_POLLO, cocina([], [5]))

    expect(vista.vencidos).toEqual(['Pollo'])
    expect(vista.faltantes).toContain('Arroz')
    expect(avisoAntesDeCocinar(vista)).toMatch(/Lo que hay de pollo está vencido: no lo uses\./)
  })
})
