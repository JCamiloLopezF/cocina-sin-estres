package co.edu.uniquindio.cocinasinestres.planificador.domain;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Lo que se necesita para adaptar una receta a las alergias de cada persona (ADR-24).
 *
 * @param comensales  las personas del hogar con sus alergias
 * @param alergenos   alérgenos de cada producto del catálogo
 * @param reemplazos  por qué productos se puede cambiar cada uno (tabla
 *                    {@code reemplazo_producto}, solo los activos)
 */
public record ReglasAlergia(
    List<Comensal> comensales,
    Map<Long, Set<String>> alergenos,
    Map<Long, Set<Long>> reemplazos) {

    /** Sin información por persona: las restricciones del hogar descartan la receta, como antes. */
    public static final ReglasAlergia SIN_DETALLE = new ReglasAlergia(List.of(), Map.of(), Map.of());

    public ReglasAlergia {
        comensales = List.copyOf(comensales);
        alergenos = Map.copyOf(alergenos);
        reemplazos = Map.copyOf(reemplazos);
    }

    /** Hay al menos una persona con alergias o condiciones registradas. */
    public boolean tieneDetalle() {
        return comensales.stream().anyMatch(c -> !c.restricciones().isEmpty());
    }
}
