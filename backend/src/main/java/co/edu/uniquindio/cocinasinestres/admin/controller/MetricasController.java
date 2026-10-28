package co.edu.uniquindio.cocinasinestres.admin.controller;

import co.edu.uniquindio.cocinasinestres.admin.dto.MedicionInicioSesionRequest;
import co.edu.uniquindio.cocinasinestres.admin.dto.MedicionPlanificacionRequest;
import co.edu.uniquindio.cocinasinestres.admin.service.MetricasService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Mediciones que manda la web para el Plan de Calidad (KAN-54). Cualquier cuenta con sesión
 * las manda sobre sí misma; solo el administrador ve el resultado, agregado, en el análisis.
 */
@RestController
@RequestMapping("/api/metricas")
public class MetricasController {

    private final MetricasService metricasService;

    public MetricasController(MetricasService metricasService) {
        this.metricasService = metricasService;
    }

    @PostMapping("/inicio-sesion")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void inicioSesion(@RequestBody MedicionInicioSesionRequest request) {
        metricasService.inicioSesion(request);
    }

    @PostMapping("/planificacion")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void planificacion(@RequestBody MedicionPlanificacionRequest request) {
        metricasService.planificacion(request);
    }
}
