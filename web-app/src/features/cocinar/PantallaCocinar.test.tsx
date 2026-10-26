import { act, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { backendFalso } from '../../prueba/backendFalso'
import { rutasCocina, SOPA } from '../../prueba/cocinaFalsa'
import { montarApp } from '../../prueba/montar'
import { crearServicioFalso, LUISA, respuestaJson } from '../../prueba/servicioFalso'
import type { RecetaDetalle } from '../recetario/tipos'

const ARROZ_BLANCO: RecetaDetalle = {
  id: 12,
  nombre: 'Arroz blanco',
  tiempoMinutos: 25,
  porciones: 4,
  pasos: ['Sofríe el ajo.', 'Cocina tapado 20 minutos.'],
  ingredientes: [{ productoId: 1, cantidad: 250, opcional: false }],
}

/**
 * `speechSynthesis` de mentiras: guarda lo que dijo el asistente. El asistente habla
 * frase por frase; las frases de un mismo mensaje se unen en una sola entrada (la última
 * frase de cada mensaje es la que trae `onend`), así `dichas.at(-1)` es el último mensaje
 * completo.
 */
function vozFalsa() {
  const dichas: string[] = []
  let mensajeAbierto = false
  class Frase {
    lang = ''
    voice = null
    rate = 1
    pitch = 1
    volume = 1
    onstart = null
    onend: (() => void) | null = null
    onerror = null
    text: string
    constructor(text: string) {
      this.text = text
    }
  }
  function hablar(frase: Frase) {
    // La frase en silencio con la que se "despierta" la voz en Safari no cuenta.
    if (!frase.text.trim()) return
    if (mensajeAbierto) dichas[dichas.length - 1] += ` ${frase.text}`
    else dichas.push(frase.text)
    mensajeAbierto = frase.onend === null
  }
  vi.stubGlobal('SpeechSynthesisUtterance', Frase)
  vi.stubGlobal('speechSynthesis', {
    speak: vi.fn(hablar),
    cancel: vi.fn(),
    getVoices: () => [],
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
  })
  return dichas
}

/** Reconocimiento de voz de mentiras: la prueba "dice" frases con `oir`. */
function oidoFalso() {
  const activos: { onresult: ((e: unknown) => void) | null; onstart: (() => void) | null }[] = []
  class Reconocedor {
    lang = ''
    continuous = false
    interimResults = false
    onresult: ((e: unknown) => void) | null = null
    onstart: (() => void) | null = null
    onend = null
    onerror = null
    start() {
      activos.push(this)
      this.onstart?.()
    }
    stop() {}
    abort() {
      activos.splice(activos.indexOf(this), 1)
    }
  }
  vi.stubGlobal('webkitSpeechRecognition', Reconocedor)
  return {
    oir(texto: string) {
      act(() => activos.at(-1)?.onresult?.({ resultIndex: 0, results: [{ isFinal: true, 0: { transcript: texto } }] }))
    },
  }
}

const yaVioLaIntro = () => localStorage.setItem('cocina-sin-estres:intro-cocina-vista', 'si')

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('Modo cocinar (pedido del profesor: la receta como tareas, con voz)', () => {
  it('la primera vez presenta los comandos y después guía paso a paso hasta descontar la despensa', async () => {
    const dichas = vozFalsa()
    const { llamadas } = backendFalso({
      ...rutasCocina(),
      'POST /preparaciones': () => new Response(null, { status: 204 }),
    })
    montarApp(crearServicioFalso(LUISA).servicio, '/recetas/10/cocinar?porciones=4')
    const usuario = userEvent.setup()

    expect(await screen.findByRole('heading', { name: 'Te acompaño paso a paso' })).toBeInTheDocument()
    expect(dichas.at(-1)).toMatch(/Te acompaño paso a paso/)
    await usuario.click(screen.getByRole('button', { name: 'Siguiente' }))
    await usuario.click(screen.getByRole('button', { name: 'Siguiente' }))
    expect(screen.getByText(/«siguiente», «repite», «atrás» o «temporizador»/)).toBeInTheDocument()
    await usuario.click(screen.getByRole('button', { name: 'Siguiente' }))
    expect(screen.getByRole('button', { name: '🔊 Voz encendida' })).toBeInTheDocument()
    await usuario.click(screen.getByRole('button', { name: 'Empezar a cocinar' }))
    expect(localStorage.getItem('cocina-sin-estres:intro-cocina-vista')).toBe('si')

    expect(screen.getByRole('heading', { name: 'Alistemos los ingredientes' })).toBeInTheDocument()
    expect(dichas.at(-1)).toMatch(/^¡Hola, Luisa! Vamos a preparar Sopa de lentejas\..*Remoja la lenteja/)
    await usuario.click(screen.getByRole('checkbox', { name: /Lenteja · 250 g/ }))
    expect(screen.getByRole('checkbox', { name: /Lenteja · 250 g/ })).toBeChecked()

    await usuario.click(screen.getByRole('button', { name: 'Empezar a cocinar' }))
    expect(screen.getByText('Paso 1 de 2')).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Sofríe la cebolla.' })).toBeInTheDocument()
    expect(dichas.at(-1)).toMatch(/^¡Listo! Empecemos\. Paso 1 de 2/)

    await usuario.click(screen.getByRole('button', { name: 'Listo' }))
    expect(screen.getByRole('heading', { name: 'Agrega la lenteja y el agua.' })).toBeInTheDocument()
    expect(dichas.at(-1)).toMatch(/Último paso/)
    await usuario.click(screen.getByRole('button', { name: 'Atrás' }))
    expect(screen.getByRole('heading', { name: 'Sofríe la cebolla.' })).toBeInTheDocument()
    await usuario.click(screen.getByRole('button', { name: 'Listo' }))
    await usuario.click(screen.getByRole('button', { name: 'Listo' }))

    expect(screen.getByRole('heading', { name: '¡Quedó listo, Luisa! Buen provecho' })).toBeInTheDocument()
    expect(screen.getByRole('progressbar', { name: 'Avance de la receta' })).toHaveAttribute('aria-valuenow', '3')
    await usuario.click(screen.getByRole('button', { name: 'La preparé' }))
    expect(await screen.findByText('Descontamos los ingredientes de tu despensa.')).toBeInTheDocument()
    expect(llamadas.find((l) => l.metodo === 'POST' && l.ruta === '/preparaciones')?.cuerpo).toEqual({ recetaId: 10 })
    expect(screen.getByRole('link', { name: 'Volver al recetario' })).toHaveAttribute('href', '/recetas')
  })

  it('desde el plan sigue donde iba, cuenta el tiempo del paso y marca la comida como preparada', async () => {
    const dichas = vozFalsa()
    yaVioLaIntro()
    localStorage.setItem(
      'cocina-sin-estres:cocinando:12',
      JSON.stringify({ etapa: 1, alistados: [1], en: Date.now() - 60_000 }),
    )
    const { llamadas } = backendFalso({
      ...rutasCocina(),
      'GET /recetas/12': () => respuestaJson(ARROZ_BLANCO),
      'PUT /planes-semanales/7/comidas/70/marcar-preparada': () =>
        respuestaJson({ id: 70, planSemanalId: 7, dia: 1, tipoComida: 'ALMUERZO', recetaId: 12, preparada: true }),
    })
    montarApp(crearServicioFalso(LUISA).servicio, '/recetas/12/cocinar?plan=7&comida=70')
    const usuario = userEvent.setup()

    expect(await screen.findByRole('heading', { name: 'Cocina tapado 20 minutos.' })).toBeInTheDocument()
    expect(dichas.at(-1)).toMatch(/^Seguimos donde ibas\. Último paso\. Paso 2 de 2/)

    await usuario.click(screen.getByRole('button', { name: /Contar 20 minutos/ }))
    const temporizador = screen.getByRole('region', { name: 'Temporizador' })
    expect(within(temporizador).getByText('20:00')).toBeInTheDocument()
    expect(dichas.at(-1)).toMatch(/cuento 20 minutos/)
    await usuario.click(within(temporizador).getByRole('button', { name: 'Pausar' }))
    expect(within(temporizador).getByRole('button', { name: 'Seguir' })).toBeInTheDocument()
    await usuario.click(within(temporizador).getByRole('button', { name: 'Cancelar' }))
    expect(screen.queryByRole('region', { name: 'Temporizador' })).not.toBeInTheDocument()

    // Silenciar: el asistente deja de hablar, pero la pantalla sigue igual.
    await usuario.click(screen.getByRole('button', { name: 'Silenciar el asistente' }))
    const antes = dichas.length
    await usuario.click(screen.getByRole('button', { name: 'Listo' }))
    expect(dichas).toHaveLength(antes)
    expect(screen.getByRole('button', { name: 'Encender la voz del asistente' })).toHaveAttribute('aria-pressed', 'false')

    await usuario.click(screen.getByRole('button', { name: 'La preparé' }))
    expect(await screen.findByRole('link', { name: 'Volver a mi semana' })).toHaveAttribute('href', '/plan')
    expect(llamadas.some((l) => l.metodo === 'PUT' && l.ruta === '/planes-semanales/7/comidas/70/marcar-preparada')).toBe(true)
    expect(llamadas.some((l) => l.ruta === '/preparaciones')).toBe(false)
    expect(localStorage.getItem('cocina-sin-estres:cocinando:12')).toBeNull()
  })

  it('con manos libres entiende «siguiente», «atrás» y «temporizador»', async () => {
    yaVioLaIntro()
    localStorage.setItem('cocina-sin-estres:manos-libres', 'si')
    const { oir } = oidoFalso()
    backendFalso({ ...rutasCocina(), 'GET /recetas/12': () => respuestaJson(ARROZ_BLANCO) })
    montarApp(crearServicioFalso(LUISA).servicio, '/recetas/12/cocinar')

    expect(await screen.findByRole('heading', { name: 'Alistemos los ingredientes' })).toBeInTheDocument()
    expect(await screen.findByText(/Te escucho/)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Apagar manos libres' })).toHaveAttribute('aria-pressed', 'true')

    oir('ya listo, siguiente')
    expect(screen.getByRole('heading', { name: 'Sofríe el ajo.' })).toBeInTheDocument()
    oir('siguiente')
    expect(screen.getByRole('heading', { name: 'Cocina tapado 20 minutos.' })).toBeInTheDocument()
    oir('pon el temporizador')
    expect(screen.getByRole('region', { name: 'Temporizador' })).toBeInTheDocument()
    oir('atrás')
    expect(screen.getByRole('heading', { name: 'Sofríe el ajo.' })).toBeInTheDocument()
    oir('qué rico huele')
    expect(screen.getByRole('heading', { name: 'Sofríe el ajo.' })).toBeInTheDocument()
  })

  it('sin voz en el navegador se cocina igual, leyendo', async () => {
    yaVioLaIntro()
    backendFalso(rutasCocina())
    montarApp(crearServicioFalso(LUISA).servicio, `/recetas/${SOPA.id}/cocinar`)

    expect(await screen.findByRole('heading', { name: 'Alistemos los ingredientes' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /asistente/ })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /manos libres/ })).not.toBeInTheDocument()
    const usuario = userEvent.setup()
    await usuario.click(screen.getByRole('button', { name: 'Ver la introducción' }))
    for (let i = 0; i < 3; i++) await usuario.click(screen.getByRole('button', { name: 'Siguiente' }))
    expect(screen.getByText(/Este celular no puede hablar/)).toBeInTheDocument()
    await usuario.click(screen.getByRole('button', { name: 'Empezar a cocinar' }))
    expect(screen.getByRole('heading', { name: 'Alistemos los ingredientes' })).toBeInTheDocument()
  })
})
