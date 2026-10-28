package co.edu.uniquindio.cocinasinestres.curaduria.service;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.uniquindio.cocinasinestres.curaduria.domain.CategoriaProducto;
import co.edu.uniquindio.cocinasinestres.curaduria.domain.ReglaAlmuerzo;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * El catálogo que cargan las migraciones (V3, V4, V17 y V18) respeta qué es un almuerzo
 * (ADR-24): toda receta de almuerzo tiene una proteína y algo que llene. También revisa que
 * haya variedad para las tres comidas y que todas las categorías sean de la lista.
 *
 * <p>Necesita Docker en ejecución, como las demás pruebas de integración.</p>
 */
@SpringBootTest
@ActiveProfiles("dev")
@Testcontainers
@DisplayName("Composición de los almuerzos del catálogo")
class ComposicionAlmuerzoIntegracionTest {

    @Container
    @ServiceConnection
    static final MySQLContainer MYSQL =
            new MySQLContainer(DockerImageName.parse("mysql:8.4")).withEnv("TZ", "UTC");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("todo almuerzo tiene proteína y algo que llene")
    void almuerzosCompletos() {
        Map<String, List<String>> categorias = new HashMap<>();
        jdbcTemplate.query("""
            SELECT r.nombre, p.categoria
            FROM receta r
            JOIN ingrediente_receta ir ON ir.receta_id = r.id AND ir.opcional = FALSE
            JOIN producto p ON p.id = ir.producto_id
            WHERE r.activa = TRUE AND r.es_plato_fuerte = TRUE AND r.para_almuerzo = TRUE
            """, rs -> {
                categorias.computeIfAbsent(rs.getString("nombre"), k -> new ArrayList<>()).add(rs.getString("categoria"));
            });

        List<String> incompletos = categorias.entrySet().stream()
            .filter(e -> !ReglaAlmuerzo.esCompleto(e.getValue()))
            .map(Map.Entry::getKey)
            .toList();

        assertThat(categorias).hasSizeGreaterThanOrEqualTo(35);
        assertThat(incompletos).as("Almuerzos sin proteína o sin algo que llene").isEmpty();
        assertThat(categorias).doesNotContainKey("Huevos pericos");
    }

    @Test
    @DisplayName("hay variedad para el desayuno, el almuerzo y la cena")
    void variedadPorComida() {
        Map<String, Object> cuentas = jdbcTemplate.queryForMap("""
            SELECT SUM(para_desayuno) AS desayunos, SUM(para_almuerzo) AS almuerzos, SUM(para_cena) AS cenas,
                   SUM(NOT (para_desayuno OR para_almuerzo OR para_cena)) AS sin_comida
            FROM receta
            WHERE activa = TRUE AND es_plato_fuerte = TRUE
            """);

        assertThat(((Number) cuentas.get("desayunos")).intValue()).isGreaterThanOrEqualTo(15);
        assertThat(((Number) cuentas.get("almuerzos")).intValue()).isGreaterThanOrEqualTo(35);
        assertThat(((Number) cuentas.get("cenas")).intValue()).isGreaterThanOrEqualTo(40);
        assertThat(((Number) cuentas.get("sin_comida")).intValue()).as("Platos fuertes sin comida asignada").isZero();
    }

    @Test
    @DisplayName("todas las categorías de producto son de la lista")
    void categoriasValidas() {
        List<String> categorias = jdbcTemplate.queryForList("SELECT DISTINCT categoria FROM producto", String.class);

        assertThat(CategoriaProducto.CODIGOS).containsAll(categorias);
    }
}
