package co.edu.uniquindio.cocinasinestres.planificador.dto;

import java.math.BigDecimal;

/**
 * Producto pendiente de comprar en la lista de un plan semanal.
 *
 * @param id            identificador del ítem en la lista
 * @param productoId    producto del catálogo que hay que comprar
 * @param cantidad      cantidad total que piden las recetas del plan, en unidad base
 * @param costoEstimado costo de esa cantidad con el precio de referencia del producto
 * @param prioridad     prioridad de compra (1 es la más alta)
 * @param comprado      si la usuaria ya lo marcó como comprado
 */
public record ItemListaComprasResponse(
    Long id,
    Long productoId,
    BigDecimal cantidad,
    BigDecimal costoEstimado,
    Integer prioridad,
    boolean comprado
) {
}
