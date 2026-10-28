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
  /** Se hizo sin conexión y todavía no llega al servidor (RNF-17); su id es provisional (negativo). */
  pendiente?: boolean
}

/** `GET /api/planes-semanales?semanaInicio=AAAA-MM-DD` (PlanSemanalResponse). */
export interface PlanSemanal {
  id: number
  /** Lunes de la semana, AAAA-MM-DD. */
  semanaInicio: string
  comidas: ComidaPlan[]
  /** La armó la app sola (motor de avisos, ADR-24) y la usuaria todavía no la confirma. */
  borrador?: boolean
}

/** `GET /api/planes-semanales/{id}/costo` (PlanCostoResponse, ADR-14). */
export interface CostoPlan {
  /** Valor de las comidas, acumulado de lunes (índice 0) a domingo (índice 6), aunque parte ya esté en la despensa. */
  costoAcumuladoPorDia: number[]
  presupuestoSemanal: number
  /** Lo que falta comprar supera el presupuesto. */
  excedePresupuesto: boolean
  /**
   * El costo del plan según RNF-02: lo que falta comprar con lo que ya hay en la despensa, para
   * las comidas que quedan por preparar. Es lo que se compara con el presupuesto.
   */
  costoPorComprar?: number
  /** Lo que suma el reemplazo o el plato aparte de quien tiene alergia (KAN-24); ya está en el total. */
  costoAlergias?: number
}

export interface ItemListaCompras {
  id: number
  productoId: number
  cantidad: number
  costoEstimado: number
  /** Lo que la usuaria corrigió que pagó (KAN-26); si falta, cuenta el estimado. */
  costoReal?: number | null
  /** 1 = indispensable; mayor = se puede posponer. */
  prioridad: number
  comprado: boolean
  /** PLAN (lo piden las recetas), REPONER (quedó bajo el mínimo) o ALERGIA (porción de quien tiene alergia). */
  motivo?: 'PLAN' | 'REPONER' | 'ALERGIA'
  /** Cuánto entró a la despensa al comprarlo, en presentaciones completas. */
  cantidadAgregada?: number | null
}

/** `PUT /api/planes-semanales/{id}/lista-compras/items/{itemId}` (ADR-24). */
export interface CompraRegistrada {
  item: ItemListaCompras
  /** Vencimiento con que quedó en la despensa, AAAA-MM-DD. */
  venceEn?: string | null
}

/** `GET /api/planes-semanales/{id}/lista-compras`: ya descuenta lo que hay en la despensa. */
export interface ListaCompras {
  id?: number
  planSemanalId: number
  items: ItemListaCompras[]
  /** Suma de lo que pagó (en lo que corrigió) y del estimado (en lo demás). */
  costoTotal: number
}

/** Un hueco que el generador llenó o cambió. `recetaAnteriorId` falta si estaba vacío. */
export interface CambioPlan {
  dia: number
  tipoComida: TipoComida
  recetaId: number
  recetaAnteriorId?: number | null
}

/** Respuesta de `/generar` y `/ajustar-presupuesto` (ADR-24). */
export interface PlanGenerado {
  plan: PlanSemanal
  cambios: CambioPlan[]
  costoEstimado: number
  presupuestoSemanal: number
  excedePresupuesto: boolean
  /** Productos por vencer que el plan aprovecha. */
  aprovecha: string[]
  /** Comidas del plan que suelen gustarles a los niños; 0 si en el hogar no hay niños. */
  paraNinos?: number
  /** Comidas que llevan un cambio o un plato aparte para alguien con alergia (ADR-24). */
  conAdaptaciones?: number
  /** Días de la semana en que recién le pagaron; ahí quedaron las comidas más costosas. */
  diasDespuesDelPago?: Dia[]
  /** Lo que falta comprar (RNF-02). */
  costoPorComprar?: number
}
