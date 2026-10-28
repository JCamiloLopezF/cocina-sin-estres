import { NOMBRE_COMIDA } from '../planificador/semana'
import type { TipoComida } from '../planificador/tipos'
import type { ProductoCurado, RecetaCuradaResumen } from './apiCuraduria'

/**
 * Reglas del formulario de recetas del curador (RNF-28). Son las mismas que valida la API
 * (`ValidadorReceta`, `ReglaAlmuerzo`); aquí se avisan mientras se escribe, para que la
 * receta se guarde a la primera.
 */

/** Las mismas formas de decir un tiempo que reconocen el Modo cocinar y `PasosReceta`. */
const TIEMPO =
  /(?:\d+\s*(?:a|-|o)\s*)?(?:\d+|un|uno|dos|tres|cuatro|cinco|seis|siete|ocho|nueve|diez|quince|veinte|treinta|cuarenta)\s*(?:minutos?|min\b)|(?:\d+(?:[.,]\d+)?|un|una|media|dos|tres)\s+horas?/gi

const RANGO = /\d+ ?(-|a) ?\d+ minutos/i

/** Los tiempos que dice un paso: "Sofríe 2 minutos y cocina 4 minutos" → ["2 minutos", "4 minutos"]. */
export function tiemposDelPaso(paso: string): string[] {
  return paso.match(TIEMPO) ?? []
}

/** Los pasos del cuadro de texto: uno por línea, sin líneas vacías. */
export function pasosDesde(texto: string): string[] {
  return texto
    .split('\n')
    .map((p) => p.trim())
    .filter(Boolean)
}

/**
 * Lo que hay que corregir en un paso para que el Modo cocinar lo entienda; `undefined` si
 * está bien.
 */
export function problemaDelPaso(paso: string): string | undefined {
  const tiempos = tiemposDelPaso(paso)
  if (tiempos.length > 1) {
    return `Dice dos tiempos (${tiempos.join(' y ')}). Pártelo en dos pasos para que el temporizador sepa qué contar.`
  }
  const minusculas = paso.toLowerCase()
  if (RANGO.test(paso) || minusculas.includes('según') || minusculas.includes('instrucciones')) {
    return 'Di un solo tiempo ("cocina 10 minutos") y no remitas al empaque o a las instrucciones.'
  }
  if (paso.length > 500) {
    return 'Es muy largo: pártelo en dos.'
  }
  return undefined
}

const PROTEINA = new Set(['PROTEINAS', 'LEGUMBRES'])
const ACOMPANANTE = new Set(['GRANOS', 'TUBERCULOS', 'HARINAS'])

/** Un almuerzo es un plato completo: una proteína y algo que llene (V17). */
export function faltaParaAlmuerzo(categorias: string[]): string | undefined {
  const proteina = categorias.some((c) => PROTEINA.has(c))
  const llena = categorias.some((c) => ACOMPANANTE.has(c))
  if (proteina && llena) return undefined
  if (!proteina && !llena) return 'una proteína (carne, pollo, pescado, huevo o legumbres) y algo que llene (arroz, pasta, papa, yuca, plátano o arepa)'
  return proteina
    ? 'algo que llene (arroz, pasta, papa, yuca, plátano o arepa)'
    : 'una proteína (carne, pollo, pescado, huevo o legumbres)'
}

export interface FilaIngrediente {
  /** Clave estable de la fila en el formulario. */
  clave: number
  productoId: number | undefined
  cantidad: string
  opcional: boolean
  principal: boolean
}

export interface FormularioReceta {
  nombre: string
  descripcion: string
  tiempoMinutos: string
  porciones: string
  pasos: string
  preparacionPrevia: string
  ingredientes: FilaIngrediente[]
  momentos: TipoComida[]
  platoFuerte: boolean
  gustaANinos: boolean
}

export type ErroresReceta = Partial<Record<'nombre' | 'tiempoMinutos' | 'porciones' | 'pasos' | 'ingredientes' | 'momentos', string>>

/** "1.5" o "1,5" → 1.5; vacío o inválido → NaN. */
export function numero(texto: string): number {
  return texto.trim() === '' ? NaN : Number(texto.trim().replace(',', '.'))
}

/** Costo de la receta con los precios de referencia, sin los opcionales (misma fórmula del backend). */
export function costoDe(filas: FilaIngrediente[], productos: Map<number, ProductoCurado>): number {
  return filas.reduce((total, fila) => {
    const producto = fila.productoId !== undefined ? productos.get(fila.productoId) : undefined
    const cantidad = numero(fila.cantidad)
    if (!producto || fila.opcional || !(cantidad > 0) || !producto.cantidadUnidadCompra) return total
    return total + (cantidad / producto.cantidadUnidadCompra) * producto.precioUnidadCompra
  }, 0)
}

export function validarReceta(f: FormularioReceta, productos: Map<number, ProductoCurado>): ErroresReceta {
  const nombre = f.nombre.trim()
  const tiempo = numero(f.tiempoMinutos)
  const porciones = numero(f.porciones)
  const pasos = pasosDesde(f.pasos)
  const conProducto = f.ingredientes.filter((i) => i.productoId !== undefined)
  const ids = conProducto.map((i) => i.productoId)
  const errores: ErroresReceta = {
    nombre: !nombre ? 'Escribe el nombre.' : nombre.length < 3 || nombre.length > 120 ? 'Entre 3 y 120 caracteres.' : undefined,
    tiempoMinutos: Number.isInteger(tiempo) && tiempo >= 1 && tiempo <= 600 ? undefined : 'Entre 1 y 600 minutos.',
    porciones: Number.isInteger(porciones) && porciones >= 1 && porciones <= 20 ? undefined : 'Entre 1 y 20 porciones.',
    momentos: f.momentos.length ? undefined : 'Marca al menos una comida.',
  }
  if (pasos.length === 0) {
    errores.pasos = 'Escribe al menos un paso.'
  } else if (pasos.length > 30) {
    errores.pasos = 'Hasta 30 pasos.'
  } else {
    const malo = pasos.findIndex((p) => problemaDelPaso(p))
    if (malo >= 0) errores.pasos = `Paso ${malo + 1}: ${problemaDelPaso(pasos[malo])}`
  }
  if (conProducto.length === 0) {
    errores.ingredientes = 'Agrega al menos un ingrediente.'
  } else if (new Set(ids).size !== ids.length) {
    errores.ingredientes = 'Un producto está dos veces: suma las cantidades en un solo ingrediente.'
  } else if (conProducto.some((i) => !(numero(i.cantidad) > 0))) {
    errores.ingredientes = 'Cada ingrediente necesita una cantidad mayor que cero.'
  } else if (conProducto.every((i) => i.opcional)) {
    errores.ingredientes = 'Al menos un ingrediente tiene que ser obligatorio.'
  } else if (f.platoFuerte && f.momentos.includes('ALMUERZO')) {
    const categorias = conProducto
      .filter((i) => !i.opcional)
      .map((i) => productos.get(i.productoId as number)?.categoria ?? '')
    const falta = faltaParaAlmuerzo(categorias)
    if (falta) errores.ingredientes = `Para el almuerzo le falta ${falta}. Agrégalo o márcala solo para desayuno o cena.`
  }
  return errores
}

/** "Almuerzo y cena" · "Desayuno" · "Acompañamiento". */
export function comidasDe(receta: Pick<RecetaCuradaResumen, 'momentos' | 'platoFuerte'>): string {
  const nombres = receta.momentos.map((m) => NOMBRE_COMIDA[m].toLowerCase())
  const lista = nombres.length <= 1 ? (nombres[0] ?? '') : `${nombres.slice(0, -1).join(', ')} y ${nombres[nombres.length - 1]}`
  const texto = receta.platoFuerte ? lista : `acompañamiento${lista ? ` (${lista})` : ''}`
  return texto.charAt(0).toUpperCase() + texto.slice(1)
}
