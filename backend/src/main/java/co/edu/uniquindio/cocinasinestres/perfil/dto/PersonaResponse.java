package co.edu.uniquindio.cocinasinestres.perfil.dto;

import java.util.List;

/**
 * Una persona del hogar como la ve la usuaria dueña del perfil.
 *
 * @param restricciones  códigos de alergias y condiciones de esta persona, ya descifrados
 * @param alergiasGraves las de {@code restricciones} que son graves (ADR-24)
 */
public record PersonaResponse(
    Long id,
    String nombre,
    Integer edad,
    List<String> restricciones,
    List<String> alergiasGraves
) {
}
