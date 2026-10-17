package co.edu.uniquindio.cocinasinestres.planificador.service;

/**
 * Servicio para generar la lista de compras a partir de un plan semanal.
 */
public interface ListaComprasService {

    /**
     * Genera o reemplaza la lista de compras para el plan indicado.
     *
     * @param planSemanalId identificador del plan semanal
     */
    void generarListaCompras(Long planSemanalId);
}
