package co.edu.uniquindio.cocinasinestres.planificador.dto;

import java.time.LocalDate;

/**
 * Request para crear/obtener un plan semanal (solo contiene la semanaInicio).
 */
public record PlanSemanalRequest(LocalDate semanaInicio) {
}
