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
 * @param restricciones     alergias y condiciones de los ingredientes obligatorios; los
 *                          opcionales se pueden omitir, así que no cuentan
 * @param video             cómo se hace, en video; {@code null} si todavía no tiene
 * @param momentos          comidas en que va: {@code DESAYUNO}, {@code ALMUERZO}, {@code CENA} (V17)
 * @param platoFuerte       es una comida; si no, es un acompañamiento o una bebida
 * @param gustaANinos       el curador la marcó como receta que suele gustarles a los niños
 */
public record RecetaDetalleResponse(
    Long id,
    String nombre,
    String descripcion,
    int tiempoMinutos,
    int porciones,
    String preparacionPrevia,
    List<String> pasos,
    List<IngredienteRecetaResponse> ingredientes,
    List<String> restricciones,
    VideoRecetaResponse video,
    List<String> momentos,
    boolean platoFuerte,
    boolean gustaANinos
) {
}
