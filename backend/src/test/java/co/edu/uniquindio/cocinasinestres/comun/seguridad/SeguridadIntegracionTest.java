package co.edu.uniquindio.cocinasinestres.comun.seguridad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.uniquindio.cocinasinestres.comun.error.CodigoError;
import co.edu.uniquindio.cocinasinestres.perfil.domain.Rol;
import co.edu.uniquindio.cocinasinestres.perfil.model.Usuario;
import co.edu.uniquindio.cocinasinestres.perfil.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Prueba de punta a punta de la cadena de seguridad, contra un MySQL de verdad.
 *
 * <p>Necesita Docker en ejecución. El solo hecho de que el contexto arranque ya
 * verifica dos cosas más: que Flyway aplica {@code V1__esquema_inicial.sql} sobre una
 * base vacía y que {@code ddl-auto=validate} no encuentra diferencias entre la entidad
 * {@code Usuario} y la tabla {@code usuario}.</p>
 *
 * <p>Corre con el perfil {@code dev} porque es el único que permite autenticarse sin un
 * proyecto de Firebase, usando el encabezado {@code X-Dev-User} (ADR-19).</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Testcontainers
@DisplayName("Seguridad de la API")
class SeguridadIntegracionTest {

    private static final String UID = "uid-de-prueba";

    @Container
    @ServiceConnection
    static final MySQLContainer MYSQL =
            new MySQLContainer(DockerImageName.parse("mysql:8.4")).withEnv("TZ", "UTC");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @BeforeEach
    void dejarLaTablaDeCuentasVacia() {
        usuarioRepository.deleteAll();
    }

    @Test
    @DisplayName("sin token responde 401 con el formato de error de la API")
    void sinTokenResponde401() throws Exception {
        mockMvc.perform(get("/api/yo"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.codigo").value(CodigoError.AUTH_TOKEN_INVALIDO))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.detail").isNotEmpty())
                // Nunca una traza de pila al cliente (CONTRIBUTING.md §7).
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    @DisplayName("con X-Dev-User responde 200 y crea la cuenta en el primer ingreso")
    void conEncabezadoDeDesarrolloResponde200YDaDeAltaLaCuenta() throws Exception {
        assertThat(usuarioRepository.findByFirebaseUid(UID)).isEmpty();

        mockMvc.perform(get("/api/yo").header(FiltroUsuarioDev.ENCABEZADO, UID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.correo").value(UID + "@dev.local"))
                .andExpect(jsonPath("$.rol").value(Rol.USUARIA.name()))
                .andExpect(jsonPath("$.perfilCompleto").value(false));

        Usuario creado = usuarioRepository.findByFirebaseUid(UID).orElseThrow();
        assertThat(creado.getRol()).isEqualTo(Rol.USUARIA);
        assertThat(creado.isActivo()).isTrue();
        assertThat(creado.isPerfilCompleto()).isFalse();
        // Las marcas de tiempo las pone MySQL y Hibernate las lee de vuelta.
        assertThat(creado.getCreadoEn()).isNotNull();
    }

    @Test
    @DisplayName("una usuaria no entra a /api/admin: responde 403")
    void unaUsuariaNoEntraALasRutasDeAdministracion() throws Exception {
        mockMvc.perform(get("/api/admin/cuentas").header(FiltroUsuarioDev.ENCABEZADO, UID))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.codigo").value(CodigoError.AUTH_SIN_PERMISO));
    }

    @Test
    @DisplayName("una usuaria tampoco entra a /api/curaduria: responde 403")
    void unaUsuariaNoEntraALasRutasDeCuraduria() throws Exception {
        mockMvc.perform(get("/api/curaduria/productos").header(FiltroUsuarioDev.ENCABEZADO, UID))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value(CodigoError.AUTH_SIN_PERMISO));
    }

    @Test
    @DisplayName("un ADMIN sí pasa el control de rutas de /api/admin")
    void unAdminPasaElControlDeRutasDeAdministracion() throws Exception {
        usuarioRepository.save(Usuario.nueva(UID, "admin@ejemplo.com", Rol.ADMIN));

        // 404 y no 403: la autorización lo deja pasar y no hay controlador todavía.
        // Cada módulo agrega sus rutas bajo /api/admin.
        mockMvc.perform(get("/api/admin/cuentas").header(FiltroUsuarioDev.ENCABEZADO, UID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value(CodigoError.RECURSO_NO_ENCONTRADO));
    }

    @Test
    @DisplayName("la ruta de salud es pública")
    void laRutaDeSaludEsPublica() throws Exception {
        mockMvc.perform(get("/api/salud"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ok"))
                // Fechas en ISO 8601, no en milisegundos (CONTRIBUTING.md §5).
                .andExpect(jsonPath("$.horaUtc").value(org.hamcrest.Matchers.matchesPattern(
                        "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?Z")));
    }

    @Test
    @DisplayName("el alta perezosa no duplica la cuenta en la segunda petición")
    void elAltaPerezosaNoDuplicaLaCuenta() throws Exception {
        mockMvc.perform(get("/api/yo").header(FiltroUsuarioDev.ENCABEZADO, UID))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/yo").header(FiltroUsuarioDev.ENCABEZADO, UID))
                .andExpect(status().isOk());

        assertThat(usuarioRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("una cuenta desactivada no puede operar")
    void unaCuentaDesactivadaNoPuedeOperar() throws Exception {
        Usuario usuario = Usuario.nueva(UID, "mariana@ejemplo.com", Rol.USUARIA);
        usuario.setActivo(false);
        usuarioRepository.save(usuario);

        mockMvc.perform(get("/api/yo").header(FiltroUsuarioDev.ENCABEZADO, UID))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value(CodigoError.AUTH_CUENTA_INACTIVA));
    }

    @Test
    @DisplayName("fuera de /api no hay nada expuesto")
    void fueraDeApiNoHayNadaExpuesto() throws Exception {
        mockMvc.perform(get("/actuator/env"))
                .andExpect(status().is4xxClientError());
    }
}
