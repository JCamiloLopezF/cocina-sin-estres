package co.edu.uniquindio.cocinasinestres.comun.seguridad;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.uniquindio.cocinasinestres.comun.config.PropiedadesLimites;
import co.edu.uniquindio.cocinasinestres.comun.error.CodigoError;
import co.edu.uniquindio.cocinasinestres.comun.error.EscritorProblemDetail;
import co.edu.uniquindio.cocinasinestres.perfil.domain.Rol;
import co.edu.uniquindio.cocinasinestres.perfil.domain.UsuarioAutenticado;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import tools.jackson.databind.json.JsonMapper;

/** El límite de peticiones por minuto, sin levantar Spring. */
@DisplayName("FiltroLimitePeticiones")
class FiltroLimitePeticionesTest {

    /** 12:00:10 UTC: faltan 50 segundos para el siguiente minuto. */
    private final RelojMovible reloj = new RelojMovible(Instant.parse("2026-10-28T12:00:10Z"));
    private final EscritorProblemDetail escritor = new EscritorProblemDetail(JsonMapper.builder().build());

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void dejaPasarHastaElTopeYDespuesResponde429ConRetryAfter() throws Exception {
        FiltroLimitePeticiones filtro = filtro(3, 0);
        entrarComo(7L);

        for (int i = 0; i < 3; i++) {
            assertThat(pedir(filtro, "GET", "/api/items-despensa").getStatus()).isEqualTo(200);
        }
        MockHttpServletResponse bloqueada = pedir(filtro, "GET", "/api/items-despensa");

        assertThat(bloqueada.getStatus()).isEqualTo(429);
        assertThat(bloqueada.getHeader("Retry-After")).isEqualTo("50");
        assertThat(bloqueada.getContentAsString()).contains(CodigoError.LIMITE_PETICIONES);
    }

    @Test
    void elMinutoSiguienteVuelveAEmpezar() throws Exception {
        FiltroLimitePeticiones filtro = filtro(1, 0);
        entrarComo(7L);

        assertThat(pedir(filtro, "GET", "/api/yo").getStatus()).isEqualTo(200);
        assertThat(pedir(filtro, "GET", "/api/yo").getStatus()).isEqualTo(429);

        reloj.avanzarSegundos(60);

        assertThat(pedir(filtro, "GET", "/api/yo").getStatus()).isEqualTo(200);
    }

    @Test
    void cadaCuentaTieneSuPropioContador() throws Exception {
        FiltroLimitePeticiones filtro = filtro(1, 0);

        entrarComo(7L);
        assertThat(pedir(filtro, "GET", "/api/yo").getStatus()).isEqualTo(200);
        assertThat(pedir(filtro, "GET", "/api/yo").getStatus()).isEqualTo(429);

        entrarComo(8L);
        assertThat(pedir(filtro, "GET", "/api/yo").getStatus()).isEqualTo(200);
    }

    @Test
    void lasRutasCostosasTienenUnTopeAparte() throws Exception {
        FiltroLimitePeticiones filtro = filtro(100, 2);
        entrarComo(7L);

        assertThat(pedir(filtro, "POST", "/api/planes-semanales/3/generar").getStatus()).isEqualTo(200);
        assertThat(pedir(filtro, "POST", "/api/planes-semanales/3/ajustar-presupuesto").getStatus()).isEqualTo(200);
        assertThat(pedir(filtro, "POST", "/api/planes-semanales/3/comidas/9/otra-opcion").getStatus())
            .isEqualTo(429);
        // Las demás rutas siguen respondiendo.
        assertThat(pedir(filtro, "GET", "/api/planes-semanales").getStatus()).isEqualTo(200);
    }

    @Test
    void reconoceLasRutasCostosas() {
        assertThat(costosa("/api/planes-semanales/3/generar")).isTrue();
        assertThat(costosa("/api/planes-semanales/3/comidas/12/otra-opcion")).isTrue();
        assertThat(costosa("/api/rescate")).isTrue();
        assertThat(costosa("/api/admin/analisis")).isTrue();
        assertThat(costosa("/api/planes-semanales/3/costo")).isFalse();
        assertThat(costosa("/api/recetas")).isFalse();
    }

    @Test
    void sinSesionCuentaPorIp() throws Exception {
        FiltroLimitePeticiones filtro = filtro(1, 0);

        assertThat(pedir(filtro, "GET", "/api/yo", "10.0.0.1").getStatus()).isEqualTo(200);
        assertThat(pedir(filtro, "GET", "/api/yo", "10.0.0.1").getStatus()).isEqualTo(429);
        assertThat(pedir(filtro, "GET", "/api/yo", "10.0.0.2").getStatus()).isEqualTo(200);
    }

    @Test
    void laRutaDeSaludYElPreflightNoCuentan() throws Exception {
        FiltroLimitePeticiones filtro = filtro(1, 0);
        entrarComo(7L);

        for (int i = 0; i < 5; i++) {
            assertThat(pedir(filtro, "GET", "/api/salud").getStatus()).isEqualTo(200);
            assertThat(pedir(filtro, "OPTIONS", "/api/yo").getStatus()).isEqualTo(200);
        }
        assertThat(pedir(filtro, "GET", "/api/yo").getStatus()).isEqualTo(200);
    }

    @Test
    void enCeroNoHayLimite() throws Exception {
        FiltroLimitePeticiones filtro = filtro(0, 0);
        entrarComo(7L);

        for (int i = 0; i < 500; i++) {
            assertThat(pedir(filtro, "POST", "/api/planes-semanales/3/generar").getStatus()).isEqualTo(200);
        }
    }

    private FiltroLimitePeticiones filtro(int porMinuto, int costosas) {
        return new FiltroLimitePeticiones(new PropiedadesLimites(porMinuto, costosas, 0), escritor, reloj);
    }

    private static void entrarComo(long id) {
        UsuarioAutenticado usuaria = new UsuarioAutenticado(id, "uid-" + id, id + "@ejemplo.com", Rol.USUARIA, true);
        SecurityContextHolder.getContext().setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(usuaria, null, List.of()));
    }

    private static MockHttpServletResponse pedir(FiltroLimitePeticiones filtro, String metodo, String ruta)
            throws Exception {
        return pedir(filtro, metodo, ruta, "127.0.0.1");
    }

    private static MockHttpServletResponse pedir(FiltroLimitePeticiones filtro, String metodo, String ruta, String ip)
            throws Exception {
        MockHttpServletRequest peticion = new MockHttpServletRequest(metodo, ruta);
        peticion.setRemoteAddr(ip);
        MockHttpServletResponse respuesta = new MockHttpServletResponse();
        filtro.doFilter(peticion, respuesta, new MockFilterChain());
        return respuesta;
    }

    private static boolean costosa(String ruta) {
        return FiltroLimitePeticiones.esCostosa(new MockHttpServletRequest("POST", ruta));
    }

    /** Reloj que se puede adelantar a mano. */
    private static final class RelojMovible extends Clock {

        private Instant ahora;

        RelojMovible(Instant inicio) {
            this.ahora = inicio;
        }

        void avanzarSegundos(long segundos) {
            ahora = ahora.plusSeconds(segundos);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zona) {
            return this;
        }

        @Override
        public Instant instant() {
            return ahora;
        }
    }
}
