package co.edu.uniquindio.cocinasinestres.recetario.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Ingrediente de una receta: un producto del catálogo global y la cantidad que pide
 * la receta, en la unidad base del producto.
 *
 * @param productoId    identificador del producto en el catálogo
 * @param cantidad      cantidad en la unidad base, para las porciones de la receta
 * @param opcional      si la receta se puede preparar sin este ingrediente
 * @param restricciones alergias y condiciones del producto (por ejemplo {@code lactosa})
 */
public record IngredienteRecetaResponse(
    Long productoId,
    BigDecimal cantidad,
    boolean opcional,
    List<String> restricciones
) {
}
