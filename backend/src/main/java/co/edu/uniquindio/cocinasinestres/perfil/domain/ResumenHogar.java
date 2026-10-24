package co.edu.uniquindio.cocinasinestres.perfil.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

/**
 * Lo que otros módulos necesitan saber del hogar para decidir por la usuaria, ya
 * descifrado: el planificador lo usa para armar la semana (ADR-24).
 *
 * @param hogarId            hogar de la usuaria
 * @param restricciones      códigos de alergias y condiciones de todas las personas
 *                           (por ejemplo {@code lactosa}, {@code diabetes})
 * @param diasSinTiempo      días sin tiempo para cocinar, 1 = lunes … 7 = domingo
 * @param presupuestoSemanal presupuesto prorrateado a una semana (ADR-14)
 * @param hayNinos           vive en el hogar al menos una persona de
 *                           {@value #EDAD_MAXIMA_NINO} años o menos (niño según la Ley 1098 de 2006)
 * @param personas           cada persona con sus alergias, para adaptar recetas (ADR-24)
 */
public record ResumenHogar(
    Long hogarId,
    Set<String> restricciones,
    Set<Integer> diasSinTiempo,
    BigDecimal presupuestoSemanal,
    boolean hayNinos,
    List<PersonaResumen> personas) {

    /** Hasta esta edad, inclusive, una persona cuenta como niño. */
    public static final int EDAD_MAXIMA_NINO = 12;

    public ResumenHogar {
        restricciones = Set.copyOf(restricciones);
        diasSinTiempo = Set.copyOf(diasSinTiempo);
        personas = List.copyOf(personas);
    }

    /** Un hogar del que no se necesita el detalle por persona. */
    public ResumenHogar(
        Long hogarId, Set<String> restricciones, Set<Integer> diasSinTiempo,
        BigDecimal presupuestoSemanal, boolean hayNinos) {
        this(hogarId, restricciones, diasSinTiempo, presupuestoSemanal, hayNinos, List.of());
    }
}
