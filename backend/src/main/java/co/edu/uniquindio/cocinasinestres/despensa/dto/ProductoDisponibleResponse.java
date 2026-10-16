package co.edu.uniquindio.cocinasinestres.despensa.dto;

import co.edu.uniquindio.cocinasinestres.curaduria.domain.TipoCantidad;
import co.edu.uniquindio.cocinasinestres.curaduria.domain.UnidadBase;
import java.math.BigDecimal;

/**
 * Un producto del catálogo que la usuaria puede agregar a su despensa (pantalla 16).
 *
 * @param id                   identificador del producto
 * @param nombre               nombre del producto
 * @param categoria            categoría del producto
 * @param tipoCantidad         CONTABLE (se cuenta) o GRANEL (se registra por nivel)
 * @param unidadBase           unidad en la que se expresan las cantidades
 * @param unidadCompra         presentación de compra del precio de referencia
 * @param cantidadUnidadCompra cuánto trae la unidad de compra, en unidad base
 * @param precioUnidadCompra   precio de referencia de la unidad de compra, en pesos
 * @param itemDespensaId       registro de la despensa si el producto ya está en ella;
 *                             ausente si todavía no
 */
public record ProductoDisponibleResponse(
    Long id,
    String nombre,
    String categoria,
    TipoCantidad tipoCantidad,
    UnidadBase unidadBase,
    String unidadCompra,
    BigDecimal cantidadUnidadCompra,
    BigDecimal precioUnidadCompra,
    Long itemDespensaId
) {
}
