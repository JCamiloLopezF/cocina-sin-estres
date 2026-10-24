package co.edu.uniquindio.cocinasinestres.perfil.domain;

import java.util.Set;

/**
 * Una persona del hogar como la necesita el planificador para adaptar recetas
 * (ADR-24), con sus datos de salud ya descifrados.
 *
 * @param nombre         cómo llamarla en los avisos ("Samuel"; "ti" si es la usuaria sin
 *                       nombre; "la persona 2" si no tiene nombre)
 * @param restricciones  sus alergias y condiciones
 * @param alergiasGraves las de {@code restricciones} que son graves
 */
public record PersonaResumen(
    Long id,
    String nombre,
    int edad,
    Set<String> restricciones,
    Set<String> alergiasGraves) {

    public PersonaResumen {
        restricciones = Set.copyOf(restricciones);
        alergiasGraves = Set.copyOf(alergiasGraves);
    }
}
