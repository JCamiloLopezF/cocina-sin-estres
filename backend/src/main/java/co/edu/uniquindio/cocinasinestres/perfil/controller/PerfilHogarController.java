
package co.edu.uniquindio.cocinasinestres.perfil.controller;

import co.edu.uniquindio.cocinasinestres.perfil.dto.PerfilHogarRequest;
import co.edu.uniquindio.cocinasinestres.perfil.dto.PerfilHogarResponse;
import co.edu.uniquindio.cocinasinestres.perfil.service.HogarService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/perfil/hogar")
public class PerfilHogarController {

    private final HogarService hogarService;

    public PerfilHogarController(HogarService hogarService) {
        this.hogarService = hogarService;
    }

    /**
     * Guarda o actualiza el perfil del hogar autenticado.
     */
    @PutMapping
    public PerfilHogarResponse guardar(
        @RequestBody PerfilHogarRequest request) {
        return hogarService.guardar(request);
    }

    /**
     * Consulta el perfil del hogar de la usuaria autenticada.
     */
    @GetMapping
    public PerfilHogarResponse consultar() {
        return hogarService.obtener();
    }
}
