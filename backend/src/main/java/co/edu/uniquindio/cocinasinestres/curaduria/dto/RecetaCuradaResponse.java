package co.edu.uniquindio.cocinasinestres.curaduria.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Una receta como la edita el curador (RNF-28).
 *
 * @param costo       costo de la receta con los ingredientes obligatorios y los precios de
 *                    referencia, para las porciones de la receta
 * @param tieneVideo  ya tiene video (se carga en la pantalla de videos)
 */
public record RecetaCuradaResponse(
    Long id,
    String nombre,
    String descripcion,
    int tiempoMinutos,
    int porciones,
    List<String> pasos,
    String preparacionPrevia,
    List<IngredienteCuradoResponse> ingredientes,
    List<String> momentos,
    boolean platoFuerte,
    boolean gustaANinos,
    boolean activa,
    BigDecimal costo,
    boolean tieneVideo) {
}
