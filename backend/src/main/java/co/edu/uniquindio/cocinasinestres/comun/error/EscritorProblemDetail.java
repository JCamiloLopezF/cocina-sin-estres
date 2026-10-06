package co.edu.uniquindio.cocinasinestres.comun.error;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Escribe un error directo en la respuesta, con el mismo formato que usa
 * {@link ManejadorErrores}.
 *
 * <p>Hace falta porque los errores de la cadena de seguridad (401 y 403) ocurren
 * antes de que exista un controlador, así que no pasan por el
 * {@code @RestControllerAdvice}. Sin esto, Spring responderia con su página de
 * error por defecto y el frontend recibiría dos formatos distintos.</p>
 */
@Component
public class EscritorProblemDetail {

    // El de Jackson 3 (tools.jackson), que es el que trae Spring Boot 4.
    private final ObjectMapper objectMapper;

    public EscritorProblemDetail(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Responde con un Problem Details (RFC 9457) y cierra el cuerpo.
     *
     * @param codigo  código estable que lee el frontend, ver {@link CodigoError}
     * @param detalle mensaje en español para la usuaria; nunca una traza ni datos internos
     */
    public void escribir(HttpServletRequest peticion,
                         HttpServletResponse respuesta,
                         HttpStatus estado,
                         String titulo,
                         String codigo,
                         String detalle) throws IOException {

        ProblemDetail problema = ProblemDetail.forStatusAndDetail(estado, detalle);
        problema.setTitle(titulo);
        problema.setProperty("codigo", codigo);
        problema.setInstance(URI.create(peticion.getRequestURI()));

        respuesta.setStatus(estado.value());
        respuesta.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        respuesta.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(respuesta.getOutputStream(), problema);
    }
}
