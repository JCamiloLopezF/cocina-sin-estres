package co.edu.uniquindio.cocinasinestres.curaduria.controller;

import co.edu.uniquindio.cocinasinestres.curaduria.dto.ReemplazoResponse;
import co.edu.uniquindio.cocinasinestres.curaduria.service.ReemplazosService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Reemplazos de un producto para adaptar recetas a una alergia (ADR-24). La aplicación
 * los propone sola; aquí el curador los corrige.
 */
@RestController
@RequestMapping("/api/curaduria/productos/{productoId}/reemplazos")
@PreAuthorize("hasRole('CURADOR')")
public class ReemplazoController {

    private final ReemplazosService reemplazosService;

    public ReemplazoController(ReemplazosService reemplazosService) {
        this.reemplazosService = reemplazosService;
    }

    @GetMapping
    public List<ReemplazoResponse> listar(@PathVariable("productoId") Long productoId) {
        return reemplazosService.listar(productoId);
    }

    /** Agrega el reemplazo o lo vuelve a usar. Idempotente. */
    @PutMapping("/{reemplazoId}")
    public List<ReemplazoResponse> usar(
        @PathVariable("productoId") Long productoId,
        @PathVariable("reemplazoId") Long reemplazoId) {
        return reemplazosService.usar(productoId, reemplazoId);
    }

    /** Deja de usarlo; la aplicación no lo vuelve a proponer. Idempotente. */
    @DeleteMapping("/{reemplazoId}")
    public List<ReemplazoResponse> quitar(
        @PathVariable("productoId") Long productoId,
        @PathVariable("reemplazoId") Long reemplazoId) {
        return reemplazosService.quitar(productoId, reemplazoId);
    }
}
