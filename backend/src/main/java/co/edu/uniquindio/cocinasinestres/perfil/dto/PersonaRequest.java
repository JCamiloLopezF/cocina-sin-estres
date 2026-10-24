package co.edu.uniquindio.cocinasinestres.perfil.dto;

import java.util.List;

/**
 * Una persona del hogar.
 *
 * @param nombre        nombre opcional, hasta 60 caracteres
 * @param edad          edad entre 0 y 120
 * @param restricciones códigos de alergias y condiciones de esta persona (por ejemplo
 *                      {@code lactosa}); son datos de salud y solo se guardan con el
 *                      consentimiento del hogar (Ley 1581, ADR-20). Vacía o ausente si no tiene
 */
public record PersonaRequest(
    String nombre,
    Integer edad,
    List<String> restricciones
) {
}
