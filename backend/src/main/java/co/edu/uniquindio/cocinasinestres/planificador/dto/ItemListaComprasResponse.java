package co.edu.uniquindio.cocinasinestres.planificador.dto;

import java.math.BigDecimal;

/**
 * Producto pendiente de comprar en la lista de un plan semanal.
 *
 * @param id            identificador del ítem en la lista
 * @param productoId    producto del catálogo que hay que comprar
 * @param cantidad      cantidad total que piden las recetas del plan, en unidad base
 * @param costoEstimado costo de esa cantidad con el precio de referencia del producto
 * @param costoReal     lo que la usuaria corrigió que pagó (KAN-26); {@code null} si no lo ha
 *                      corregido. El total de la lista usa este valor cuando existe
 * @param prioridad     prioridad de compra (1 es la más alta)
 * @param comprado      si la usuaria ya lo marcó como comprado
 * @param motivo        {@code PLAN} (lo piden las recetas), {@code REPONER} (quedó por debajo
 *                      del mínimo) o {@code ALERGIA} (reemplazo o plato aparte de alguien)
 * @param cantidadAgregada cuánto entró a la despensa al comprarlo (en presentaciones completas)
 */
public record ItemListaComprasResponse(
    Long id,
    Long productoId,
    BigDecimal cantidad,
    BigDecimal costoEstimado,
    BigDecimal costoReal,
    Integer prioridad,
    boolean comprado,
    String motivo,
    BigDecimal cantidadAgregada
) {
}
