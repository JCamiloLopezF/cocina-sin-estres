import type { ItemDespensa, ProductoDisponible } from '../features/despensa/tipos'
import type { RecetaDetalle } from '../features/recetario/tipos'
import { CUENTA_USUARIA } from './backendFalso'
import { respuestaJson } from './servicioFalso'

/**
 * Una cocina de mentiras para probar el recetario y el planificador:
 * - Sopa de lentejas (25 min, 4 porciones): $3.600 → $900 por porción; falta la zanahoria.
 * - Arroz con pollo (50 min, 4 porciones): $12.400 → $3.100 por porción; faltan arroz y pollo.
 */
function producto(id: number, nombre: string, unidadBase: 'g' | 'unidad', cantidadUnidadCompra: number, precio: number): ProductoDisponible {
  return {
    id,
    nombre,
    categoria: 'GRANOS',
    tipoCantidad: unidadBase === 'g' ? 'GRANEL' : 'CONTABLE',
    unidadBase,
    unidadCompra: unidadBase === 'g' ? 'Bolsa' : 'Unidad',
    cantidadUnidadCompra,
    precioUnidadCompra: precio,
  }
}

export const CATALOGO: ProductoDisponible[] = [
  { ...producto(1, 'Lenteja', 'g', 500, 4000), itemDespensaId: 31 },
  { ...producto(2, 'Cebolla', 'unidad', 1, 800), itemDespensaId: 32 },
  producto(3, 'Zanahoria', 'unidad', 1, 800),
  producto(4, 'Arroz', 'g', 1000, 4500),
  producto(5, 'Pollo', 'g', 500, 9800),
  producto(6, 'Sal', 'g', 1000, 2000),
  producto(7, 'Leche', 'g', 1000, 3800),
  producto(8, 'Azúcar', 'g', 1000, 4500),
]

function enDespensa(id: number, productoId: number, nombre: string, cantidad: number, unidadBase: 'g' | 'unidad'): ItemDespensa {
  return {
    id,
    productoId,
    nombre,
    categoria: 'GRANOS',
    tipoCantidad: unidadBase === 'g' ? 'GRANEL' : 'CONTABLE',
    unidadBase,
    cantidad,
    esEstimada: unidadBase === 'g',
    estadoVencimiento: 'SIN_FECHA',
    bajoMinimo: false,
  }
}

export const DESPENSA: ItemDespensa[] = [enDespensa(31, 1, 'Lenteja', 500, 'g'), enDespensa(32, 2, 'Cebolla', 3, 'unidad')]

export const SOPA: RecetaDetalle = {
  id: 10,
  nombre: 'Sopa de lentejas',
  descripcion: 'Sopa espesa y rendidora.',
  tiempoMinutos: 25,
  porciones: 4,
  preparacionPrevia: 'Remoja la lenteja 30 minutos.',
  pasos: ['Sofríe la cebolla.', 'Agrega la lenteja y el agua.'],
  ingredientes: [
    { productoId: 1, cantidad: 250, opcional: false },
    { productoId: 2, cantidad: 1, opcional: false },
    { productoId: 3, cantidad: 1, opcional: false },
    { productoId: 6, cantidad: 5, opcional: true },
  ],
}

export const ARROZ_CON_POLLO: RecetaDetalle = {
  id: 11,
  nombre: 'Arroz con pollo',
  tiempoMinutos: 50,
  porciones: 4,
  pasos: ['Dora el pollo.', 'Agrega el arroz.'],
  ingredientes: [
    { productoId: 4, cantidad: 400, opcional: false },
    { productoId: 5, cantidad: 500, opcional: false },
    { productoId: 2, cantidad: 1, opcional: false },
  ],
}

/** Lleva leche (lactosa) y azúcar (diabetes): sirve para probar las alergias por persona. */
export const ARROZ_CON_LECHE: RecetaDetalle = {
  id: 12,
  nombre: 'Arroz con leche',
  tiempoMinutos: 40,
  porciones: 4,
  pasos: ['Cocina el arroz en la leche.', 'Endulza al final.'],
  ingredientes: [
    { productoId: 4, cantidad: 200, opcional: false, restricciones: [] },
    { productoId: 7, cantidad: 1000, opcional: false, restricciones: ['lactosa'] },
    { productoId: 8, cantidad: 100, opcional: false, restricciones: ['diabetes'] },
  ],
  restricciones: ['diabetes', 'lactosa'],
}

/** Perfil con alergias por persona: Samuel no puede lactosa (no grave) y Luisa tiene diabetes. */
export const PERFIL_CON_ALERGIAS = {
  diasSinTiempo: [3],
  periodicidadIngreso: 'QUINCENAL',
  personas: [
    { id: 1, nombre: 'Luisa', edad: 34, restricciones: ['diabetes'], alergiasGraves: [] as string[] },
    { id: 2, nombre: 'Samuel', edad: 9, restricciones: ['lactosa'], alergiasGraves: [] as string[] },
  ],
}

/** El mismo hogar, pero la lactosa de Samuel es grave: la receta con leche no se cocina en casa. */
export const PERFIL_CON_ALERGIA_GRAVE = {
  ...PERFIL_CON_ALERGIAS,
  personas: [PERFIL_CON_ALERGIAS.personas[0], { ...PERFIL_CON_ALERGIAS.personas[1], alergiasGraves: ['lactosa'] }],
}

const resumen = (r: RecetaDetalle) => ({
  id: r.id,
  nombre: r.nombre,
  descripcion: r.descripcion,
  tiempoMinutos: r.tiempoMinutos,
  porciones: r.porciones,
})

export { resumen as resumenReceta }

/** Rutas del backend falso que leen las pantallas del recetario y del plan. */
export function rutasCocina() {
  return {
    'GET /yo': () => respuestaJson(CUENTA_USUARIA),
    'GET /recetas': () => respuestaJson([ARROZ_CON_POLLO, SOPA].map(resumen)),
    'GET /recetas/10': () => respuestaJson(SOPA),
    'GET /recetas/11': () => respuestaJson(ARROZ_CON_POLLO),
    'GET /items-despensa/catalogo': () => respuestaJson(CATALOGO),
    'GET /items-despensa': () => respuestaJson(DESPENSA),
    'GET /perfil/hogar': () => respuestaJson({ diasSinTiempo: [3], periodicidadIngreso: 'QUINCENAL' }),
  }
}
