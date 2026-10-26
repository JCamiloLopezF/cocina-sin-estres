package co.edu.uniquindio.cocinasinestres.planificador.dto;

import java.math.BigDecimal;

/**
 * Cuerpo de {@code PUT /api/planes-semanales/{id}/lista-compras/items/{itemId}/costo} (KAN-26).
 *
 * @param costoReal lo que la usuaria pagó de verdad, en pesos; {@code null} para volver al
 *                  costo estimado
 */
public record CostoItemRequest(BigDecimal costoReal) {
}
