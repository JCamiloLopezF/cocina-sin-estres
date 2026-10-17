package co.edu.uniquindio.cocinasinestres.planificador.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Respuesta con el plan semanal y sus comidas.
 */
public record PlanSemanalResponse(
    Long id,
    LocalDate semanaInicio,
    LocalDateTime creadoEn,
    List<ComidaPlanResponse> comidas
) {
}
