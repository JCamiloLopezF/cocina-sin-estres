/** Estado y alarma del temporizador de cada paso del Modo cocinar. */

export interface EstadoTemporizador {
  /** Minutos que se pidieron. */
  minutos: number
  /** Momento (ms) en que termina; si está en pausa, cuántos ms faltaban. */
  terminaEn: number
  pausado: boolean
  restantePausado: number
  terminado: boolean
}

export function nuevoTemporizador(minutos: number, ahora = Date.now()): EstadoTemporizador {
  return { minutos, terminaEn: ahora + minutos * 60_000, pausado: false, restantePausado: 0, terminado: false }
}

/** Tres pitidos con el audio del navegador y una vibración, para oírlo desde la cocina. */
export function sonarAlarma() {
  try {
    const Contexto =
      window.AudioContext ?? (window as unknown as { webkitAudioContext?: typeof AudioContext }).webkitAudioContext
    if (Contexto) {
      const audio = new Contexto()
      for (let i = 0; i < 3; i++) {
        const tono = audio.createOscillator()
        const volumen = audio.createGain()
        tono.frequency.value = 880
        volumen.gain.value = 0.2
        tono.connect(volumen).connect(audio.destination)
        tono.start(audio.currentTime + i * 0.45)
        tono.stop(audio.currentTime + i * 0.45 + 0.25)
      }
    }
    navigator.vibrate?.([300, 150, 300, 150, 300])
  } catch {
    // Sin audio: queda el aviso en pantalla y la voz.
  }
}
