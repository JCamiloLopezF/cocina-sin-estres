import { ErrorApi, pedir } from '../../api/cliente'
import { catalogoDisponible, listarDespensa } from '../despensa/apiDespensa'
import type { ProductoDisponible, UnidadBase } from '../despensa/tipos'
import { ALERGIAS, esAlergia, nombreRestriccion } from '../perfil/restricciones'
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
  /** Productos con algo vencido en la despensa: el recetario dice «vencido» y no «falta». */
  vencidos?: Set<number>
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
  const vencidos = new Set<number>()
  items.forEach((item) => {
    if (item.estadoVencimiento === 'VENCIDO') vencidos.add(item.productoId)
    else existencias.set(item.productoId, (existencias.get(item.productoId) ?? 0) + item.cantidad)
  })
  return { productos: new Map(productos.map((p) => [p.id, p])), existencias, vencidos, personas }
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
  /** No alcanza y lo que hay está vencido (auditoría: decía «falta»). */
  vencido: boolean
  /** Alergias y condiciones del producto, con su nombre ("Lactosa"). */
  restricciones: string[]
}

export interface RecetaVista {
  receta: RecetaDetalle
  /** Porciones para las que están calculadas las cantidades y el costo. */
  porciones: number
  ingredientes: IngredienteVista[]
  /** Costo de la receta completa sin los opcionales (que no se compran). */
  costo?: number
  costoPorcion?: number
  /** Ingredientes obligatorios que no alcanzan con lo que hay en la despensa (incluye los vencidos). */
  faltan: number
  /** Nombres de los obligatorios que no alcanzan, para avisar antes de cocinar. */
  faltantes: string[]
  /** Nombres de los obligatorios que solo están vencidos en la despensa. */
  vencidos: string[]
  /** Personas con una alergia grave a un ingrediente obligatorio: la receta no se cocina en casa. */
  noAptaPara: Choque[]
  /** Personas con una alergia (no grave): se les cambia el ingrediente o comen otro plato (ADR-24). */
  ajustePara: Choque[]
  /** Personas con una condición (diabetes, hipertensión…) que un ingrediente toca: solo avisa. */
  precaucionPara: Choque[]
  /**
   * Restricciones que la receta cumple, para la tarjeta (RNF-15): "Sin lactosa". Primero las
   * alergias de alguien del hogar; si nadie tiene, las tres más comunes (lactosa, gluten, huevo).
   */
  cumple: string[]
}

const ALERGIAS_COMUNES = ['lactosa', 'gluten', 'huevo']

/** "Sin lactosa", "Sin gluten"… para las alergias que la receta no tiene en sus ingredientes obligatorios. */
export function restriccionesQueCumple(receta: RecetaDetalle, personas: PersonaHogar[]): string[] {
  const deLaReceta = new Set(receta.restricciones ?? [])
  const delHogar = ALERGIAS.map((a) => a.codigo).filter((codigo) => personas.some((p) => p.restricciones.includes(codigo)))
  const revisar = delHogar.length > 0 ? delHogar : ALERGIAS_COMUNES
  return revisar.filter((codigo) => !deLaReceta.has(codigo)).map((codigo) => `Sin ${nombreRestriccion(codigo).toLowerCase()}`)
}

/** Quién y por qué: "Samuel" por "Lactosa". */
export interface Choque {
  persona: string
  restricciones: string[]
}

/**
 * Porciones que cocina el hogar: una por persona, como el backend calcula el plan, la lista
 * y lo que se descuenta de la despensa. Sin perfil, las de la receta.
 */
export function porcionesDelHogar(receta: RecetaDetalle, cocina: Cocina): number {
  return cocina.personas.length > 0 ? cocina.personas.length : receta.porciones
}

function alcanza(cocina: Cocina, productoId: number, cantidad: number): boolean {
  return (cocina.existencias.get(productoId) ?? 0) >= cantidad - 1e-9
}

/**
 * Aviso antes de cocinar (auditoría del 27 de octubre: el Modo cocinar dejaba usar huevos
 * vencidos sin decir nada): «No tienes suficiente arroz. Lo que hay de huevos está vencido: no lo uses».
 */
export function avisoAntesDeCocinar(vista: Pick<RecetaVista, 'faltantes' | 'vencidos'>): string | undefined {
  const partes: string[] = []
  if (vista.faltantes.length) partes.push(`No tienes suficiente ${enLista(vista.faltantes).toLowerCase()}.`)
  if (vista.vencidos.length) partes.push(`Lo que hay de ${enLista(vista.vencidos).toLowerCase()} está vencido: no lo uses.`)
  return partes.length ? partes.join(' ') : undefined
}

function enLista(nombres: string[]): string {
  return nombres.length === 1 ? nombres[0] : `${nombres.slice(0, -1).join(', ')} y ${nombres[nombres.length - 1]}`
}

/** Calcula costo y faltantes. `porciones` escala las cantidades (por defecto, las del hogar). */
export function analizarReceta(receta: RecetaDetalle, cocina: Cocina, porciones = porcionesDelHogar(receta, cocina)): RecetaVista {
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
      enDespensa: alcanza(cocina, ing.productoId, cantidad),
      vencido: !alcanza(cocina, ing.productoId, cantidad) && (cocina.vencidos?.has(ing.productoId) ?? false),
      restricciones: (ing.restricciones ?? []).map(nombreRestriccion),
    }
  })
  const obligatorios = ingredientes.filter((i) => !i.opcional)
  const conPrecio = obligatorios.filter((i) => i.costo !== undefined)
  const costo = conPrecio.length ? conPrecio.reduce((suma, i) => suma + (i.costo ?? 0), 0) : undefined
  return {
    receta,
    porciones,
    ingredientes,
    costo,
    costoPorcion: costo !== undefined && porciones > 0 ? costo / porciones : undefined,
    faltan: obligatorios.filter((i) => !i.enDespensa).length,
    faltantes: obligatorios.filter((i) => !i.enDespensa && !i.vencido).map((i) => i.nombre),
    vencidos: obligatorios.filter((i) => i.vencido).map((i) => i.nombre),
    ...choques(receta, cocina.personas),
    cumple: restriccionesQueCumple(receta, cocina.personas),
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
