package co.edu.uniquindio.cocinasinestres.planificador.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO: costo acumulado por día (7 elementos), presupuesto semanal y flag de excede.
 * Índice 0 = lunes ... índice 6 = domingo.
 *
 * @param costoAlergias lo que suma al total el reemplazo o el plato aparte de quien tiene
 *                      alergia (KAN-24); ya está incluido en el acumulado
 */
public record PlanCostoResponse(
    List<BigDecimal> costoAcumuladoPorDia,
    BigDecimal presupuestoSemanal,
    boolean excedePresupuesto,
    BigDecimal costoAlergias
) {}
