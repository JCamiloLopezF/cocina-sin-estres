/**
 * Alergias y condiciones que maneja la app (tabla restriccion, V6). El código es el que
 * se guarda cifrado (ADR-20). Una ALERGIA se adapta: a esa persona se le cambia el
 * ingrediente o se le hace otro plato; si es grave, la receta no se propone (ADR-24).
 * Una CONDICION solo avisa.
 */
export const ALERGIAS = [
  { codigo: 'lactosa', texto: 'Lactosa' },
  { codigo: 'gluten', texto: 'Gluten' },
  { codigo: 'mani', texto: 'Maní' },
  { codigo: 'huevo', texto: 'Huevo' },
  { codigo: 'mariscos', texto: 'Mariscos' },
  { codigo: 'pescado', texto: 'Pescado' },
]

export const CONDICIONES = [
  { codigo: 'hipertension', texto: 'Hipertensión' },
  { codigo: 'diabetes', texto: 'Diabetes' },
  { codigo: 'colesterol', texto: 'Colesterol alto' },
]

const NOMBRES = new Map([...ALERGIAS, ...CONDICIONES].map((r) => [r.codigo, r.texto]))
const CODIGOS_ALERGIA = new Set(ALERGIAS.map((a) => a.codigo))

/**
 * Alergia a un ingrediente que no está en la lista (auditoría del 4 de noviembre): se
 * guarda como `producto:<id>` con las demás restricciones de la persona, y el backend la
 * adapta igual que cualquier alergia (AlergiaIngrediente).
 */
const PREFIJO_INGREDIENTE = 'producto:'

/** Nombres de los ingredientes del catálogo, para mostrar "Piña" en vez de "producto:31". */
const nombresIngredientes = new Map<number, string>()

export function codigoIngrediente(productoId: number): string {
  return `${PREFIJO_INGREDIENTE}${productoId}`
}

/** El producto de un código `producto:<id>`, o `undefined` si es otra restricción. */
export function ingredienteDe(codigo: string): number | undefined {
  if (!codigo.startsWith(PREFIJO_INGREDIENTE)) return undefined
  const id = Number(codigo.slice(PREFIJO_INGREDIENTE.length))
  return Number.isInteger(id) ? id : undefined
}

/** Guarda los nombres de los productos para `nombreRestriccion`. */
export function recordarIngredientes(productos: Iterable<{ id: number; nombre: string }>): void {
  for (const p of productos) nombresIngredientes.set(p.id, p.nombre)
}

export function esAlergia(codigo: string): boolean {
  return CODIGOS_ALERGIA.has(codigo) || ingredienteDe(codigo) !== undefined
}

/** "Lactosa", "Colesterol alto", "Piña"…; un código desconocido se muestra tal cual. */
export function nombreRestriccion(codigo: string): string {
  const ingrediente = ingredienteDe(codigo)
  if (ingrediente !== undefined) return nombresIngredientes.get(ingrediente) ?? 'un ingrediente'
  return NOMBRES.get(codigo) ?? codigo
}
