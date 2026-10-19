import type { Dia, TipoComida } from './tipos'

/** Fechas y nombres de la semana del planificador. La semana va de lunes (1) a domingo (7). */

export const DIAS: Dia[] = [1, 2, 3, 4, 5, 6, 7]

export const NOMBRE_DIA: Record<Dia, string> = {
  1: 'Lunes',
  2: 'Martes',
  3: 'Miércoles',
  4: 'Jueves',
  5: 'Viernes',
  6: 'Sábado',
  7: 'Domingo',
}

export const INICIAL_DIA: Record<Dia, string> = { 1: 'L', 2: 'M', 3: 'M', 4: 'J', 5: 'V', 6: 'S', 7: 'D' }

export const COMIDAS: TipoComida[] = ['DESAYUNO', 'ALMUERZO', 'CENA']

export const NOMBRE_COMIDA: Record<TipoComida, string> = {
  DESAYUNO: 'Desayuno',
  ALMUERZO: 'Almuerzo',
  CENA: 'Cena',
}

/** AAAA-MM-DD en la hora local (no en UTC, para que el lunes no se corra de noche). */
export function aIso(fecha: Date): string {
  const mes = String(fecha.getMonth() + 1).padStart(2, '0')
  const dia = String(fecha.getDate()).padStart(2, '0')
  return `${fecha.getFullYear()}-${mes}-${dia}`
}

/** El día de la semana de una fecha: lunes = 1 … domingo = 7. */
export function diaDe(fecha: Date): Dia {
  return (((fecha.getDay() + 6) % 7) + 1) as Dia
}

/** El lunes de la semana de `fecha`. */
export function lunesDe(fecha: Date): Date {
  const lunes = new Date(fecha.getFullYear(), fecha.getMonth(), fecha.getDate())
  lunes.setDate(lunes.getDate() - (diaDe(fecha) - 1))
  return lunes
}

/** La fecha de un día de la semana que empieza en `lunesIso`. */
export function fechaDelDia(lunesIso: string, dia: number): Date {
  const [anio, mes, d] = lunesIso.split('-').map(Number)
  return new Date(anio, mes - 1, d + dia - 1)
}

const MESES = ['ene', 'feb', 'mar', 'abr', 'may', 'jun', 'jul', 'ago', 'sep', 'oct', 'nov', 'dic']

/** "14 – 20 oct" o "28 sep – 4 oct". */
export function rangoSemana(lunesIso: string): string {
  const inicio = fechaDelDia(lunesIso, 1)
  const fin = fechaDelDia(lunesIso, 7)
  const primero = inicio.getMonth() === fin.getMonth() ? `${inicio.getDate()}` : `${inicio.getDate()} ${MESES[inicio.getMonth()]}`
  return `${primero} – ${fin.getDate()} ${MESES[fin.getMonth()]}`
}

/** "Martes 15". */
export function tituloDia(lunesIso: string, dia: Dia): string {
  return `${NOMBRE_DIA[dia]} ${fechaDelDia(lunesIso, dia).getDate()}`
}

export function esDia(valor: unknown): valor is Dia {
  return typeof valor === 'number' && Number.isInteger(valor) && valor >= 1 && valor <= 7
}

export function esComida(valor: unknown): valor is TipoComida {
  return valor === 'DESAYUNO' || valor === 'ALMUERZO' || valor === 'CENA'
}

/** Lee `?dia=2&comida=CENA` (cuando la usuaria llega al recetario desde un día del plan). */
export function huecoDesde(parametros: URLSearchParams): { dia: Dia; comida: TipoComida } | undefined {
  const dia = Number(parametros.get('dia'))
  const comida = parametros.get('comida')
  return esDia(dia) && esComida(comida) ? { dia, comida } : undefined
}
