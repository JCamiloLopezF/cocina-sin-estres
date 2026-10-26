package co.edu.uniquindio.cocinasinestres.recetario.repository;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.uniquindio.cocinasinestres.recetario.domain.PasosReceta;
import java.util.ArrayList;
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
 * Los pasos del catálogo que cargan las migraciones (V3, V4 y V12) cumplen la regla del
 * Modo cocinar: como máximo un tiempo por paso, para que el temporizador sepa qué contar.
 * Si una receta nueva trae un paso con dos tiempos, esta prueba dice cuál partir.
 *
 * <p>Necesita Docker en ejecución, como las demás pruebas de integración.</p>
 */
@SpringBootTest
@ActiveProfiles("dev")
@Testcontainers
@DisplayName("Pasos de las recetas del catálogo")
class PasosRecetasIntegracionTest {

    @Container
    @ServiceConnection
    static final MySQLContainer MYSQL =
            new MySQLContainer(DockerImageName.parse("mysql:8.4")).withEnv("TZ", "UTC");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("ningún paso pide dos tiempos")
    void cadaPasoTieneComoMaximoUnTiempo() {
        List<Map<String, Object>> recetas = jdbcTemplate.queryForList("SELECT nombre, pasos FROM receta");
        List<String> pasosConVariosTiempos = new ArrayList<>();

        for (Map<String, Object> receta : recetas) {
            for (String paso : PasosReceta.dividir((String) receta.get("pasos"))) {
                List<String> tiempos = PasosReceta.tiemposDelPaso(paso);
                if (tiempos.size() > 1) {
                    pasosConVariosTiempos.add(receta.get("nombre") + ": " + paso + " " + tiempos);
                }
            }
        }

        assertThat(recetas).hasSizeGreaterThanOrEqualTo(21);
        assertThat(pasosConVariosTiempos).as("Pasos que hay que partir en dos").isEmpty();
    }

    @Test
    @DisplayName("ningún paso remite a otra parte ni da un rango de tiempo")
    void losPasosSonConcretos() {
        List<String> ambiguos = jdbcTemplate.queryForList("""
            SELECT nombre
            FROM receta
            WHERE pasos LIKE '%según%'
               OR pasos LIKE '%instrucciones%'
               OR pasos REGEXP '[0-9]+ ?(-|a) ?[0-9]+ minutos'
            """, String.class);

        assertThat(ambiguos).isEmpty();
    }
}
