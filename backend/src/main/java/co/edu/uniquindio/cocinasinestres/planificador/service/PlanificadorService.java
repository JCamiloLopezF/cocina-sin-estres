package co.edu.uniquindio.cocinasinestres.planificador.service;

import co.edu.uniquindio.cocinasinestres.planificador.dto.ComidaPlanAssignRequest;
import co.edu.uniquindio.cocinasinestres.planificador.dto.ComidaPlanResponse;
import co.edu.uniquindio.cocinasinestres.planificador.dto.CompraResponse;
import co.edu.uniquindio.cocinasinestres.planificador.dto.ListaComprasResponse;
import co.edu.uniquindio.cocinasinestres.planificador.dto.PlanSemanalResponse;
import java.time.LocalDate;

/**
 * Interfaz del servicio de planificador.
 */
public interface PlanificadorService {

    PlanSemanalResponse crearPlanSiNoExiste(LocalDate semanaInicio);

    PlanSemanalResponse obtenerPorSemana(LocalDate semanaInicio);

    ComidaPlanResponse asignarComida(Long planId, ComidaPlanAssignRequest request);

    void eliminarComida(Long planId, Long comidaId);

    ComidaPlanResponse marcarPreparada(Long planId, Long comidaId);

    /**
     * Lista de compras generada del plan, con su costo total. El plan debe ser del
     * hogar de la usuaria autenticada; si no existe o es de otro hogar responde 404.
     *
     * @param planId identificador del plan semanal
     */
    ListaComprasResponse consultarListaCompras(Long planId);

    /**
     * Marca un producto de la lista como comprado (entra a la despensa) o lo deshace
     * (ADR-24). 404 si el plan o el ítem no son del hogar de la usuaria.
     */
    CompraResponse marcarComprado(Long planId, Long itemId, boolean comprado);
}
