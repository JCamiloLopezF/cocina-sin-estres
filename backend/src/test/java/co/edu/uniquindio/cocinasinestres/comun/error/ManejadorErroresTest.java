package co.edu.uniquindio.cocinasinestres.comun.error;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.ProblemDetail;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.bind.MissingServletRequestParameterException;

/** Los parámetros mal escritos son un 400 de quien llama, no un 500 del servidor. */
@DisplayName("ManejadorErrores: parámetros inválidos")
class ManejadorErroresTest {

    private final ManejadorErrores manejador = new ManejadorErrores();

    @Test
    void faltaUnParametroObligatorio() {
        ProblemDetail problema = manejador.manejarParametroInvalido(
            new MissingServletRequestParameterException("semanaInicio", "LocalDate"),
            new MockHttpServletRequest("GET", "/api/planes-semanales"));

        assertThat(problema.getStatus()).isEqualTo(400);
        assertThat(problema.getProperties()).containsEntry("codigo", CodigoError.PETICION_MAL_FORMADA);
    }

    @Test
    void unValorQueNoSePuedeConvertir() {
        ProblemDetail problema = manejador.manejarParametroInvalido(
            new TypeMismatchException("hola", LocalDate.class),
            new MockHttpServletRequest("GET", "/api/items-despensa/abc"));

        assertThat(problema.getStatus()).isEqualTo(400);
        assertThat(problema.getProperties()).containsEntry("codigo", CodigoError.PETICION_MAL_FORMADA);
        assertThat(problema.getDetail()).doesNotContain("hola");
    }
}
