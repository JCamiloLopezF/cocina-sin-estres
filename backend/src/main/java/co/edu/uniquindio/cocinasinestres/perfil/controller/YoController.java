package co.edu.uniquindio.cocinasinestres.perfil.controller;

import co.edu.uniquindio.cocinasinestres.perfil.domain.UsuarioAutenticado;
import co.edu.uniquindio.cocinasinestres.perfil.dto.UsuarioResponse;
import co.edu.uniquindio.cocinasinestres.perfil.service.UsuarioActualService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Devuelve la cuenta de quien está usando la aplicación.
 *
 * <p>Es lo primero que llama el frontend después de entrar con Firebase: con la
 * respuesta sabe qué rol tiene y si debe mandarla al onboarding. También confirma
 * que el alta perezosa funcionó (ADR-05).</p>
 */
@RestController
@RequestMapping("/api/yo")
public class YoController {

    private final UsuarioActualService usuarioActualService;

    public YoController(UsuarioActualService usuarioActualService) {
        this.usuarioActualService = usuarioActualService;
    }

    @GetMapping
    public UsuarioResponse consultar() {
        UsuarioAutenticado usuario = usuarioActualService.obtener();
        return new UsuarioResponse(
                usuario.id(),
                usuario.correo(),
                usuario.rol().name(),
                usuario.perfilCompleto());
    }
}
