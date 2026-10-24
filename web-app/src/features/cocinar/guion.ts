/**
 * Lo que el Modo cocinar sabe hacer sin pantalla: detectar el tiempo de un paso,
 * entender los comandos de voz y armar las frases del asistente. Son funciones puras
 * para probarlas sin navegador.
 */

export type Comando = 'siguiente' | 'repite' | 'atras' | 'temporizador'

const PALABRAS: Record<Comando, string[]> = {
  siguiente: ['siguiente', 'listo', 'lista', 'ya está', 'ya esta', 'sigue', 'continúa', 'continua', 'adelante', 'hecho'],
  repite: ['repite', 'repítelo', 'repitelo', 'repetir', 'otra vez', 'de nuevo', 'qué dijiste', 'que dijiste'],
  atras: ['atrás', 'atras', 'anterior', 'devuélvete', 'devuelvete', 'regresa', 'volver'],
  temporizador: ['temporizador', 'cronómetro', 'cronometro', 'alarma', 'cuenta el tiempo', 'pon el tiempo'],
}

/** Saca el comando de lo que dijo la usuaria. Lo que no se entiende se ignora. */
export function comandoDe(transcripcion: string): Comando | undefined {
  const texto = ` ${transcripcion.toLowerCase().trim()} `
  // El último comando dicho gana: "atrás, no, siguiente" → siguiente.
  let elegido: Comando | undefined
  let posicion = -1
  for (const [comando, palabras] of Object.entries(PALABRAS) as [Comando, string[]][]) {
    for (const palabra of palabras) {
      const en = texto.lastIndexOf(palabra)
      if (en > posicion) {
        posicion = en
        elegido = comando
      }
    }
  }
  return elegido
}

const NUMEROS: Record<string, number> = {
  un: 1, una: 1, uno: 1, dos: 2, tres: 3, cuatro: 4, cinco: 5, seis: 6, siete: 7, ocho: 8, nueve: 9, diez: 10,
  quince: 15, veinte: 20, treinta: 30, cuarenta: 40, media: 0.5,
}

/**
 * Minutos que pide un paso: "cocina 20 minutos" → 20; "de 15 a 20 minutos" → 20;
 * "una hora" → 60; "media hora" → 30. `undefined` si el paso no dice un tiempo.
 */
export function minutosDelPaso(paso: string): number | undefined {
  const texto = paso.toLowerCase()
  const horas = texto.match(/(\d+(?:[.,]\d+)?|un|una|media|dos|tres)\s+horas?/)
  if (horas) {
    const valor = NUMEROS[horas[1]] ?? Number(horas[1].replace(',', '.'))
    if (valor > 0) return Math.round(valor * 60)
  }
  const minutos = texto.match(/(?:(\d+)\s*(?:a|-|o)\s*)?(\d+|un|uno|dos|tres|cuatro|cinco|seis|siete|ocho|nueve|diez|quince|veinte|treinta|cuarenta)\s*(?:minutos?|min\b)/)
  if (minutos) {
    const valor = NUMEROS[minutos[2]] ?? Number(minutos[2])
    if (valor > 0) return valor
  }
  return undefined
}

/** "Luisa María" → "Luisa"; "luisa" → "Luisa". */
export function primerNombre(nombre?: string | null): string | undefined {
  const primero = nombre?.trim().split(/\s+/)[0]
  if (!primero || primero.includes('@')) return undefined
  return primero.charAt(0).toUpperCase() + primero.slice(1)
}

const ANIMOS = ['¡Muy bien!', '¡Vas muy bien!', '¡Perfecto!', '¡Eso es!', '¡Excelente!']

/** Frase cálida para pasar al siguiente paso; varía para no sonar repetida. */
export function animo(indice: number, nombre?: string): string {
  const frase = ANIMOS[indice % ANIMOS.length]
  return nombre && indice % 3 === 0 ? frase.replace('!', `, ${nombre}!`) : frase
}

/** Lo que dice el asistente al llegar a un paso. */
export function fraseDelPaso(numero: number, total: number, texto: string, minutos?: number): string {
  const partes = [`Paso ${numero} de ${total}.`, texto]
  if (numero === total && total > 1) partes.unshift('Último paso.')
  else if (numero === total - 1 && total > 2) partes.unshift('Ya casi terminamos.')
  if (minutos) {
    partes.push(`Cuando quieras, toca el botón del temporizador y cuento ${minutosEnPalabras(minutos)} por ti.`)
  }
  return partes.join(' ')
}

/** 1 → "1 minuto"; 90 → "1 hora y 30 minutos". */
export function minutosEnPalabras(minutos: number): string {
  const h = Math.floor(minutos / 60)
  const m = minutos % 60
  const enMin = m === 1 ? '1 minuto' : `${m} minutos`
  if (h === 0) return enMin
  const enHoras = h === 1 ? '1 hora' : `${h} horas`
  return m === 0 ? enHoras : `${enHoras} y ${enMin}`
}

/** "05:30" para mostrar en el temporizador. */
export function relojDe(segundos: number): string {
  const s = Math.max(0, Math.ceil(segundos))
  const h = Math.floor(s / 3600)
  const m = Math.floor((s % 3600) / 60)
  const r = s % 60
  const mm = String(m).padStart(2, '0')
  const ss = String(r).padStart(2, '0')
  return h > 0 ? `${h}:${mm}:${ss}` : `${mm}:${ss}`
}
