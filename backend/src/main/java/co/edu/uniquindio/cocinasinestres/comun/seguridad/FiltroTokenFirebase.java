package co.edu.uniquindio.cocinasinestres.comun.seguridad;

import co.edu.uniquindio.cocinasinestres.comun.error.EscritorProblemDetail;
import co.edu.uniquindio.cocinasinestres.perfil.service.UsuarioActualService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

/**
 * Autentica con el token de Firebase que manda el navegador (ADR-05).
 *
 * <p>Es el único modo de entrar fuera del perfil {@code dev}. No hay endpoint de
 * registro ni de inicio de sesión: el frontend entra con el SDK de Firebase y aquí
 * solo se verifica el token que trae.</p>
 */
@Component
@Profile("!dev")
public class FiltroTokenFirebase extends FiltroAutenticacionBase {

    private static final String PREFIJO_BEARER = "Bearer ";

    private final VerificadorTokenFirebase verificador;

    public FiltroTokenFirebase(UsuarioActualService usuarioActualService,
                               EscritorProblemDetail escritorProblemDetail,
                               VerificadorTokenFirebase verificador) {
        super(usuarioActualService, escritorProblemDetail);
        this.verificador = verificador;
    }

    @Override
    protected Optional<IdentidadExterna> identificar(HttpServletRequest peticion) {
        String encabezado = peticion.getHeader(HttpHeaders.AUTHORIZATION);

        if (encabezado == null || !encabezado.startsWith(PREFIJO_BEARER)) {
            return Optional.empty();
        }

        String idToken = encabezado.substring(PREFIJO_BEARER.length()).trim();
        if (idToken.isEmpty()) {
            return Optional.empty();
        }

        var identidad = verificador.verificar(idToken);
        return Optional.of(new IdentidadExterna(identidad.firebaseUid(), identidad.correo()));
    }
}
