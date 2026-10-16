package co.edu.uniquindio.cocinasinestres.planificador.controller;

import co.edu.uniquindio.cocinasinestres.planificador.dto.ComidaPlanAssignRequest;
import co.edu.uniquindio.cocinasinestres.planificador.dto.ComidaPlanResponse;
import co.edu.uniquindio.cocinasinestres.planificador.dto.PlanSemanalRequest;
import co.edu.uniquindio.cocinasinestres.planificador.dto.PlanSemanalResponse;
import co.edu.uniquindio.cocinasinestres.planificador.service.PlanificadorService;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador para manejar planes semanales y asignación de recetas a día/comida.
 */
@RestController
@RequestMapping("/api/plan-semanal")
public class PlanificadorController {

    private final PlanificadorService planificadorService;

    public PlanificadorController(PlanificadorService planificadorService) {
        this.planificadorService = planificadorService;
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

    @PutMapping("/{planId}/comida")
    public ResponseEntity<ComidaPlanResponse> asignarComida(
        @PathVariable("planId") Long planId,
        @RequestBody ComidaPlanAssignRequest request) {
        ComidaPlanResponse resp = planificadorService.asignarComida(planId, request);
        return ResponseEntity.ok(resp);
    }

    @DeleteMapping("/{planId}/comida/{comidaId}")
    public ResponseEntity<Void> eliminarComida(
        @PathVariable("planId") Long planId,
        @PathVariable("comidaId") Long comidaId) {
        planificadorService.eliminarComida(planId, comidaId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{planId}/comida/{comidaId}/marcar-preparada")
    public ResponseEntity<ComidaPlanResponse> marcarPreparada(
        @PathVariable("planId") Long planId,
        @PathVariable("comidaId") Long comidaId) {
        ComidaPlanResponse resp = planificadorService.marcarPreparada(planId, comidaId);
        return ResponseEntity.ok(resp);
    }
}
