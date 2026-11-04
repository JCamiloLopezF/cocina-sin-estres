package co.edu.uniquindio.cocinasinestres.comun.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;

@DisplayName("Comprobación de salud")
class SaludControllerTest {

    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final SaludController controller = new SaludController("1.0.0", jdbc);

    @Test
    @DisplayName("responde ok cuando la base contesta")
    void ok() {
        when(jdbc.queryForObject("SELECT 1", Integer.class)).thenReturn(1);

        var respuesta = controller.consultar();

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody().estado()).isEqualTo("ok");
    }

    @Test
    @DisplayName("responde 503 si la base no contesta, para que la vigilancia lo registre")
    void sinBase() {
        when(jdbc.queryForObject("SELECT 1", Integer.class))
            .thenThrow(new DataAccessResourceFailureException("sin conexión"));

        var respuesta = controller.consultar();

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(respuesta.getBody().estado()).isEqualTo("sin-base-de-datos");
    }
}
