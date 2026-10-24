import { ErrorApi, pedir } from '../../api/cliente'
import { nombreRestriccion } from '../perfil/restricciones'

/**
 * Cómo adaptar una receta a las alergias de cada persona (ADR-24, `GET /api/adaptaciones`).
 * La mamá cocina para todos y a quien es alérgico le cambia el ingrediente o le hace
 * otro plato; solo una alergia grave saca la receta de la casa.
 */
export type EstadoAdaptacion = 'APTA' | 'ADAPTADA' | 'PLATO_APARTE' | 'NO_APTA'

export interface CambioAdaptacion {
  tipo: 'REEMPLAZO' | 'SIN' | 'PLATO_APARTE' | 'NO_APTA'
  persona: string
  alergias: string[]
  producto?: string | null
  reemplazo?: string | null
  enDespensa: boolean
  recetaAparteId?: number | null
  recetaAparte?: string | null
}

export interface Adaptacion {
  recetaId: number
  estado: EstadoAdaptacion
  cambios: CambioAdaptacion[]
}

/**
 * Adaptaciones de varias recetas, por id. Si el hogar no tiene perfil o la consulta
 * falla, devuelve un mapa vacío: la pantalla sigue con los avisos de siempre.
 */
export async function obtenerAdaptaciones(recetas: number[], senal?: AbortSignal): Promise<Map<number, Adaptacion>> {
  if (recetas.length === 0) return new Map()
  try {
    const lista = await pedir<Adaptacion[]>(`/adaptaciones?recetas=${recetas.join(',')}`, { senal })
    return new Map(lista.map((a) => [a.recetaId, a]))
  } catch (error) {
    if (error instanceof DOMException && error.name === 'AbortError') throw error
    if (error instanceof ErrorApi || error instanceof TypeError) return new Map()
    throw error
  }
}

function minuscula(texto?: string | null): string {
  return (texto ?? '').toLowerCase()
}

function alergias(cambio: CambioAdaptacion): string {
  return cambio.alergias.map((a) => nombreRestriccion(a).toLowerCase()).join(' y ')
}

/** El texto que se le muestra a la usuaria por cada cambio. */
export function textoCambio(cambio: CambioAdaptacion): string {
  const esTu = cambio.persona === 'ti'
  const para = `Para ${cambio.persona}`
  switch (cambio.tipo) {
    case 'REEMPLAZO':
      return `${para} (${alergias(cambio)}): cambia ${minuscula(cambio.producto)} por ${minuscula(cambio.reemplazo)}${
        cambio.enDespensa ? ', que ya tienes en la despensa' : ''
      }.`
    case 'SIN':
      return `${para} (${alergias(cambio)}): sirve ${esTu ? 'tu' : 'su'} porción sin ${minuscula(cambio.producto)}.`
    case 'PLATO_APARTE':
      return cambio.recetaAparte
        ? `${para} (${alergias(cambio)}): no se puede cambiar lo principal del plato; ${
            esTu ? 'prepárate' : 'prepárale'
          } aparte ${cambio.recetaAparte}.`
        : `${para} (${alergias(cambio)}): no se puede cambiar lo principal del plato; ${
            esTu ? 'prepárate' : 'prepárale'
          } otro plato.`
    case 'NO_APTA':
      return esTu
        ? `No apta: tienes alergia grave a ${alergias(cambio)}.`
        : `No apta: ${cambio.persona} tiene alergia grave a ${alergias(cambio)}.`
  }
}

/** "Samuel" · "Samuel y Ana": las personas con algún cambio en la receta. */
export function personasConCambio(adaptacion?: Adaptacion): string[] {
  return [...new Set((adaptacion?.cambios ?? []).map((c) => c.persona))]
}
