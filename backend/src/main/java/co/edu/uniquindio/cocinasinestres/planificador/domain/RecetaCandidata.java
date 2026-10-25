package co.edu.uniquindio.cocinasinestres.planificador.domain;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

/**
 * Una receta como la ve el generador del plan (ADR-24): cuánto cuesta, qué gasta y
 * a quién no le sirve.
 *
 * @param id             receta
 * @param tiempoMinutos  tiempo total de preparación
 * @param costo          costo de la receta completa con sus ingredientes obligatorios,
 *                       con la misma fórmula del costo del plan (ADR-14)
 * @param ingredientes   cantidad obligatoria por producto, en su unidad base
 * @param alergias       códigos de tipo ALERGIA de sus ingredientes obligatorios
 * @param condiciones    códigos de tipo CONDICION de sus ingredientes obligatorios
 * @param activa         sigue en el catálogo
 * @param platoFuerte    sirve como almuerzo o cena (V7)
 * @param gustaANinos    el curador la marcó como receta que en general les gusta a los niños (V7)
 * @param principales    ingredientes que definen el plato (V8): si alguien es alérgico a uno,
 *                       no se reemplaza; le toca plato aparte
 */
public record RecetaCandidata(
    Long id,
    int tiempoMinutos,
    BigDecimal costo,
    Map<Long, BigDecimal> ingredientes,
    Set<String> alergias,
    Set<String> condiciones,
    boolean activa,
    boolean platoFuerte,
    boolean gustaANinos,
    Set<Long> principales) {

    public RecetaCandidata {
        costo = costo == null ? BigDecimal.ZERO : costo;
        ingredientes = Map.copyOf(ingredientes);
        alergias = Set.copyOf(alergias);
        condiciones = Set.copyOf(condiciones);
        principales = Set.copyOf(principales);
    }

    /** Una receta sin ingredientes principales marcados. */
    public RecetaCandidata(
        Long id, int tiempoMinutos, BigDecimal costo, Map<Long, BigDecimal> ingredientes,
        Set<String> alergias, Set<String> condiciones, boolean activa, boolean platoFuerte, boolean gustaANinos) {
        this(id, tiempoMinutos, costo, ingredientes, alergias, condiciones, activa, platoFuerte, gustaANinos, Set.of());
    }
}
