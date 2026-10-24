package co.edu.uniquindio.cocinasinestres.planificador.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Respuesta con el plan semanal y sus comidas.
 *
 * @param borrador la armó la app sola y la usuaria todavía no la confirma (ADR-24, V11)
 */
public record PlanSemanalResponse(
    Long id,
    LocalDate semanaInicio,
    LocalDateTime creadoEn,
    List<ComidaPlanResponse> comidas,
    boolean borrador
) {
}
