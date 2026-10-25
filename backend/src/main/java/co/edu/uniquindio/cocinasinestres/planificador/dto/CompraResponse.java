package co.edu.uniquindio.cocinasinestres.planificador.dto;

import java.time.LocalDate;

/**
 * Resultado de marcar un producto de la lista como comprado o deshacerlo (ADR-24).
 *
 * @param item    el ítem como quedó
 * @param venceEn vencimiento con que quedó en la despensa; {@code null} si no se conoce o
 *                si se deshizo la compra
 */
public record CompraResponse(ItemListaComprasResponse item, LocalDate venceEn) {
}
