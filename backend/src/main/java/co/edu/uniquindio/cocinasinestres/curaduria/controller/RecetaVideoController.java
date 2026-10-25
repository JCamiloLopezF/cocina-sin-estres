package co.edu.uniquindio.cocinasinestres.curaduria.controller;

import co.edu.uniquindio.cocinasinestres.curaduria.dto.RecetaVideoRequest;
import co.edu.uniquindio.cocinasinestres.curaduria.dto.RecetaVideoResponse;
import co.edu.uniquindio.cocinasinestres.curaduria.service.RecetaVideoService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** El curador carga el video de cada receta (máximo la mitad del tiempo de la receta). */
@RestController
@RequestMapping("/api/curaduria/recetas")
@PreAuthorize("hasRole('CURADOR')")
public class RecetaVideoController {

    private final RecetaVideoService recetaVideoService;

    public RecetaVideoController(RecetaVideoService recetaVideoService) {
        this.recetaVideoService = recetaVideoService;
    }

    @GetMapping
    public List<RecetaVideoResponse> listar() {
        return recetaVideoService.listar();
    }

    @PutMapping("/{id}/video")
    public RecetaVideoResponse guardar(@PathVariable("id") Long id, @RequestBody RecetaVideoRequest request) {
        return recetaVideoService.guardar(id, request);
    }

    @DeleteMapping("/{id}/video")
    public RecetaVideoResponse quitar(@PathVariable("id") Long id) {
        return recetaVideoService.quitar(id);
    }
}
