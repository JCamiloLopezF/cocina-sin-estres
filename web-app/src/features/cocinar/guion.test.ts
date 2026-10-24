import { describe, expect, it } from 'vitest'
import { animo, comandoDe, fraseDelPaso, minutosDelPaso, minutosEnPalabras, primerNombre, relojDe } from './guion'

describe('Modo cocinar: guion del asistente', () => {
  it('entiende los comandos aunque vengan dentro de una frase', () => {
    expect(comandoDe('Siguiente')).toBe('siguiente')
    expect(comandoDe('ya está, listo')).toBe('siguiente')
    expect(comandoDe('¿me lo repites otra vez?')).toBe('repite')
    expect(comandoDe('vuelve al paso anterior')).toBe('atras')
    expect(comandoDe('pon el temporizador')).toBe('temporizador')
    expect(comandoDe('atrás, no, mejor siguiente')).toBe('siguiente')
    expect(comandoDe('qué rico huele')).toBeUndefined()
  })

  it('detecta el tiempo que pide un paso', () => {
    expect(minutosDelPaso('Cuando hierva, baja el fuego, tapa y cocina 20 minutos sin destapar.')).toBe(20)
    expect(minutosDelPaso('Deja reposar tapado 5 minutos antes de servir.')).toBe(5)
    expect(minutosDelPaso('Cocina de 15 a 20 minutos.')).toBe(20)
    expect(minutosDelPaso('Hierve una hora a fuego bajo.')).toBe(60)
    expect(minutosDelPaso('Deja en remojo media hora.')).toBe(30)
    expect(minutosDelPaso('Revuelve un minuto para que se cubra.')).toBe(1)
    expect(minutosDelPaso('Pica la cebolla y el tomate.')).toBeUndefined()
  })

  it('habla cálido y dice en qué paso va', () => {
    expect(fraseDelPaso(1, 5, 'Pica la cebolla.')).toBe('Paso 1 de 5. Pica la cebolla.')
    expect(fraseDelPaso(4, 5, 'Agrega el agua.')).toBe('Ya casi terminamos. Paso 4 de 5. Agrega el agua.')
    expect(fraseDelPaso(5, 5, 'Cocina 20 minutos.', 20)).toBe(
      'Último paso. Paso 5 de 5. Cocina 20 minutos. Cuando quieras, toca el botón del temporizador y cuento 20 minutos por ti.',
    )
    expect(animo(0, 'Luisa')).toBe('¡Muy bien, Luisa!')
    expect(animo(1, 'Luisa')).toBe('¡Vas muy bien!')
  })

  it('da formato a nombres y tiempos', () => {
    expect(primerNombre('luisa maría')).toBe('Luisa')
    expect(primerNombre('luisa@correo.com')).toBeUndefined()
    expect(primerNombre(null)).toBeUndefined()
    expect(minutosEnPalabras(1)).toBe('1 minuto')
    expect(minutosEnPalabras(90)).toBe('1 hora y 30 minutos')
    expect(relojDe(330)).toBe('05:30')
    expect(relojDe(3725)).toBe('1:02:05')
  })
})
