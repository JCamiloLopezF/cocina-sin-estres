package co.edu.uniquindio.cocinasinestres.planificador.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
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
 * @param momentos       comidas en que va la receta (V17): {@code DESAYUNO}, {@code ALMUERZO}
 *                       o {@code CENA}. El plan solo la pone en esas comidas
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
    Set<Long> principales,
    Set<String> momentos) {

    /** Comidas de un plato fuerte cuando no se dice otra cosa: el almuerzo y la cena. */
    public static final Set<String> ALMUERZO_Y_CENA = Set.of("ALMUERZO", "CENA");

    public RecetaCandidata {
        costo = costo == null ? BigDecimal.ZERO : costo;
        ingredientes = Map.copyOf(ingredientes);
        alergias = Set.copyOf(alergias);
        condiciones = Set.copyOf(condiciones);
        principales = Set.copyOf(principales);
        momentos = Set.copyOf(momentos);
    }

    /** Receta sin momentos explícitos: un plato fuerte va al almuerzo y la cena. */
    public RecetaCandidata(
        Long id, int tiempoMinutos, BigDecimal costo, Map<Long, BigDecimal> ingredientes,
        Set<String> alergias, Set<String> condiciones, boolean activa, boolean platoFuerte, boolean gustaANinos,
        Set<Long> principales) {
        this(id, tiempoMinutos, costo, ingredientes, alergias, condiciones, activa, platoFuerte, gustaANinos,
            principales, platoFuerte ? ALMUERZO_Y_CENA : Set.of());
    }

    /** Una receta sin ingredientes principales marcados. */
    public RecetaCandidata(
        Long id, int tiempoMinutos, BigDecimal costo, Map<Long, BigDecimal> ingredientes,
        Set<String> alergias, Set<String> condiciones, boolean activa, boolean platoFuerte, boolean gustaANinos) {
        this(id, tiempoMinutos, costo, ingredientes, alergias, condiciones, activa, platoFuerte, gustaANinos, Set.of());
    }

    /**
     * La receta puede ir sola en esa comida: es plato fuerte y es de ese momento. Un
     * almuerzo es un plato completo (V17), así que unos huevos pericos no lo son.
     */
    public boolean sirvePara(String tipoComida) {
        return platoFuerte && momentos.contains(tipoComida);
    }

    /**
     * La misma receta para otro número de porciones: cantidades y costo multiplicados por
     * {@code factor} (comensales del hogar / porciones de la receta). Con factor 1, la misma.
     */
    public RecetaCandidata escalada(BigDecimal factor) {
        if (factor == null || factor.signum() <= 0 || factor.compareTo(BigDecimal.ONE) == 0) {
            return this;
        }
        Map<Long, BigDecimal> cantidades = new HashMap<>();
        ingredientes.forEach((producto, cantidad) ->
            cantidades.put(producto, cantidad.multiply(factor).setScale(3, RoundingMode.HALF_UP)));
        return new RecetaCandidata(id, tiempoMinutos, costo.multiply(factor).setScale(2, RoundingMode.HALF_UP),
            cantidades, alergias, condiciones, activa, platoFuerte, gustaANinos, principales, momentos);
    }
}
