package co.edu.uniquindio.cocinasinestres.avisos.controller;

import co.edu.uniquindio.cocinasinestres.avisos.dto.AvisoResponse;
import co.edu.uniquindio.cocinasinestres.avisos.dto.PreferenciaAvisoRequest;
import co.edu.uniquindio.cocinasinestres.avisos.dto.PreferenciaAvisoResponse;
import co.edu.uniquindio.cocinasinestres.avisos.service.AvisosService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Motor y centro de avisos (ADR-17, KAN-27). */
@RestController
@RequestMapping("/api/avisos")
@PreAuthorize("hasRole('USUARIA')")
public class AvisosController {

    private final AvisosService avisosService;

    public AvisosController(AvisosService avisosService) {
        this.avisosService = avisosService;
    }

    /** Evalúa las reglas al abrir la aplicación y devuelve hasta 5 avisos sin leer. */
    @GetMapping
    public List<AvisoResponse> evaluar() {
        return avisosService.evaluar();
    }

    /** Centro de avisos: los de los últimos 30 días, leídos o no. */
    @GetMapping("/recientes")
    public List<AvisoResponse> recientes() {
        return avisosService.recientes();
    }

    @PutMapping("/{id}/leido")
    public ResponseEntity<Void> marcarLeido(@PathVariable("id") Long id) {
        avisosService.marcarLeido(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/leidos")
    public ResponseEntity<Void> marcarTodosLeidos() {
        avisosService.marcarTodosLeidos();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/preferencias")
    public List<PreferenciaAvisoResponse> preferencias() {
        return avisosService.preferencias();
    }

    @PutMapping("/preferencias/{tipo}")
    public List<PreferenciaAvisoResponse> cambiarPreferencia(
        @PathVariable("tipo") String tipo, @RequestBody(required = false) PreferenciaAvisoRequest request) {
        return avisosService.cambiarPreferencia(tipo, request == null ? null : request.activo());
    }
}
