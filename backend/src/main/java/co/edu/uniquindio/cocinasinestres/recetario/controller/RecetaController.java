package co.edu.uniquindio.cocinasinestres.recetario.controller;

import co.edu.uniquindio.cocinasinestres.recetario.dto.RecetaDetalleResponse;
import co.edu.uniquindio.cocinasinestres.recetario.dto.RecetaResponse;
import co.edu.uniquindio.cocinasinestres.recetario.service.RecetaService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Catálogo de recetas en modo lectura: cualquier cuenta autenticada puede
 * consultarlo (la cadena de seguridad ya cubre {@code /api/**}).
 */
@RestController
@RequestMapping("/api/recetas")
public class RecetaController {

    private final RecetaService recetaService;

    public RecetaController(RecetaService recetaService) {
        this.recetaService = recetaService;
    }

    /** Lista las recetas activas ordenadas por nombre. */
    @GetMapping
    public List<RecetaResponse> listar() {
        return recetaService.listar();
    }

    /** Devuelve el detalle de una receta. 404 si no existe o está desactivada. */
    @GetMapping("/{id}")
    public RecetaDetalleResponse obtenerPorId(@PathVariable Long id) {
        return recetaService.obtenerPorId(id);
    }
}
