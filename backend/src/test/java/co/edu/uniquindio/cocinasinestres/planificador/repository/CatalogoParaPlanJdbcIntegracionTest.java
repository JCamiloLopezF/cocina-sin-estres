package co.edu.uniquindio.cocinasinestres.planificador.repository;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.uniquindio.cocinasinestres.planificador.domain.CatalogoParaPlan;
import co.edu.uniquindio.cocinasinestres.planificador.domain.ContextoPlan;
import co.edu.uniquindio.cocinasinestres.planificador.domain.GeneradorPlan;
import co.edu.uniquindio.cocinasinestres.planificador.domain.Hueco;
import co.edu.uniquindio.cocinasinestres.planificador.domain.RecetaCandidata;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
 * El generador del plan (ADR-24) sobre el catálogo real que cargan las migraciones
 * V2 a V7, contra un MySQL de verdad. Necesita Docker en ejecución.
 */
@SpringBootTest
@ActiveProfiles("dev")
@Testcontainers
@DisplayName("Catálogo para el generador del plan")
class CatalogoParaPlanJdbcIntegracionTest {

    @Container
    @ServiceConnection
    static final MySQLContainer MYSQL =
            new MySQLContainer(DockerImageName.parse("mysql:8.4")).withEnv("TZ", "UTC");

    @Autowired
    private CatalogoParaPlan catalogo;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long recetaLlamada(String nombre) {
        return jdbcTemplate.queryForObject("SELECT id FROM receta WHERE nombre = ?", Long.class, nombre);
    }

    @Test
    @DisplayName("lee las recetas con costo, ingredientes, restricciones y si son plato fuerte")
    void leeElCatalogo() {
        Map<Long, RecetaCandidata> recetas = catalogo.recetas();
        Integer total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM receta", Integer.class);

        assertThat(recetas).hasSize(total);
        assertThat(recetas.get(recetaLlamada("Batido de banano y leche")).platoFuerte()).isFalse();
        assertThat(recetas.get(recetaLlamada("Ensalada de frutas")).platoFuerte()).isFalse();

        RecetaCandidata pericos = recetas.get(recetaLlamada("Huevos pericos"));
        assertThat(pericos.platoFuerte()).isTrue();
        assertThat(pericos.costo()).isPositive();
        assertThat(pericos.alergias()).contains("huevo");
        assertThat(pericos.ingredientes()).isNotEmpty();
        assertThat(pericos.gustaANinos()).isTrue();
        assertThat(recetas.get(recetaLlamada("Carne guisada con papa")).gustaANinos()).isFalse();
    }

    @Test
    @DisplayName("con alguien alérgico a la lactosa arma la semana sin leche ni queso")
    void semanaSinLactosa() {
        Set<Long> conLactosa = new HashSet<>(jdbcTemplate.queryForList("""
            SELECT DISTINCT ir.receta_id
            FROM ingrediente_receta ir
            JOIN producto p ON p.id = ir.producto_id
            WHERE ir.opcional = FALSE AND p.nombre IN ('Leche', 'Queso')
            """, Long.class));
        assertThat(conLactosa).isNotEmpty();

        ContextoPlan contexto = new ContextoPlan(
            catalogo.recetas(), Set.of("lactosa"), Set.of(3), BigDecimal.valueOf(195_000),
            Map.of(), Set.of(), Map.of());
        List<Hueco> semana = new ArrayList<>();
        for (int dia = 1; dia <= 7; dia++) {
            semana.add(new Hueco(dia, "ALMUERZO"));
            semana.add(new Hueco(dia, "CENA"));
        }

        Map<Hueco, Long> plan = new GeneradorPlan().completar(contexto, semana);

        assertThat(plan).hasSize(14);
        assertThat(plan.values()).doesNotContainAnyElementsOf(conLactosa);
        assertThat(catalogo.recetas().get(plan.get(new Hueco(3, "ALMUERZO"))).tiempoMinutos())
            .isLessThanOrEqualTo(GeneradorPlan.MINUTOS_DIA_SIN_TIEMPO);
    }
}
