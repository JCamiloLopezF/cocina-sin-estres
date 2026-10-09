package co.edu.uniquindio.cocinasinestres.comun.seguridad;

import co.edu.uniquindio.cocinasinestres.comun.error.CodigoError;
import co.edu.uniquindio.cocinasinestres.comun.error.EscritorProblemDetail;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/**
 * Responde 403 cuando la usuaria está identificada pero su rol no alcanza para la ruta.
 *
 * <p>Ojo con la diferencia que pide RNF-22: esto es "tu rol no puede entrar aquí". Pedir
 * un recurso de otro hogar es distinto y responde 404, para no confirmar que existe.</p>
 */
@Component
public class ManejadorAccesoDenegado implements AccessDeniedHandler {

    private final EscritorProblemDetail escritorProblemDetail;

    public ManejadorAccesoDenegado(EscritorProblemDetail escritorProblemDetail) {
        this.escritorProblemDetail = escritorProblemDetail;
    }

    @Override
    public void handle(HttpServletRequest peticion,
                       HttpServletResponse respuesta,
                       AccessDeniedException excepcion) throws IOException {

        escritorProblemDetail.escribir(peticion, respuesta, HttpStatus.FORBIDDEN,
                "Sin permiso", CodigoError.AUTH_SIN_PERMISO,
                "Tu cuenta no tiene permiso para esta sección.");
    }
}
