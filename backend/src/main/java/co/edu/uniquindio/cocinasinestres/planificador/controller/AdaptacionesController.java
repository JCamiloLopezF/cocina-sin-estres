package co.edu.uniquindio.cocinasinestres.planificador.controller;

import co.edu.uniquindio.cocinasinestres.planificador.dto.AdaptacionResponse;
import co.edu.uniquindio.cocinasinestres.planificador.service.GeneradorPlanService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Cómo adaptar recetas a las alergias de cada persona del hogar de la usuaria (ADR-24).
 * Lo usan el detalle de receta y el día del plan.
 */
@RestController
@RequestMapping("/api/adaptaciones")
@PreAuthorize("hasRole('USUARIA')")
public class AdaptacionesController {

    /** Tope de recetas por consulta: un plan tiene como mucho 21 comidas. */
    private static final int MAXIMO = 30;

    private final GeneradorPlanService generadorPlanService;

    public AdaptacionesController(GeneradorPlanService generadorPlanService) {
        this.generadorPlanService = generadorPlanService;
    }

    /** {@code GET /api/adaptaciones?recetas=1,2,3}. */
    @GetMapping
    public List<AdaptacionResponse> adaptaciones(@RequestParam("recetas") List<Long> recetas) {
        return generadorPlanService.adaptaciones(recetas.stream().distinct().limit(MAXIMO).toList());
    }
}
