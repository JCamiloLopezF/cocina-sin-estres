package co.edu.uniquindio.cocinasinestres.planificador.service;

import co.edu.uniquindio.cocinasinestres.planificador.dto.ListaComprasResponse;

/**
 * Servicio para generar y consultar la lista de compras de un plan semanal.
 */
public interface ListaComprasService {

    /**
     * Genera o reemplaza la lista de compras para el plan indicado.
     *
     * @param planSemanalId identificador del plan semanal
     */
    void generarListaCompras(Long planSemanalId);

    /**
     * Lee la lista de compras ya generada para el plan, con su costo total.
     * Si el plan todavía no tiene comidas, devuelve una lista vacía con costo cero.
     *
     * @param planSemanalId identificador del plan semanal
     */
    ListaComprasResponse consultar(Long planSemanalId);
}
