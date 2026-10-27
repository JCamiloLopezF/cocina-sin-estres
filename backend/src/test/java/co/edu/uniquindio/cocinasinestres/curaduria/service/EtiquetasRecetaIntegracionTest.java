package co.edu.uniquindio.cocinasinestres.curaduria.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import co.edu.uniquindio.cocinasinestres.comun.error.DatosInvalidosException;
import co.edu.uniquindio.cocinasinestres.curaduria.dto.EtiquetasRecetaRequest;
import co.edu.uniquindio.cocinasinestres.curaduria.dto.RecetaVideoResponse;
import co.edu.uniquindio.cocinasinestres.perfil.domain.Rol;
import co.edu.uniquindio.cocinasinestres.perfil.domain.UsuarioAutenticado;
import co.edu.uniquindio.cocinasinestres.perfil.service.UsuarioActualService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * El curador marca una receta como plato fuerte o que les gusta a los niños (KAN-17), contra
 * un MySQL de verdad: cambia la receta y queda en la auditoría del catálogo (RNF-24).
 *
 * <p>Necesita Docker en ejecución, como las demás pruebas de integración.</p>
 */
@SpringBootTest
@ActiveProfiles("dev")
@Testcontainers
@DisplayName("Etiquetas de receta que pone el curador")
class EtiquetasRecetaIntegracionTest {

    @Container
    @ServiceConnection
    static final MySQLContainer MYSQL =
            new MySQLContainer(DockerImageName.parse("mysql:8.4")).withEnv("TZ", "UTC");

    @Autowired
    private RecetaVideoService servicio;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private UsuarioActualService usuarioActualService;

    @Test
    @DisplayName("cambia solo lo que se envía y lo deja en la auditoría")
    void cambiaYAudita() {
        jdbcTemplate.update(
            "INSERT INTO usuario (firebase_uid, correo, rol) VALUES ('uid-curador', 'curador@dev.local', 'CURADOR')");
        Long curador = jdbcTemplate.queryForObject(
            "SELECT id FROM usuario WHERE firebase_uid = 'uid-curador'", Long.class);
        when(usuarioActualService.obtener()).thenReturn(
            new UsuarioAutenticado(curador, "uid-curador", "curador@dev.local", Rol.CURADOR, false));
        Long batido = jdbcTemplate.queryForObject(
            "SELECT id FROM receta WHERE nombre = 'Batido de banano y leche'", Long.class);

        RecetaVideoResponse antes = servicio.listar().stream().filter(r -> r.id().equals(batido)).findFirst().orElseThrow();
        assertThat(antes.platoFuerte()).isFalse();
        assertThat(antes.gustaANinos()).isTrue();

        RecetaVideoResponse despues = servicio.cambiarEtiquetas(batido, new EtiquetasRecetaRequest(true, null));

        assertThat(despues.platoFuerte()).isTrue();
        assertThat(despues.gustaANinos()).isTrue();
        assertThat(jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM auditoria_catalogo WHERE entidad_id = ? AND usuario_id = ? AND JSON_CONTAINS_PATH(cambios, 'one', '$.es_plato_fuerte')",
            Integer.class, batido, curador)).isEqualTo(1);

        // Sin cambios no se escribe otra auditoría.
        servicio.cambiarEtiquetas(batido, new EtiquetasRecetaRequest(true, true));
        assertThat(jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM auditoria_catalogo WHERE entidad_id = ? AND usuario_id = ?",
            Integer.class, batido, curador)).isEqualTo(1);

        assertThatThrownBy(() -> servicio.cambiarEtiquetas(batido, new EtiquetasRecetaRequest(null, null)))
            .isInstanceOf(DatosInvalidosException.class);
    }
}
