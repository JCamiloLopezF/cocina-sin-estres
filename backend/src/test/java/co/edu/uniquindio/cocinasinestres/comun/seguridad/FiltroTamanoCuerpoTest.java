package co.edu.uniquindio.cocinasinestres.comun.seguridad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.edu.uniquindio.cocinasinestres.comun.error.CodigoError;
import co.edu.uniquindio.cocinasinestres.comun.error.EscritorProblemDetail;
import jakarta.servlet.FilterChain;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

/** El tope del tamaño del cuerpo, sin levantar Spring. */
@DisplayName("FiltroTamanoCuerpo")
class FiltroTamanoCuerpoTest {

    private final FiltroTamanoCuerpo filtro =
        new FiltroTamanoCuerpo(100, new EscritorProblemDetail(JsonMapper.builder().build()));

    @Test
    void unCuerpoDeclaradoMasGrandeResponde413SinPasar() throws Exception {
        MockHttpServletRequest peticion = new MockHttpServletRequest("PUT", "/api/perfil/hogar");
        peticion.setContent(new byte[101]);
        MockHttpServletResponse respuesta = new MockHttpServletResponse();
        AtomicBoolean paso = new AtomicBoolean();

        filtro.doFilter(peticion, respuesta, (req, res) -> paso.set(true));

        assertThat(respuesta.getStatus()).isEqualTo(413);
        assertThat(respuesta.getContentAsString()).contains(CodigoError.CUERPO_DEMASIADO_GRANDE);
        assertThat(paso).isFalse();
    }

    @Test
    void unCuerpoDentroDelTopePasaYSeLeeEntero() throws Exception {
        MockHttpServletRequest peticion = new MockHttpServletRequest("PUT", "/api/perfil/hogar");
        peticion.setContent(new byte[100]);
        MockHttpServletResponse respuesta = new MockHttpServletResponse();
        int[] leidos = new int[1];

        filtro.doFilter(peticion, respuesta, (req, res) -> leidos[0] = req.getInputStream().readAllBytes().length);

        assertThat(respuesta.getStatus()).isEqualTo(200);
        assertThat(leidos[0]).isEqualTo(100);
    }

    @Test
    void sinContentLengthCortaLaLecturaAlPasarElTope() throws Exception {
        MockHttpServletRequest peticion = new MockHttpServletRequest("PUT", "/api/perfil/hogar") {
            @Override
            public long getContentLengthLong() {
                return -1;
            }

            @Override
            public int getContentLength() {
                return -1;
            }
        };
        peticion.setContent(new byte[500]);
        FilterChain leerTodo = (req, res) -> req.getInputStream().readAllBytes();

        assertThatThrownBy(() -> filtro.doFilter(peticion, new MockHttpServletResponse(), leerTodo))
            .isInstanceOf(IOException.class);
    }
}
