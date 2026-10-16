package co.edu.uniquindio.cocinasinestres.curaduria.domain;

import java.math.BigDecimal;

/**
 * Copia inmutable de los campos auditables de un producto en un momento dado.
 *
 * <p>Permite comparar el antes y el después sin depender de la entidad JPA.</p>
 */
public record InstantaneaProducto(
    String nombre,
    String categoria,
    TipoCantidad tipoCantidad,
    UnidadBase unidadBase,
    String unidadCompra,
    BigDecimal cantidadUnidadCompra,
    BigDecimal precioUnidadCompra,
    BigDecimal capacidadReferencia,
    boolean esBasico,
    boolean activo
) {
}
