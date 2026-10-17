package co.edu.uniquindio.cocinasinestres.planificador.service;

import co.edu.uniquindio.cocinasinestres.planificador.dto.ComidaPlanAssignRequest;
import co.edu.uniquindio.cocinasinestres.planificador.dto.ComidaPlanResponse;
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
}
