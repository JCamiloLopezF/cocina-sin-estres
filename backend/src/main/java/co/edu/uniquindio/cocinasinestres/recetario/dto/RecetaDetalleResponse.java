package co.edu.uniquindio.cocinasinestres.recetario.dto;

import java.util.List;

/**
 * Receta completa tal como la devuelve el detalle de la API.
 *
 * @param id                identificador de la receta
 * @param nombre            nombre de la receta
 * @param descripcion       descripción breve, puede ser {@code null}
 * @param tiempoMinutos     tiempo total de preparación en minutos
 * @param porciones         porciones para las que están calculadas las cantidades
 * @param preparacionPrevia indicación previa (p. ej. remojar), puede ser {@code null}
 * @param pasos             un paso por línea, ya separados y sin numerar
 * @param ingredientes      productos del catálogo que pide la receta
 */
public record RecetaDetalleResponse(
    Long id,
    String nombre,
    String descripcion,
    int tiempoMinutos,
    int porciones,
    String preparacionPrevia,
    List<String> pasos,
    List<IngredienteRecetaResponse> ingredientes
) {
}
