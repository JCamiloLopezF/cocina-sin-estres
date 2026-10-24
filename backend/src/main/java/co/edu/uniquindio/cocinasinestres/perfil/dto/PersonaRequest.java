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
 * @param alergiasGraves alergias de {@code restricciones} que son graves ("no puede ni
 *                      tocarlo"): el plan nunca propone una receta que las lleve, ni
 *                      adaptada (ADR-24). Vacía o ausente si ninguna lo es
 */
public record PersonaRequest(
    String nombre,
    Integer edad,
    List<String> restricciones,
    List<String> alergiasGraves
) {

    /** Una persona sin alergias graves. */
    public PersonaRequest(String nombre, Integer edad, List<String> restricciones) {
        this(nombre, edad, restricciones, List.of());
    }
}
