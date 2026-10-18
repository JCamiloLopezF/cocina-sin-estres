package co.edu.uniquindio.cocinasinestres.planificador.controller;

import co.edu.uniquindio.cocinasinestres.planificador.dto.ComidaPlanAssignRequest;
import co.edu.uniquindio.cocinasinestres.planificador.dto.ComidaPlanResponse;
import co.edu.uniquindio.cocinasinestres.planificador.dto.ListaComprasResponse;
import co.edu.uniquindio.cocinasinestres.planificador.dto.PlanCostoResponse;
import co.edu.uniquindio.cocinasinestres.planificador.dto.PlanSemanalRequest;
import co.edu.uniquindio.cocinasinestres.planificador.dto.PlanSemanalResponse;
import co.edu.uniquindio.cocinasinestres.planificador.service.PlanificadorCostoService;
import co.edu.uniquindio.cocinasinestres.planificador.service.PlanificadorService;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador para manejar planes semanales y asignación de recetas a día/comida.
 */
@RestController
@RequestMapping("/api/planes-semanales")
@PreAuthorize("hasRole('USUARIA')")
public class PlanificadorController {

    private final PlanificadorService planificadorService;
    private final PlanificadorCostoService planificadorCostoService;

    public PlanificadorController(
        PlanificadorService planificadorService,
        PlanificadorCostoService planificadorCostoService) {
        this.planificadorService = planificadorService;
        this.planificadorCostoService = planificadorCostoService;
    }

    @PostMapping
    public ResponseEntity<PlanSemanalResponse> crearPlan(@RequestBody PlanSemanalRequest request) {
        PlanSemanalResponse resp = planificadorService.crearPlanSiNoExiste(request.semanaInicio());
        return ResponseEntity.ok(resp);
    }

    @GetMapping
    public ResponseEntity<PlanSemanalResponse> obtenerPorSemana(
        @RequestParam("semanaInicio") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate semanaInicio) {
        PlanSemanalResponse resp = planificadorService.obtenerPorSemana(semanaInicio);
        return ResponseEntity.ok(resp);
    }

    @PutMapping("/{planId}/comidas")
    public ResponseEntity<ComidaPlanResponse> asignarComida(
        @PathVariable("planId") Long planId,
        @RequestBody ComidaPlanAssignRequest request) {
        ComidaPlanResponse resp = planificadorService.asignarComida(planId, request);
        return ResponseEntity.ok(resp);
    }

    @DeleteMapping("/{planId}/comidas/{comidaId}")
    public ResponseEntity<Void> eliminarComida(
        @PathVariable("planId") Long planId,
        @PathVariable("comidaId") Long comidaId) {
        planificadorService.eliminarComida(planId, comidaId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{planId}/comidas/{comidaId}/marcar-preparada")
    public ResponseEntity<ComidaPlanResponse> marcarPreparada(
        @PathVariable("planId") Long planId,
        @PathVariable("comidaId") Long comidaId) {
        ComidaPlanResponse resp = planificadorService.marcarPreparada(planId, comidaId);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/{planId}/costo")
    public ResponseEntity<PlanCostoResponse> consultarCosto(
        @PathVariable("planId") Long planId) {
        PlanCostoResponse resp = planificadorCostoService.calcularCosto(planId);
        return ResponseEntity.ok(resp);
    }

    /**
     * Lista de compras generada del plan con su costo total. Si el plan no tiene
     * comidas aún, responde con la lista vacía y costo cero.
     */
    @GetMapping("/{planId}/lista-compras")
    public ListaComprasResponse consultarListaCompras(
        @PathVariable("planId") Long planId) {
        return planificadorService.consultarListaCompras(planId);
    }
}
