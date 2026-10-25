import { useEffect, useEffectEvent, useState } from 'react'
import { comandoDe, type Comando } from './guion'

/**
 * Manos libres: escucha "siguiente", "repite", "atrás" o "temporizador" con el
 * reconocimiento de voz del navegador (Chrome en Android). El audio lo procesa el
 * servicio de voz de Google, por eso está apagado por defecto y se avisa antes de
 * encenderlo. Mientras el asistente habla no escucha, para no oírse a sí mismo.
 */

interface ResultadoVoz {
  isFinal: boolean
  0: { transcript: string }
}

interface EventoVoz {
  resultIndex: number
  results: ArrayLike<ResultadoVoz>
}

interface Reconocedor {
  lang: string
  continuous: boolean
  interimResults: boolean
  onresult: ((evento: EventoVoz) => void) | null
  onstart: (() => void) | null
  onend: (() => void) | null
  onerror: ((evento: { error: string }) => void) | null
  start: () => void
  stop: () => void
  abort: () => void
}

type ConstructorReconocedor = new () => Reconocedor

function constructor(): ConstructorReconocedor | undefined {
  if (typeof window === 'undefined') return undefined
  const w = window as unknown as { SpeechRecognition?: ConstructorReconocedor; webkitSpeechRecognition?: ConstructorReconocedor }
  return w.SpeechRecognition ?? w.webkitSpeechRecognition
}

/** El navegador sabe escuchar. Si no, el botón 🎤 no se muestra. */
export function escuchaDisponible(): boolean {
  return constructor() !== undefined
}

export interface Escucha {
  escuchando: boolean
  /** Último error (por ejemplo, la usuaria no dio permiso al micrófono). */
  error?: 'permiso' | 'otro'
}

export function useEscucha(activa: boolean, pausada: boolean, alComando: (comando: Comando) => void): Escucha {
  const [escuchando, setEscuchando] = useState(false)
  const [error, setError] = useState<Escucha['error']>()
  const enComando = useEffectEvent(alComando)

  useEffect(() => {
    const Clase = constructor()
    if (!Clase || !activa || pausada) return
    let vivo = true
    const reconocedor = new Clase()
    reconocedor.lang = 'es-CO'
    reconocedor.continuous = true
    reconocedor.interimResults = false
    reconocedor.onresult = (evento) => {
      for (let i = evento.resultIndex; i < evento.results.length; i++) {
        const resultado = evento.results[i]
        if (!resultado.isFinal) continue
        const comando = comandoDe(resultado[0].transcript)
        if (comando) enComando(comando)
      }
    }
    reconocedor.onerror = (evento) => {
      if (evento.error === 'not-allowed' || evento.error === 'service-not-allowed') {
        vivo = false
        setError('permiso')
      } else if (evento.error !== 'no-speech' && evento.error !== 'aborted') {
        setError('otro')
      }
    }
    // Chrome corta la escucha cada tanto: se vuelve a empezar mientras siga encendida.
    reconocedor.onend = () => {
      if (vivo) {
        try {
          reconocedor.start()
        } catch {
          setEscuchando(false)
        }
      } else {
        setEscuchando(false)
      }
    }
    reconocedor.onstart = () => {
      setEscuchando(true)
      setError(undefined)
    }
    try {
      reconocedor.start()
    } catch {
      // Ya estaba escuchando: no hay nada que hacer.
    }
    return () => {
      vivo = false
      reconocedor.onend = null
      reconocedor.onstart = null
      reconocedor.abort()
    }
  }, [activa, pausada])

  // Apagada o en pausa (mientras habla el asistente) no está escuchando, sin importar el último evento.
  return { escuchando: escuchando && activa && !pausada, error }
}
