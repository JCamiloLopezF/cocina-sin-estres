package co.edu.uniquindio.cocinasinestres.despensa.dto;

/**
 * Los números de la tarjeta "Despensa" de la pantalla de inicio. Cada cifra coincide con
 * la cantidad de resultados de la pestaña del mismo nombre en el listado.
 *
 * @param total      productos en la despensa
 * @param porVencer  productos que vencen pronto o ya vencieron
 * @param bajoMinimo productos en su mínimo o por debajo
 */
public record ResumenDespensaResponse(int total, int porVencer, int bajoMinimo) {
}
