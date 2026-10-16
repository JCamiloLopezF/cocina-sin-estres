package co.edu.uniquindio.cocinasinestres.planificador.dto;

/**
 * Request para asignar/actualizar una comida en un plan (upsert).
 */
public record ComidaPlanAssignRequest(
    Integer dia,
    String tipoComida,
    Long recetaId,
    String idCambioCliente
) {
}
