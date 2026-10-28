package co.edu.uniquindio.cocinasinestres.curaduria.controller;

import co.edu.uniquindio.cocinasinestres.curaduria.dto.RecetaCuradaRequest;
import co.edu.uniquindio.cocinasinestres.curaduria.dto.RecetaCuradaResponse;
import co.edu.uniquindio.cocinasinestres.curaduria.dto.RecetaCuradaResumen;
import co.edu.uniquindio.cocinasinestres.curaduria.service.RecetaCuradaService;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * El curador crea y edita recetas sin tocar código (RNF-28). El video y las etiquetas se
 * siguen editando en {@link RecetaVideoController}.
 */
@RestController
@RequestMapping("/api/curaduria/recetas")
@PreAuthorize("hasRole('CURADOR')")
public class RecetaCuradaController {

    private final RecetaCuradaService recetaCuradaService;

    public RecetaCuradaController(RecetaCuradaService recetaCuradaService) {
        this.recetaCuradaService = recetaCuradaService;
    }

    /** Todas las recetas, activas y desactivadas, con su costo de referencia. */
    @GetMapping("/catalogo")
    public List<RecetaCuradaResumen> listar() {
        return recetaCuradaService.listar();
    }

    @GetMapping("/{id}")
    public RecetaCuradaResponse obtener(@PathVariable("id") Long id) {
        return recetaCuradaService.obtener(id);
    }

    @PostMapping
    public ResponseEntity<RecetaCuradaResponse> crear(@RequestBody RecetaCuradaRequest request) {
        RecetaCuradaResponse creada = recetaCuradaService.crear(request);
        return ResponseEntity.created(URI.create("/api/curaduria/recetas/" + creada.id())).body(creada);
    }

    @PutMapping("/{id}")
    public RecetaCuradaResponse actualizar(@PathVariable("id") Long id, @RequestBody RecetaCuradaRequest request) {
        return recetaCuradaService.actualizar(id, request);
    }

    /** La receta deja de ofrecerse; los planes que ya la tienen la conservan. */
    @DeleteMapping("/{id}")
    public RecetaCuradaResponse desactivar(@PathVariable("id") Long id) {
        return recetaCuradaService.desactivar(id);
    }

    @PostMapping("/{id}/reactivar")
    public RecetaCuradaResponse reactivar(@PathVariable("id") Long id) {
        return recetaCuradaService.reactivar(id);
    }
}
