package co.edu.uniquindio.cocinasinestres.planificador.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Resultado de "Armar mi semana" y de "Ajustar al presupuesto" (ADR-24).
 *
 * @param plan               el plan como quedó
 * @param cambios            lo que el generador puso o cambió, en orden de la semana
 * @param costoEstimado      costo total del plan (misma fórmula que {@code /costo})
 * @param presupuestoSemanal presupuesto del hogar prorrateado a la semana (ADR-14)
 * @param excedePresupuesto  el plan sigue por encima del presupuesto (no había recetas
 *                           más baratas que encajaran)
 * @param aprovecha          productos por vencer que el plan usa
 * @param paraNinos          comidas del plan que suelen gustarles a los niños; 0 si en el
 *                           hogar no hay niños
 * @param conAdaptaciones    comidas del plan que llevan un cambio o un plato aparte para
 *                           alguien con alergia (ADR-24)
 * @param diasDespuesDelPago días de esta semana (1 = lunes … 7 = domingo) en que la usuaria
 *                           recién recibió el pago; ahí quedaron las comidas más costosas.
 *                           Vacío si la semana no tiene pago
 */
public record PlanGeneradoResponse(
    PlanSemanalResponse plan,
    List<CambioPlanResponse> cambios,
    BigDecimal costoEstimado,
    BigDecimal presupuestoSemanal,
    boolean excedePresupuesto,
    List<String> aprovecha,
    int paraNinos,
    int conAdaptaciones,
    List<Integer> diasDespuesDelPago) {
}
