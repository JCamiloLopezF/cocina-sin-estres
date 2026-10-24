package co.edu.uniquindio.cocinasinestres.planificador.dto;

/**
 * Cuerpo de {@code PUT /api/planes-semanales/{id}/lista-compras/items/{itemId}}.
 *
 * @param comprado {@code true} para marcarlo como comprado (entra a la despensa);
 *                 {@code false} para deshacerlo (sale de la despensa)
 */
public record CompraRequest(boolean comprado) {
}
