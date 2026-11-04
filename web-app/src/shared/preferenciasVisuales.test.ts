import { beforeEach, describe, expect, it } from 'vitest'
import { aplicarPreferencias, guardarPreferencias, leerPreferencias } from './preferenciasVisuales'

describe('Tamaño de letra y alto contraste', () => {
  beforeEach(() => {
    localStorage.clear()
    aplicarPreferencias({ letra: 'normal', altoContraste: false })
  })

  it('por defecto, letra normal y sin alto contraste', () => {
    expect(leerPreferencias()).toEqual({ letra: 'normal', altoContraste: false })
    expect(document.documentElement.dataset.letra).toBeUndefined()
  })

  it('guarda la elección y la aplica en <html>', () => {
    guardarPreferencias({ letra: 'muy-grande', altoContraste: true })

    expect(leerPreferencias()).toEqual({ letra: 'muy-grande', altoContraste: true })
    expect(document.documentElement.dataset.letra).toBe('muy-grande')
    expect(document.documentElement.dataset.contraste).toBe('alto')
  })

  it('ignora un valor guardado que no es válido', () => {
    localStorage.setItem('cocina.preferencias-visuales', '{"letra":"gigante"}')
    expect(leerPreferencias().letra).toBe('normal')
  })
})
