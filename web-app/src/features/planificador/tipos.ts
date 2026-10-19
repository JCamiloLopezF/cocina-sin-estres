/** Tipos de la API del planificador (backend: módulo planificador, `/api/planes-semanales`). */

export type TipoComida = 'DESAYUNO' | 'ALMUERZO' | 'CENA'

/** Día de la semana como lo guarda el backend: 1 = lunes … 7 = domingo. */
export type Dia = 1 | 2 | 3 | 4 | 5 | 6 | 7

export interface ComidaPlan {
  id: number
  planSemanalId: number
  dia: number
  tipoComida: TipoComida
  recetaId: number
  preparada: boolean
  preparadaEn?: string
}

/** `GET /api/planes-semanales?semanaInicio=AAAA-MM-DD` (PlanSemanalResponse). */
export interface PlanSemanal {
  id: number
  /** Lunes de la semana, AAAA-MM-DD. */
  semanaInicio: string
  comidas: ComidaPlan[]
}

/** `GET /api/planes-semanales/{id}/costo` (PlanCostoResponse, ADR-14). */
export interface CostoPlan {
  /** Acumulado de lunes (índice 0) a domingo (índice 6). El último es el total. */
  costoAcumuladoPorDia: number[]
  presupuestoSemanal: number
  excedePresupuesto: boolean
}

export interface ItemListaCompras {
  id: number
  productoId: number
  cantidad: number
  costoEstimado: number
  /** 1 = indispensable; mayor = se puede posponer. */
  prioridad: number
  comprado: boolean
}

/** `GET /api/planes-semanales/{id}/lista-compras`: ya descuenta lo que hay en la despensa. */
export interface ListaCompras {
  id?: number
  planSemanalId: number
  items: ItemListaCompras[]
  costoTotal: number
}
