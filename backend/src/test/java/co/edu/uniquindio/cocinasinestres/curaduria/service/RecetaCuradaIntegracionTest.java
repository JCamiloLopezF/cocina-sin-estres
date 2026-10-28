package co.edu.uniquindio.cocinasinestres.curaduria.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import co.edu.uniquindio.cocinasinestres.comun.error.ConflictoException;
import co.edu.uniquindio.cocinasinestres.comun.error.DatosInvalidosException;
import co.edu.uniquindio.cocinasinestres.curaduria.dto.IngredienteCuradoRequest;
import co.edu.uniquindio.cocinasinestres.curaduria.dto.RecetaCuradaRequest;
import co.edu.uniquindio.cocinasinestres.curaduria.dto.RecetaCuradaResponse;
import co.edu.uniquindio.cocinasinestres.perfil.domain.Rol;
import co.edu.uniquindio.cocinasinestres.perfil.domain.UsuarioAutenticado;
import co.edu.uniquindio.cocinasinestres.perfil.service.UsuarioActualService;
import co.edu.uniquindio.cocinasinestres.recetario.dto.RecetaDetalleResponse;
import co.edu.uniquindio.cocinasinestres.recetario.service.RecetaService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
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
 * El curador crea, edita y desactiva recetas sin tocar código (RNF-28), contra un MySQL de
 * verdad con el catálogo de las migraciones: la receta aparece en el recetario de las
 * usuarias y cada cambio queda en la auditoría a su nombre (RNF-24).
 *
 * <p>Necesita Docker en ejecución, como las demás pruebas de integración.</p>
 */
@SpringBootTest
@ActiveProfiles("dev")
@Testcontainers
@DisplayName("Recetas que crea y edita el curador (RNF-28)")
class RecetaCuradaIntegracionTest {

    @Container
    @ServiceConnection
    static final MySQLContainer MYSQL =
            new MySQLContainer(DockerImageName.parse("mysql:8.4")).withEnv("TZ", "UTC");

    @Autowired
    private RecetaCuradaService servicio;

    @Autowired
    private RecetaService recetario;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private UsuarioActualService usuarioActualService;

    private Long curador;

    @BeforeEach
    void comoCurador() {
        jdbcTemplate.update("""
            INSERT IGNORE INTO usuario (firebase_uid, correo, rol)
            VALUES ('uid-curador-recetas', 'curador-recetas@dev.local', 'CURADOR')
            """);
        curador = jdbcTemplate.queryForObject(
            "SELECT id FROM usuario WHERE firebase_uid = 'uid-curador-recetas'", Long.class);
        when(usuarioActualService.obtener()).thenReturn(
            new UsuarioAutenticado(curador, "uid-curador-recetas", "curador-recetas@dev.local", Rol.CURADOR, false));
    }

    private Long producto(String nombre) {
        return jdbcTemplate.queryForObject(
            "SELECT id FROM producto WHERE hogar_id IS NULL AND activo = TRUE AND nombre = ? ORDER BY id LIMIT 1",
            Long.class, nombre);
    }

    private IngredienteCuradoRequest ingrediente(String nombre, String cantidad) {
        return new IngredienteCuradoRequest(producto(nombre), new BigDecimal(cantidad), false, false);
    }

    private RecetaCuradaRequest almuerzo(String nombre, List<String> pasos) {
        return new RecetaCuradaRequest(
            nombre,
            "Hígado encebollado con arroz, como en casa.",
            40,
            4,
            pasos,
            null,
            List.of(
                new IngredienteCuradoRequest(producto("Carne de res"), new BigDecimal("500"), false, true),
                ingrediente("Arroz", "400"),
                ingrediente("Cebolla", "200"),
                ingrediente("Aceite vegetal", "30"),
                ingrediente("Sal", "10"),
                new IngredienteCuradoRequest(producto("Cilantro"), new BigDecimal("10"), true, false)),
            List.of("CENA", "ALMUERZO"),
            true,
            false);
    }

    private static final List<String> PASOS = List.of(
        "Corta la carne en tiras y échale la mitad de la sal.",
        "Lava el arroz y ponlo en una olla con 800 ml de agua y una cucharada de aceite.",
        "Cuando el agua se seque, tapa y cocina 15 minutos a fuego bajo.",
        "Dora la carne con la cebolla en el resto del aceite durante 10 minutos.",
        "Sirve con el arroz.");

    @Test
    @DisplayName("una receta nueva aparece en el recetario con sus comidas y queda auditada")
    void creaYApareceEnElRecetario() {
        RecetaCuradaResponse creada = servicio.crear(almuerzo("  Carne encebollada   con arroz ", PASOS));

        assertThat(creada.nombre()).isEqualTo("Carne encebollada con arroz");
        assertThat(creada.momentos()).containsExactly("ALMUERZO", "CENA");
        assertThat(creada.ingredientes()).hasSize(6);
        assertThat(creada.costo()).isPositive();

        RecetaDetalleResponse enElRecetario = recetario.obtenerPorId(creada.id());
        assertThat(enElRecetario.pasos()).hasSize(5);
        assertThat(enElRecetario.momentos()).containsExactly("ALMUERZO", "CENA");
        assertThat(recetario.listarConDetalle()).extracting(RecetaDetalleResponse::id).contains(creada.id());

        assertThat(jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM auditoria_catalogo WHERE entidad = 'RECETA' AND entidad_id = ? AND accion = 'CREAR' AND usuario_id = ?",
            Integer.class, creada.id(), curador)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT modificado_por FROM receta WHERE id = ?", Long.class, creada.id()))
            .isEqualTo(curador);
    }

    @Test
    @DisplayName("editar cambia ingredientes y pasos y deja el antes y el después en la auditoría")
    void editaYAudita() {
        RecetaCuradaResponse creada = servicio.crear(almuerzo("Carne en bistec de la casa", PASOS));
        RecetaCuradaRequest cambio = new RecetaCuradaRequest(
            creada.nombre(), creada.descripcion(), 45, 4, PASOS, "Saca la carne de la nevera una hora antes.",
            List.of(
                ingrediente("Carne de res", "600"),
                ingrediente("Arroz", "400"),
                ingrediente("Cebolla", "200"),
                ingrediente("Tomate", "200"),
                ingrediente("Sal", "10")),
            List.of("ALMUERZO"), true, true);

        RecetaCuradaResponse editada = servicio.actualizar(creada.id(), cambio);

        assertThat(editada.tiempoMinutos()).isEqualTo(45);
        assertThat(editada.ingredientes()).extracting("nombre").contains("Tomate").doesNotContain("Aceite vegetal");
        assertThat(editada.momentos()).containsExactly("ALMUERZO");
        assertThat(editada.gustaANinos()).isTrue();
        assertThat(jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM auditoria_catalogo WHERE entidad_id = ? AND accion = 'EDITAR' AND usuario_id = ? "
                + "AND JSON_CONTAINS_PATH(cambios, 'all', '$.ingredientes', '$.tiempo_minutos', '$.momentos')",
            Integer.class, creada.id(), curador)).isEqualTo(1);

        // Guardar lo mismo no escribe otra auditoría.
        servicio.actualizar(creada.id(), cambio);
        assertThat(jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM auditoria_catalogo WHERE entidad_id = ? AND accion = 'EDITAR'",
            Integer.class, creada.id())).isEqualTo(1);
    }

    @Test
    @DisplayName("desactivar la saca del recetario y reactivarla la devuelve")
    void desactivaYReactiva() {
        RecetaCuradaResponse creada = servicio.crear(almuerzo("Carne para desactivar", PASOS));

        assertThat(servicio.desactivar(creada.id()).activa()).isFalse();
        assertThat(recetario.listar()).extracting("id").doesNotContain(creada.id());
        assertThat(servicio.listar()).anyMatch(r -> r.id().equals(creada.id()) && !r.activa());

        assertThat(servicio.reactivar(creada.id()).activa()).isTrue();
        assertThat(recetario.listar()).extracting("id").contains(creada.id());
    }

    @Test
    @DisplayName("un almuerzo sin proteína o sin algo que llene no se acepta")
    void almuerzoIncompleto() {
        RecetaCuradaRequest pericos = new RecetaCuradaRequest(
            "Pericos de almuerzo", null, 15, 2, List.of("Revuelve los huevos con tomate y cebolla."), null,
            List.of(ingrediente("Huevos", "4"), ingrediente("Tomate", "100"), ingrediente("Cebolla", "50")),
            List.of("ALMUERZO"), true, false);

        assertThatThrownBy(() -> servicio.crear(pericos))
            .isInstanceOf(DatosInvalidosException.class)
            .hasFieldOrPropertyWithValue("codigo", "RECETA_ALMUERZO_INCOMPLETO");
    }

    @Test
    @DisplayName("un paso con dos tiempos, un nombre repetido o un producto que no existe no se aceptan")
    void validaciones() {
        assertThatThrownBy(() -> servicio.crear(almuerzo("Carne con dos tiempos",
            List.of("Sofríe la cebolla 5 minutos y luego cocina 10 minutos."))))
            .hasFieldOrPropertyWithValue("codigo", "RECETA_PASO_DOS_TIEMPOS");

        assertThatThrownBy(() -> servicio.crear(almuerzo("  arroz CON pollo y verduras ", PASOS)))
            .isInstanceOf(ConflictoException.class)
            .hasFieldOrPropertyWithValue("codigo", "RECETA_DUPLICADA");

        RecetaCuradaRequest sinProducto = new RecetaCuradaRequest(
            "Receta con producto inventado", null, 10, 2, List.of("Mezcla todo."), null,
            List.of(new IngredienteCuradoRequest(999_999L, BigDecimal.ONE, false, false)),
            List.of("CENA"), true, false);
        assertThatThrownBy(() -> servicio.crear(sinProducto))
            .hasFieldOrPropertyWithValue("codigo", "RECETA_INGREDIENTE_INVALIDO");
    }
}
