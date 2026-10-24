import { ErrorApi, pedir } from '../../api/cliente'
import { catalogoDisponible, listarDespensa } from '../despensa/apiDespensa'
import type { ProductoDisponible, UnidadBase } from '../despensa/tipos'
import { esAlergia, nombreRestriccion } from '../perfil/restricciones'
import type { RecetaDetalle } from './tipos'

/**
 * Lo que hay en la cocina del hogar: el catálogo (nombres, unidades y precios de
 * referencia) y lo que queda en la despensa. Con esto el recetario sabe qué falta y
 * cuánto cuesta cada receta, con la misma fórmula del backend (KAN-24):
 * `cantidad / cantidadUnidadCompra * precioUnidadCompra`.
 */
export interface Cocina {
  productos: Map<number, ProductoDisponible>
  /** Cantidad disponible por producto, en su unidad base. Lo vencido no cuenta. */
  existencias: Map<number, number>
  /** Quiénes comen en casa y sus alergias y condiciones (perfil del hogar). */
  personas: PersonaHogar[]
}

export interface PersonaHogar {
  nombre: string
  restricciones: string[]
  /** Alergias graves: la receta no se propone ni adaptada (ADR-24). */
  alergiasGraves: string[]
}

interface PerfilConPersonas {
  personas: { nombre: string | null; restricciones?: string[]; alergiasGraves?: string[] }[]
}

/** Si el hogar todavía no tiene perfil, la despensa y el catálogo responden este código. */
function sinHogar<T>(valor: T) {
  return (error: unknown): T => {
    if (error instanceof ErrorApi && error.codigo === 'DESPENSA_HOGAR_REQUERIDO') {
      return valor
    }
    throw error
  }
}

export async function cargarCocina(senal?: AbortSignal): Promise<Cocina> {
  const [productos, items, perfil] = await Promise.all([
    catalogoDisponible('', senal).catch(sinHogar<ProductoDisponible[]>([])),
    listarDespensa('TODO', '', senal).catch(sinHogar([])),
    // Sin perfil (404) el recetario igual funciona, solo que sin filtrar por alergias.
    pedir<PerfilConPersonas>('/perfil/hogar', { senal }).catch((error: unknown) => {
      if (error instanceof DOMException && error.name === 'AbortError') throw error
      return undefined
    }),
  ])
  const personas = (perfil?.personas ?? []).map((p, i) => ({
    nombre: p.nombre?.trim() || (i === 0 ? 'ti' : `la persona ${i + 1}`),
    restricciones: p.restricciones ?? [],
    alergiasGraves: p.alergiasGraves ?? [],
  }))
  const existencias = new Map<number, number>()
  items
    .filter((item) => item.estadoVencimiento !== 'VENCIDO')
    .forEach((item) => existencias.set(item.productoId, (existencias.get(item.productoId) ?? 0) + item.cantidad))
  return { productos: new Map(productos.map((p) => [p.id, p])), existencias, personas }
}

export interface IngredienteVista {
  productoId: number
  nombre: string
  cantidad: number
  unidad: UnidadBase
  opcional: boolean
  /** Costo estimado de esa cantidad; ausente si el producto no tiene precio de referencia. */
  costo?: number
  /** La despensa alcanza para esta cantidad. */
  enDespensa: boolean
  /** Alergias y condiciones del producto, con su nombre ("Lactosa"). */
  restricciones: string[]
}

export interface RecetaVista {
  receta: RecetaDetalle
  ingredientes: IngredienteVista[]
  /** Costo de la receta completa sin los opcionales (que no se compran). */
  costo?: number
  costoPorcion?: number
  /** Ingredientes obligatorios que no alcanzan con lo que hay en la despensa. */
  faltan: number
  /** Personas con una alergia grave a un ingrediente obligatorio: la receta no se cocina en casa. */
  noAptaPara: Choque[]
  /** Personas con una alergia (no grave): se les cambia el ingrediente o comen otro plato (ADR-24). */
  ajustePara: Choque[]
  /** Personas con una condición (diabetes, hipertensión…) que un ingrediente toca: solo avisa. */
  precaucionPara: Choque[]
}

/** Quién y por qué: "Samuel" por "Lactosa". */
export interface Choque {
  persona: string
  restricciones: string[]
}

/** Calcula costo y faltantes. `porciones` escala las cantidades (por defecto, las de la receta). */
export function analizarReceta(receta: RecetaDetalle, cocina: Cocina, porciones = receta.porciones): RecetaVista {
  const factor = receta.porciones > 0 ? porciones / receta.porciones : 1
  const ingredientes = receta.ingredientes.map((ing): IngredienteVista => {
    const producto = cocina.productos.get(ing.productoId)
    const cantidad = ing.cantidad * factor
    const costo =
      producto?.precioUnidadCompra != null && producto.cantidadUnidadCompra
        ? (cantidad / producto.cantidadUnidadCompra) * producto.precioUnidadCompra
        : undefined
    return {
      productoId: ing.productoId,
      nombre: producto?.nombre ?? `Producto ${ing.productoId}`,
      cantidad,
      unidad: producto?.unidadBase ?? 'unidad',
      opcional: ing.opcional,
      costo,
      enDespensa: (cocina.existencias.get(ing.productoId) ?? 0) >= cantidad - 1e-9,
      restricciones: (ing.restricciones ?? []).map(nombreRestriccion),
    }
  })
  const obligatorios = ingredientes.filter((i) => !i.opcional)
  const conPrecio = obligatorios.filter((i) => i.costo !== undefined)
  const costo = conPrecio.length ? conPrecio.reduce((suma, i) => suma + (i.costo ?? 0), 0) : undefined
  return {
    receta,
    ingredientes,
    costo,
    costoPorcion: costo !== undefined && porciones > 0 ? costo / porciones : undefined,
    faltan: obligatorios.filter((i) => !i.enDespensa).length,
    ...choques(receta, cocina.personas),
  }
}

/** Cruza las restricciones de la receta con las de cada persona del hogar. */
function choques(
  receta: RecetaDetalle,
  personas: PersonaHogar[],
): Pick<RecetaVista, 'noAptaPara' | 'ajustePara' | 'precaucionPara'> {
  const deLaReceta = new Set(receta.restricciones ?? [])
  const noAptaPara: Choque[] = []
  const ajustePara: Choque[] = []
  const precaucionPara: Choque[] = []
  for (const persona of personas) {
    const comunes = persona.restricciones.filter((r) => deLaReceta.has(r))
    const graves = comunes.filter((r) => esAlergia(r) && persona.alergiasGraves.includes(r)).map(nombreRestriccion)
    const alergias = comunes.filter((r) => esAlergia(r) && !persona.alergiasGraves.includes(r)).map(nombreRestriccion)
    const condiciones = comunes.filter((r) => !esAlergia(r)).map(nombreRestriccion)
    if (graves.length) noAptaPara.push({ persona: persona.nombre, restricciones: graves })
    if (alergias.length) ajustePara.push({ persona: persona.nombre, restricciones: alergias })
    if (condiciones.length) precaucionPara.push({ persona: persona.nombre, restricciones: condiciones })
  }
  return { noAptaPara, ajustePara, precaucionPara }
}

/** "Samuel" · "Samuel y ti" · "Ana, Samuel y ti". */
export function listaPersonas(choques: Choque[]): string {
  const nombres = choques.map((c) => c.persona)
  return nombres.length <= 1 ? (nombres[0] ?? '') : `${nombres.slice(0, -1).join(', ')} y ${nombres[nombres.length - 1]}`
}
