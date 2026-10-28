package co.edu.uniquindio.cocinasinestres.curaduria.dto;

import java.math.BigDecimal;

/**
 * Un ingrediente de la receta.
 *
 * @param productoId producto activo del catálogo global
 * @param cantidad   en la unidad base del producto (g, ml o unidad), para las porciones de la receta
 * @param opcional   la receta se puede preparar sin él: no se compra ni se descuenta
 * @param principal  define el plato: si alguien es alérgico, no se reemplaza (le toca plato aparte)
 */
public record IngredienteCuradoRequest(Long productoId, BigDecimal cantidad, Boolean opcional, Boolean principal) {
}
