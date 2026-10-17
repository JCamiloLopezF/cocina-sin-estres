package co.edu.uniquindio.cocinasinestres.comun.seguridad;

import co.edu.uniquindio.cocinasinestres.comun.error.CodigoError;
import co.edu.uniquindio.cocinasinestres.comun.error.EscritorProblemDetail;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * Responde 401 cuando la petición necesita identidad y no la trae.
 *
 * <p>Reemplaza la respuesta por defecto de Spring Security para que el frontend reciba
 * el mismo Problem Details que en cualquier otro error (CONTRIBUTING.md §7).</p>
 */
@Component
public class EntryPointNoAutenticado implements AuthenticationEntryPoint {

    private final EscritorProblemDetail escritorProblemDetail;

    public EntryPointNoAutenticado(EscritorProblemDetail escritorProblemDetail) {
        this.escritorProblemDetail = escritorProblemDetail;
    }

    @Override
    public void commence(HttpServletRequest peticion,
                         HttpServletResponse respuesta,
                         AuthenticationException excepcion) throws IOException {

        escritorProblemDetail.escribir(peticion, respuesta, HttpStatus.UNAUTHORIZED,
                "Sesión no válida", CodigoError.AUTH_TOKEN_INVALIDO,
                "Inicia sesión para continuar.");
    }
}
