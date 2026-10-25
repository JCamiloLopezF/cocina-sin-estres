package co.edu.uniquindio.cocinasinestres.planificador.controller;

import co.edu.uniquindio.cocinasinestres.planificador.dto.ComidaPlanAssignRequest;
import co.edu.uniquindio.cocinasinestres.planificador.dto.ComidaPlanResponse;
import co.edu.uniquindio.cocinasinestres.planificador.dto.CompraRequest;
import co.edu.uniquindio.cocinasinestres.planificador.dto.CompraResponse;
import co.edu.uniquindio.cocinasinestres.planificador.dto.GenerarPlanRequest;
import co.edu.uniquindio.cocinasinestres.planificador.dto.ListaComprasResponse;
import co.edu.uniquindio.cocinasinestres.planificador.dto.OtraOpcionRequest;
import co.edu.uniquindio.cocinasinestres.planificador.dto.PlanCostoResponse;
import co.edu.uniquindio.cocinasinestres.planificador.dto.PlanGeneradoResponse;
import co.edu.uniquindio.cocinasinestres.planificador.dto.PlanSemanalRequest;
import co.edu.uniquindio.cocinasinestres.planificador.dto.PlanSemanalResponse;
import co.edu.uniquindio.cocinasinestres.planificador.service.GeneradorPlanService;
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
    private final GeneradorPlanService generadorPlanService;

    public PlanificadorController(
        PlanificadorService planificadorService,
        PlanificadorCostoService planificadorCostoService,
        GeneradorPlanService generadorPlanService) {
        this.planificadorService = planificadorService;
        this.planificadorCostoService = planificadorCostoService;
        this.generadorPlanService = generadorPlanService;
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

    /**
     * Marca un producto de la lista como comprado: entra a la despensa en presentaciones
     * completas y con su vencimiento estimado (ADR-24). Con {@code comprado: false} se
     * deshace. Idempotente.
     */
    @PutMapping("/{planId}/lista-compras/items/{itemId}")
    public CompraResponse marcarComprado(
        @PathVariable("planId") Long planId,
        @PathVariable("itemId") Long itemId,
        @RequestBody CompraRequest request) {
        return planificadorService.marcarComprado(planId, itemId, request != null && request.comprado());
    }

    /**
     * "Armar mi semana" (ADR-24): llena los huecos vacíos del almuerzo y la cena con
     * recetas aptas para el hogar, usando primero la despensa y cuidando el presupuesto.
     * Lo que ya está en el plan no se toca.
     */
    @PostMapping("/{planId}/generar")
    public PlanGeneradoResponse generar(
        @PathVariable("planId") Long planId,
        @RequestBody(required = false) GenerarPlanRequest request) {
        return generadorPlanService.generar(planId, request);
    }

    /** Cambia la receta de una comida por la siguiente mejor opción. 409 si no quedan. */
    @PostMapping("/{planId}/comidas/{comidaId}/otra-opcion")
    public ComidaPlanResponse otraOpcion(
        @PathVariable("planId") Long planId,
        @PathVariable("comidaId") Long comidaId,
        @RequestBody(required = false) OtraOpcionRequest request) {
        return generadorPlanService.otraOpcion(planId, comidaId, request);
    }

    /** Cambia las comidas no preparadas más caras por otras más baratas hasta caber en el presupuesto. */
    @PostMapping("/{planId}/ajustar-presupuesto")
    public PlanGeneradoResponse ajustarAlPresupuesto(@PathVariable("planId") Long planId) {
        return generadorPlanService.ajustarAlPresupuesto(planId);
    }

    /** La usuaria se queda con la semana que la app armó sola (ADR-24): deja de ser borrador. */
    @PutMapping("/{planId}/confirmar")
    public PlanSemanalResponse confirmarBorrador(@PathVariable("planId") Long planId) {
        return planificadorService.confirmarBorrador(planId);
    }
}
