import { fechaCorta } from '../despensa/formato'
import type { Analisis, Indicador } from './apiAnalisis'

/** Formato de los indicadores del análisis del administrador (KAN-54). */

export const numero = new Intl.NumberFormat('es-CO', { maximumFractionDigits: 1 })

/** "92 %", "2,4 min", "120 ms"; sin valor, una raya. */
export function valorIndicador(indicador: Pick<Indicador, 'valor' | 'unidad'>): string {
  if (indicador.valor === null) return '—'
  const unidad = indicador.unidad ?? ''
  return unidad === '%' ? `${numero.format(indicador.valor)} %` : `${numero.format(indicador.valor)} ${unidad}`.trim()
}

/** El estado del indicador en palabras: el color solo acompaña. */
export function estadoIndicador(indicador: Indicador): { texto: string; clase: string } {
  if (indicador.cumple === true) return { texto: '✓ Cumple', clase: 'estado--cumple' }
  if (indicador.cumple === false) return { texto: '✗ No cumple', clase: 'estado--no-cumple' }
  if (indicador.meta === null && indicador.valor !== null) return { texto: 'Seguimiento', clase: 'estado--neutro' }
  if (indicador.muestra === 0 && indicador.unidad === null) return { texto: 'Fuera de la app', clase: 'estado--neutro' }
  return { texto: 'Sin datos', clase: 'estado--neutro' }
}

/** Tabla en texto para pegar en el Plan de Calidad (sección 5, evidencias métricas). */
export function textoParaInforme(analisis: Analisis): string {
  const filas = analisis.indicadores.map((i) =>
    [i.proceso, i.nombre, valorIndicador(i), i.meta ?? 'Seguimiento', estadoIndicador(i).texto.replace(/^[✓✗] /, ''), String(i.muestra)].join('\t'),
  )
  return [
    `Indicadores de calidad · Cocina sin estrés · ${fechaCorta(analisis.desde)} a ${fechaCorta(analisis.hasta)}`,
    ['Proceso', 'Indicador', 'Valor', 'Meta', 'Estado', 'Muestra'].join('\t'),
    ...filas,
  ].join('\n')
}
