package co.edu.uniquindio.cocinasinestres.curaduria.dto;

import java.math.BigDecimal;

/**
 * Un ingrediente de la receta con lo que el curador necesita ver del producto.
 *
 * @param activo el producto sigue en el catálogo; si no, conviene cambiarlo
 */
public record IngredienteCuradoResponse(
    Long productoId,
    String nombre,
    String categoria,
    String unidadBase,
    BigDecimal cantidad,
    boolean opcional,
    boolean principal,
    boolean activo) {
}
