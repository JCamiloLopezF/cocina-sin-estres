package co.edu.uniquindio.cocinasinestres.planificador.dto;

/**
 * Un hueco que el generador llenó o cambió.
 *
 * @param recetaAnteriorId receta que había antes; {@code null} si el hueco estaba vacío
 */
public record CambioPlanResponse(
    Integer dia,
    String tipoComida,
    Long recetaId,
    Long recetaAnteriorId) {
}
