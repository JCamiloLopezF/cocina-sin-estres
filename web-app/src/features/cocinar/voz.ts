import { useCallback, useEffect, useRef, useState } from 'react'

/**
 * El asistente que habla (Web Speech API, `speechSynthesis`): viene en el navegador del
 * celular y no cuesta nada. Si el navegador no la tiene, el Modo cocinar funciona igual,
 * solo que en silencio.
 *
 * Para que suene lo menos robótico posible:
 * - se elige la voz más natural que tenga el dispositivo (las "Natural", "Mejorada",
 *   "Premium" o de Google), antes que fijarse solo en el país;
 * - se habla frase por frase, para que haga las pausas de la puntuación (y porque Chrome
 *   corta las frases largas a los 15 segundos);
 * - velocidad un poco más pausada y tono normal.
 *
 * Safari solo deja hablar si la primera frase sale justo de un toque de la usuaria, y
 * pierde la frase si se corta la anterior y se habla en el mismo instante. Por eso la voz
 * se "despierta" con el primer toque en cualquier parte de la app y, si hay que cortar,
 * se espera un momento antes de hablar.
 */

/** Más pausada que lo normal: está cocinando, no leyendo. */
export const VELOCIDAD = 0.95
/** Tono natural: subirlo es lo que más la hace sonar a robot. */
export const TONO = 1

/** Voces de Apple de efectos o de lectura de pantalla: suenan muy artificiales. */
const VOCES_ARTIFICIALES =
  /\b(eddy|flo|grandma|grandpa|reed|rocko|sandy|shelley|albert|bad news|bahh|bells|boing|bubbles|cellos|good news|jester|organ|superstar|trinoids|whisper|wobble|zarvox|eloquence)\b/i

/** Señales de una voz de buena calidad (neuronal, mejorada o en línea). */
const VOCES_NATURALES = /natural|neural|premium|enhanced|mejorada|online|google|siri/i

/** Preferencia por variante de español: primero la colombiana y las latinoamericanas. */
const REGIONES: Record<string, number> = {
  'es-co': 6,
  'es-419': 5,
  'es-us': 5,
  'es-mx': 4,
  'es-ve': 4,
  'es-pe': 4,
  'es-ec': 4,
  'es-cl': 3,
  'es-ar': 3,
  'es-es': 2,
}

/**
 * Qué tan buena es una voz para el asistente. La calidad pesa más que el país: una voz
 * natural de España suena mejor que una básica de México.
 */
export function puntajeVoz(voz: Pick<SpeechSynthesisVoice, 'name' | 'lang' | 'voiceURI' | 'localService'>): number {
  const idioma = voz.lang.toLowerCase().replace('_', '-')
  if (!idioma.startsWith('es')) return -1
  const texto = `${voz.name} ${voz.voiceURI ?? ''}`
  if (VOCES_ARTIFICIALES.test(texto)) return 0
  let puntaje = 1 + (REGIONES[idioma] ?? 1)
  if (VOCES_NATURALES.test(texto)) puntaje += 20
  // Las voces en línea (localService = false) suelen ser las neuronales del proveedor.
  if (voz.localService === false) puntaje += 3
  if (/compact/i.test(texto)) puntaje -= 2
  return puntaje
}

export function elegirVoz(voces: SpeechSynthesisVoice[]): SpeechSynthesisVoice | undefined {
  let mejor: SpeechSynthesisVoice | undefined
  let mejorPuntaje = 0
  for (const voz of voces) {
    const puntaje = puntajeVoz(voz)
    if (puntaje > mejorPuntaje) {
      mejor = voz
      mejorPuntaje = puntaje
    }
  }
  return mejor
}

/** La voz elegida suena natural (no es una de las básicas del dispositivo). */
export function esVozNatural(voz?: Pick<SpeechSynthesisVoice, 'name' | 'voiceURI'>): boolean {
  return Boolean(voz && VOCES_NATURALES.test(`${voz.name} ${voz.voiceURI ?? ''}`))
}

/** "Hola. ¿Cómo estás?" → ["Hola.", "¿Cómo estás?"]: cada frase con su pausa. */
export function partirEnFrases(texto: string): string[] {
  return texto
    .split(/(?<=[.!?…:])\s+/)
    .map((f) => f.trim())
    .filter(Boolean)
}

// --- Safari: despertar la voz con el primer toque --------------------------------------

let despierta = false
let pendiente: (() => void) | undefined

function sintesisDisponible(): SpeechSynthesis | undefined {
  return typeof window !== 'undefined' && 'speechSynthesis' in window ? window.speechSynthesis : undefined
}

/** Todavía no ha habido ningún toque en la página: el navegador no dejaría hablar. */
function faltaUnToque(): boolean {
  if (despierta || typeof navigator === 'undefined') return false
  const activacion = (navigator as Navigator & { userActivation?: { hasBeenActive: boolean } }).userActivation
  return activacion !== undefined && !activacion.hasBeenActive
}

function despertar() {
  const sintesis = sintesisDisponible()
  if (despierta || !sintesis) return
  despierta = true
  for (const evento of EVENTOS_TOQUE) document.removeEventListener(evento, despertar, true)
  if (pendiente) {
    // Lo que se quiso decir antes del primer toque se dice ahora, dentro del toque.
    const decirAhora = pendiente
    pendiente = undefined
    decirAhora()
    return
  }
  // Una frase vacía y en silencio basta para que Safari deje hablar después.
  const silencio = new SpeechSynthesisUtterance(' ')
  silencio.volume = 0
  sintesis.speak(silencio)
}

const EVENTOS_TOQUE = ['click', 'touchend', 'keydown'] as const
if (typeof document !== 'undefined') {
  for (const evento of EVENTOS_TOQUE) document.addEventListener(evento, despertar, { capture: true, passive: true })
}

// --- Hook -------------------------------------------------------------------------------

export interface Voz {
  /** El navegador puede hablar. */
  disponible: boolean
  /** La usuaria la tiene encendida (🔊). */
  encendida: boolean
  alternar: () => void
  hablando: boolean
  /** Dice el texto si la voz está encendida; corta lo que estuviera diciendo. */
  decir: (texto: string, alTerminar?: () => void) => void
  callar: () => void
  /** Nombre de la voz elegida, para mostrarlo en la introducción. */
  nombreVoz?: string
  /** La voz elegida es de las naturales; si no, se sugiere descargar una mejor. */
  vozNatural: boolean
}

const CLAVE = 'cocina-sin-estres:voz'
/** Lo que espera Safari entre cortar una frase y empezar otra para no perderla. */
const PAUSA_TRAS_CORTAR_MS = 80

function leerPreferencia(): boolean {
  try {
    return localStorage.getItem(CLAVE) !== 'apagada'
  } catch {
    return true
  }
}

export function useVoz(): Voz {
  const sintesis = sintesisDisponible()
  const [encendida, setEncendida] = useState(leerPreferencia)
  const [hablando, setHablando] = useState(false)
  const [elegida, setElegida] = useState<SpeechSynthesisVoice>()
  const voz = useRef<SpeechSynthesisVoice | undefined>(undefined)
  const espera = useRef<number | undefined>(undefined)

  // Las voces cargan después de abrir la página en Chrome y Safari: se eligen cuando llegan.
  useEffect(() => {
    if (!sintesis) return
    const cargar = () => {
      voz.current = elegirVoz(sintesis.getVoices())
      setElegida(voz.current)
    }
    cargar()
    sintesis.addEventListener?.('voiceschanged', cargar)
    return () => {
      sintesis.removeEventListener?.('voiceschanged', cargar)
      window.clearTimeout(espera.current)
      pendiente = undefined
      sintesis.cancel()
    }
  }, [sintesis])

  const callar = useCallback(() => {
    window.clearTimeout(espera.current)
    pendiente = undefined
    sintesis?.cancel()
    setHablando(false)
  }, [sintesis])

  const decir = useCallback(
    (texto: string, alTerminar?: () => void) => {
      if (!sintesis || !encendida) {
        alTerminar?.()
        return
      }
      const frases = partirEnFrases(texto)
      if (frases.length === 0) {
        alTerminar?.()
        return
      }
      let terminado = false
      const terminar = () => {
        if (terminado) return
        terminado = true
        setHablando(false)
        alTerminar?.()
      }
      const hablar = () => {
        frases.forEach((frase, i) => {
          const enunciado = new SpeechSynthesisUtterance(frase)
          enunciado.lang = voz.current?.lang ?? 'es-CO'
          if (voz.current) enunciado.voice = voz.current
          enunciado.rate = VELOCIDAD
          enunciado.pitch = TONO
          if (i === 0) enunciado.onstart = () => setHablando(true)
          if (i === frases.length - 1) enunciado.onend = terminar
          enunciado.onerror = terminar
          sintesis.speak(enunciado)
        })
      }

      window.clearTimeout(espera.current)
      if (faltaUnToque()) {
        // Sin un toque el navegador no deja hablar: se dice con el primer toque.
        pendiente = hablar
        return
      }
      if (sintesis.speaking || sintesis.pending) {
        sintesis.cancel()
        espera.current = window.setTimeout(hablar, PAUSA_TRAS_CORTAR_MS)
      } else {
        hablar()
      }
    },
    [sintesis, encendida],
  )

  const alternar = useCallback(() => {
    setEncendida((antes) => {
      const ahora = !antes
      try {
        localStorage.setItem(CLAVE, ahora ? 'encendida' : 'apagada')
      } catch {
        // Sin almacenamiento: la preferencia dura mientras la pantalla esté abierta.
      }
      if (!ahora) {
        window.clearTimeout(espera.current)
        pendiente = undefined
        sintesis?.cancel()
      }
      return ahora
    })
  }, [sintesis])

  return {
    disponible: Boolean(sintesis),
    encendida,
    alternar,
    hablando,
    decir,
    callar,
    nombreVoz: elegida?.name,
    vozNatural: esVozNatural(elegida),
  }
}
