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

export function esAlergia(codigo: string): boolean {
  return CODIGOS_ALERGIA.has(codigo)
}

/** "Lactosa", "Colesterol alto"…; un código desconocido se muestra tal cual. */
export function nombreRestriccion(codigo: string): string {
  return NOMBRES.get(codigo) ?? codigo
}
