package co.edu.uniquindio.cocinasinestres.planificador.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO: costo acumulado por día (7 elementos), presupuesto semanal y flag de excede.
 * Índice 0 = lunes ... índice 6 = domingo.
 */
public record PlanCostoResponse(
    List<BigDecimal> costoAcumuladoPorDia,
    BigDecimal presupuestoSemanal,
    boolean excedePresupuesto
) {}
