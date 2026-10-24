package co.edu.uniquindio.cocinasinestres.planificador.domain;

import java.util.Set;

/**
 * Una persona que come en casa, con lo que el generador necesita para adaptarle una
 * receta (ADR-24).
 *
 * @param nombre         cómo llamarla en el aviso ("Samuel", "ti")
 * @param restricciones  sus alergias y condiciones (códigos)
 * @param alergiasGraves las alergias que descartan la receta para toda la casa
 */
public record Comensal(String nombre, Set<String> restricciones, Set<String> alergiasGraves) {

    public Comensal {
        restricciones = Set.copyOf(restricciones);
        alergiasGraves = Set.copyOf(alergiasGraves);
    }
}
