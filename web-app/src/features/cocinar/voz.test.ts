import { describe, expect, it } from 'vitest'
import { elegirVoz, esVozNatural, partirEnFrases, puntajeVoz } from './voz'

function voz(name: string, lang: string, voiceURI = name, localService = true) {
  return { name, lang, voiceURI, localService, default: false } as SpeechSynthesisVoice
}

describe('Voz del asistente', () => {
  it('prefiere una voz natural aunque sea de otro país antes que una básica', () => {
    const voces = [
      voz('Paulina', 'es-MX', 'com.apple.voice.compact.es-MX.Paulina'),
      voz('Mónica (mejorada)', 'es-ES', 'com.apple.voice.enhanced.es-ES.Monica'),
      voz('Samantha', 'en-US'),
    ]
    expect(elegirVoz(voces)?.name).toBe('Mónica (mejorada)')
  })

  it('entre naturales, gana la colombiana; y descarta las voces de efectos de Apple', () => {
    const voces = [
      voz('Grandma (español (México))', 'es-MX'),
      voz('Google español de Estados Unidos', 'es-US', 'Google español de Estados Unidos', false),
      voz('Microsoft Salome Online (Natural) - Spanish (Colombia)', 'es-CO', 'x', false),
    ]
    expect(elegirVoz(voces)?.name).toContain('Salome')
    expect(puntajeVoz(voces[0])).toBe(0)
    expect(elegirVoz([voz('Eddy (español (España))', 'es-ES')])).toBeUndefined()
  })

  it('sin voces naturales, prefiere el español latinoamericano', () => {
    expect(elegirVoz([voz('Jorge', 'es-ES'), voz('Paulina', 'es-MX')])?.name).toBe('Paulina')
    expect(elegirVoz([voz('Samantha', 'en-US')])).toBeUndefined()
  })

  it('reconoce si la voz suena natural', () => {
    expect(esVozNatural(voz('Paulina', 'es-MX', 'com.apple.voice.compact.es-MX.Paulina'))).toBe(false)
    expect(esVozNatural(voz('Paulina', 'es-MX', 'com.apple.voice.premium.es-MX.Paulina'))).toBe(true)
    expect(esVozNatural(undefined)).toBe(false)
  })

  it('habla frase por frase para hacer las pausas', () => {
    expect(partirEnFrases('¡Hola, Luisa! Vamos a preparar arroz. Paso 1 de 3: lava el arroz.')).toEqual([
      '¡Hola, Luisa!',
      'Vamos a preparar arroz.',
      'Paso 1 de 3:',
      'lava el arroz.',
    ])
    expect(partirEnFrases('   ')).toEqual([])
  })
})
