package co.edu.uniquindio.cocinasinestres.admin.controller;

import co.edu.uniquindio.cocinasinestres.admin.dto.AnalisisResponse;
import co.edu.uniquindio.cocinasinestres.admin.service.AnalisisService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Análisis del administrador (KAN-54). Solo el rol ADMIN: lo exige la regla de
 * {@code /api/admin/**} y también esta anotación (RNF-23). Devuelve cifras agregadas,
 * nunca datos de una usuaria en particular.
 */
@RestController
@RequestMapping("/api/admin/analisis")
@PreAuthorize("hasRole('ADMIN')")
public class AnalisisController {

    private final AnalisisService analisisService;

    public AnalisisController(AnalisisService analisisService) {
        this.analisisService = analisisService;
    }

    @GetMapping
    public AnalisisResponse analizar() {
        return analisisService.analizar();
    }
}
