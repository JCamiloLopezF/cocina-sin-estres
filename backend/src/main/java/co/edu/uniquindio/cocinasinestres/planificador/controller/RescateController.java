package co.edu.uniquindio.cocinasinestres.planificador.controller;

import co.edu.uniquindio.cocinasinestres.comun.error.DatosInvalidosException;
import co.edu.uniquindio.cocinasinestres.planificador.dto.RescateResponse;
import co.edu.uniquindio.cocinasinestres.planificador.service.RescateService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Modo rescate (KAN-25): qué cocinar hoy sin comprar nada. */
@RestController
@RequestMapping("/api/rescate")
@PreAuthorize("hasRole('USUARIA')")
public class RescateController {

    private final RescateService rescateService;

    public RescateController(RescateService rescateService) {
        this.rescateService = rescateService;
    }

    /** {@code GET /api/rescate?maxMinutos=20}; sin {@code maxMinutos} no limita el tiempo. */
    @GetMapping
    public RescateResponse opciones(@RequestParam(name = "maxMinutos", required = false) Integer maxMinutos) {
        if (maxMinutos != null && (maxMinutos < 5 || maxMinutos > 240)) {
            throw new DatosInvalidosException("PLAN_DATOS_INVALIDOS", "El tiempo debe estar entre 5 y 240 minutos.");
        }
        return rescateService.opciones(maxMinutos);
    }
}
