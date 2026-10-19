package co.edu.uniquindio.cocinasinestres.recetario.dto;

/**
 * Resumen de una receta tal como lo lista la API.
 *
 * <p>No lleva pasos ni ingredientes: el listado es para elegir recetas. El detalle
 * está en {@link RecetaDetalleResponse}.</p>
 *
 * @param id            identificador de la receta
 * @param nombre        nombre de la receta
 * @param descripcion   descripción breve, puede ser {@code null}
 * @param tiempoMinutos tiempo total de preparación en minutos
 * @param porciones     porciones para las que están calculadas las cantidades
 */
public record RecetaResponse(
    Long id,
    String nombre,
    String descripcion,
    int tiempoMinutos,
    int porciones
) {
}
