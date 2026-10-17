package co.edu.uniquindio.cocinasinestres.planificador.dto;

import java.time.LocalDateTime;

/**
 * Respuesta que describe una comida dentro del plan.
 */
public record ComidaPlanResponse(
    Long id,
    Long planSemanalId,
    Integer dia,
    String tipoComida,
    Long recetaId,
    boolean preparada,
    LocalDateTime preparadaEn,
    String idCambioCliente
) {
}
