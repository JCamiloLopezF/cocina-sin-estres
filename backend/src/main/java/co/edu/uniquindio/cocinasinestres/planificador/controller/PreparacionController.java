package co.edu.uniquindio.cocinasinestres.planificador.controller;

import co.edu.uniquindio.cocinasinestres.comun.error.ConflictoException;
import co.edu.uniquindio.cocinasinestres.comun.error.DatosInvalidosException;
import co.edu.uniquindio.cocinasinestres.perfil.model.Hogar;
import co.edu.uniquindio.cocinasinestres.perfil.repository.HogarRepository;
import co.edu.uniquindio.cocinasinestres.perfil.service.UsuarioActualService;
import co.edu.uniquindio.cocinasinestres.planificador.domain.MovimientosDespensa;
import co.edu.uniquindio.cocinasinestres.planificador.dto.PreparacionRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * "La preparé" cuando se cocina una receta que no está en el plan (Modo cocinar desde el
 * recetario): descuenta sus ingredientes de la despensa (KAN-16, ADR-24). Si la receta
 * está en el plan se usa {@code marcar-preparada}, que además la marca en el día.
 */
@RestController
@RequestMapping("/api/preparaciones")
@PreAuthorize("hasRole('USUARIA')")
public class PreparacionController {

    private final HogarRepository hogarRepository;
    private final UsuarioActualService usuarioActualService;
    private final MovimientosDespensa movimientosDespensa;

    public PreparacionController(
        HogarRepository hogarRepository,
        UsuarioActualService usuarioActualService,
        MovimientosDespensa movimientosDespensa) {
        this.hogarRepository = hogarRepository;
        this.usuarioActualService = usuarioActualService;
        this.movimientosDespensa = movimientosDespensa;
    }

    @PostMapping
    @Transactional
    public ResponseEntity<Void> preparar(@RequestBody PreparacionRequest request) {
        if (request == null || request.recetaId() == null) {
            throw new DatosInvalidosException("PLAN_DATOS_INVALIDOS", "Falta la receta que preparaste.");
        }
        Hogar hogar = hogarRepository.findByUsuario_Id(usuarioActualService.obtener().id())
            .orElseThrow(() -> new ConflictoException(
                "PLAN_SIN_PERFIL_HOGAR", "Completa el perfil de tu hogar antes de usar la despensa."));
        movimientosDespensa.descontarReceta(hogar.getId(), request.recetaId());
        return ResponseEntity.noContent().build();
    }
}
