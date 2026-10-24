package co.edu.uniquindio.cocinasinestres.planificador.dto;

/**
 * "La preparé" fuera del plan (Modo cocinar desde el recetario).
 *
 * @param recetaId receta que se cocinó
 */
public record PreparacionRequest(Long recetaId) {
}
