package co.edu.uniquindio.cocinasinestres.comun.error;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Único traductor de excepciones a respuestas de error de la API.
 *
 * <p>Todas salen en formato Problem Details (RFC 9457) con el campo {@code codigo}
 * estable, según la tabla de CONTRIBUTING.md §7. Los controladores no construyen
 * respuestas de error: lanzan una {@link ExcepcionNegocio} y esto se encarga.</p>
 */
@RestControllerAdvice
public class ManejadorErrores {

    private static final Logger log = LoggerFactory.getLogger(ManejadorErrores.class);

    /** Errores previstos del negocio: cada uno trae su código y su estado. */
    @ExceptionHandler(ExcepcionNegocio.class)
    public ProblemDetail manejarNegocio(ExcepcionNegocio excepcion, HttpServletRequest peticion) {
        return construir(excepcion.getEstado(), excepcion.getTitulo(),
                excepcion.getCodigo(), excepcion.getMessage(), peticion);
    }

    /** Falló una anotación de validación sobre el cuerpo de la petición. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail manejarValidacion(MethodArgumentNotValidException excepcion,
                                           HttpServletRequest peticion) {

        List<ErrorCampo> errores = excepcion.getBindingResult().getFieldErrors().stream()
                .map(error -> new ErrorCampo(error.getField(), mensajeDe(error)))
                .toList();

        ProblemDetail problema = construir(HttpStatus.BAD_REQUEST, "Datos inválidos",
                CodigoError.VALIDACION_DATOS, "Revisa los datos marcados.", peticion);
        problema.setProperty("errores", errores);
        return problema;
    }

    /** Falló la validación de un parámetro de la ruta o de la consulta. */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ProblemDetail manejarValidacionDeParametros(HandlerMethodValidationException excepcion,
                                                       HttpServletRequest peticion) {
        return construir(HttpStatus.BAD_REQUEST, "Datos inválidos",
                CodigoError.VALIDACION_DATOS, "Revisa los datos enviados.", peticion);
    }

    /** El cuerpo no es JSON válido, o un campo trae un tipo que no corresponde. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail manejarCuerpoIlegible(HttpMessageNotReadableException excepcion,
                                               HttpServletRequest peticion) {
        return construir(HttpStatus.BAD_REQUEST, "Petición mal formada",
                CodigoError.PETICION_MAL_FORMADA, "No pudimos leer los datos enviados.", peticion);
    }

    /**
     * Errores que Spring MVC ya marca con un estado: ruta inexistente, método que no
     * aplica, tipo de contenido no soportado.
     *
     * <p>Hay que atenderlos aquí para que no caigan en el manejador de lo inesperado,
     * que los convertiría en un 500 y llenaría el log de errores que no lo son.</p>
     */
    @ExceptionHandler({
            ErrorResponseException.class,
            NoResourceFoundException.class,
            NoHandlerFoundException.class,
            HttpRequestMethodNotSupportedException.class,
            HttpMediaTypeNotSupportedException.class,
            HttpMediaTypeNotAcceptableException.class
    })
    public ProblemDetail manejarErrorConEstado(Exception excepcion, HttpServletRequest peticion) {

        // Todas las excepciones listadas arriba implementan ErrorResponse y ya saben
        // con qué estado deben responder; aquí solo se les agrega el campo codigo.
        HttpStatus estado = HttpStatus.valueOf(((ErrorResponse) excepcion).getStatusCode().value());
        return switch (estado) {
            case NOT_FOUND -> construir(estado, "No encontrado",
                    CodigoError.RECURSO_NO_ENCONTRADO, "No encontramos lo que buscas.", peticion);
            case METHOD_NOT_ALLOWED, UNSUPPORTED_MEDIA_TYPE, NOT_ACCEPTABLE ->
                    construir(estado, "Petición no soportada",
                            CodigoError.PETICION_NO_SOPORTADA, "Esa operación no aplica aquí.", peticion);
            default -> construir(estado, "Petición mal formada",
                    CodigoError.PETICION_MAL_FORMADA, "No pudimos procesar la petición.", peticion);
        };
    }

    /**
     * El rol no alcanza. Llega aquí cuando la restricción está en un método; la de
     * las rutas la atiende antes {@code ManejadorAccesoDenegado}.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail manejarAccesoDenegado(AccessDeniedException excepcion,
                                               HttpServletRequest peticion) {
        return construir(HttpStatus.FORBIDDEN, "Sin permiso",
                CodigoError.AUTH_SIN_PERMISO, "Tu cuenta no tiene permiso para esta acción.", peticion);
    }

    /**
     * Cualquier cosa no prevista. La traza va al log con un identificador que también
     * viaja en la respuesta, para poder cruzarlos sin exponer nada al cliente.
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail manejarInesperado(Exception excepcion, HttpServletRequest peticion) {
        String identificador = UUID.randomUUID().toString();
        log.error("Error inesperado [{}] en {} {}",
                identificador, peticion.getMethod(), peticion.getRequestURI(), excepcion);

        ProblemDetail problema = construir(HttpStatus.INTERNAL_SERVER_ERROR, "Error del servidor",
                CodigoError.ERROR_INTERNO,
                "Algo falló de nuestro lado. Vuelve a intentarlo en un momento.", peticion);
        problema.setProperty("identificador", identificador);
        return problema;
    }

    private ProblemDetail construir(HttpStatus estado, String titulo, String codigo,
                                    String detalle, HttpServletRequest peticion) {

        ProblemDetail problema = ProblemDetail.forStatusAndDetail(estado, detalle);
        problema.setTitle(titulo);
        problema.setProperty("codigo", codigo);
        problema.setInstance(URI.create(peticion.getRequestURI()));
        return problema;
    }

    private String mensajeDe(FieldError error) {
        return error.getDefaultMessage() != null ? error.getDefaultMessage() : "Dato inválido.";
    }
}
