import { useCallback, useEffect, useRef, useState } from 'react'

/**
 * El asistente que habla (Web Speech API, `speechSynthesis`): viene en el navegador del
 * celular y no cuesta nada. Si el navegador no la tiene, el Modo cocinar funciona igual,
 * solo que en silencio.
 */

/** Orden de preferencia: primero una voz colombiana. */
const IDIOMAS = ['es-CO', 'es-MX', 'es-US', 'es-419', 'es-ES']

export function elegirVoz(voces: SpeechSynthesisVoice[]): SpeechSynthesisVoice | undefined {
  const enEspanol = voces.filter((v) => v.lang.toLowerCase().startsWith('es'))
  for (const idioma of IDIOMAS) {
    const exactas = enEspanol.filter((v) => v.lang.toLowerCase() === idioma.toLowerCase())
    // Entre las del mismo idioma, las "naturales" o de Google suenan menos robóticas.
    const buena = exactas.find((v) => /natural|google|online/i.test(v.name)) ?? exactas[0]
    if (buena) return buena
  }
  return enEspanol[0]
}

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
}

const CLAVE = 'cocina-sin-estres:voz'

function leerPreferencia(): boolean {
  try {
    return localStorage.getItem(CLAVE) !== 'apagada'
  } catch {
    return true
  }
}

export function useVoz(): Voz {
  const sintesis = typeof window !== 'undefined' && 'speechSynthesis' in window ? window.speechSynthesis : undefined
  const [encendida, setEncendida] = useState(leerPreferencia)
  const [hablando, setHablando] = useState(false)
  const voz = useRef<SpeechSynthesisVoice | undefined>(undefined)

  // Las voces cargan después de abrir la página en Chrome: se eligen cuando llegan.
  useEffect(() => {
    if (!sintesis) return
    const cargar = () => {
      voz.current = elegirVoz(sintesis.getVoices())
    }
    cargar()
    sintesis.addEventListener?.('voiceschanged', cargar)
    return () => {
      sintesis.removeEventListener?.('voiceschanged', cargar)
      sintesis.cancel()
    }
  }, [sintesis])

  const callar = useCallback(() => {
    sintesis?.cancel()
    setHablando(false)
  }, [sintesis])

  const decir = useCallback(
    (texto: string, alTerminar?: () => void) => {
      if (!sintesis || !encendida) {
        alTerminar?.()
        return
      }
      sintesis.cancel()
      const frase = new SpeechSynthesisUtterance(texto)
      frase.lang = voz.current?.lang ?? 'es-CO'
      if (voz.current) frase.voice = voz.current
      // Un poco más pausada que lo normal: está cocinando, no leyendo.
      frase.rate = 0.92
      frase.pitch = 1.05
      frase.onstart = () => setHablando(true)
      frase.onend = () => {
        setHablando(false)
        alTerminar?.()
      }
      frase.onerror = () => {
        setHablando(false)
        alTerminar?.()
      }
      sintesis.speak(frase)
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
      if (!ahora) sintesis?.cancel()
      return ahora
    })
  }, [sintesis])

  return { disponible: Boolean(sintesis), encendida, alternar, hablando, decir, callar }
}
