package co.edu.uniquindio.cocinasinestres.curaduria.dto;

/**
 * Cuerpo de {@code PUT /api/curaduria/recetas/{id}/etiquetas} (KAN-17). Lo que no se envía
 * no cambia.
 *
 * @param platoFuerte sirve como almuerzo o cena
 * @param gustaANinos en general les gusta a los niños
 */
public record EtiquetasRecetaRequest(Boolean platoFuerte, Boolean gustaANinos) {
}
