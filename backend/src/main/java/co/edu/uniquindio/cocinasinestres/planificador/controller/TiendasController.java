package co.edu.uniquindio.cocinasinestres.planificador.controller;

import co.edu.uniquindio.cocinasinestres.planificador.dto.TiendaRequest;
import co.edu.uniquindio.cocinasinestres.planificador.dto.TiendaResponse;
import co.edu.uniquindio.cocinasinestres.planificador.service.TiendasService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Tiendas para pedir el mercado por WhatsApp (KAN-53). */
@RestController
@RequestMapping("/api/tiendas")
@PreAuthorize("hasRole('USUARIA')")
public class TiendasController {

    private final TiendasService tiendasService;

    public TiendasController(TiendasService tiendasService) {
        this.tiendasService = tiendasService;
    }

    @GetMapping
    public List<TiendaResponse> listar() {
        return tiendasService.listar();
    }

    /** 400 {@code TIENDA_DATOS_INVALIDOS}; 409 {@code TIENDA_REPETIDA} o {@code TIENDAS_MAXIMO}. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TiendaResponse crear(@RequestBody(required = false) TiendaRequest request) {
        return tiendasService.crear(request);
    }

    @DeleteMapping("/{tiendaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void borrar(@PathVariable("tiendaId") Long tiendaId) {
        tiendasService.borrar(tiendaId);
    }
}
