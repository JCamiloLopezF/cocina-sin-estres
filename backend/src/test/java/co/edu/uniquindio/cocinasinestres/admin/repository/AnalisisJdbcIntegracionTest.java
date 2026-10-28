package co.edu.uniquindio.cocinasinestres.admin.repository;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.uniquindio.cocinasinestres.admin.domain.DatosAnalisis;
import co.edu.uniquindio.cocinasinestres.admin.domain.DatosLogin;
import co.edu.uniquindio.cocinasinestres.admin.domain.FuenteAnalisis;
import co.edu.uniquindio.cocinasinestres.avisos.domain.AlmacenAvisos;
import co.edu.uniquindio.cocinasinestres.planificador.domain.RegistroUso;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
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
 * El registro de uso (V15) y las consultas del análisis del administrador (KAN-54) contra
 * un MySQL de verdad: lo que anota la app al cocinar, al abrir el modo rescate y al abrir
 * la app es lo que después cuenta el análisis.
 *
 * <p>Necesita Docker en ejecución, como las demás pruebas de integración.</p>
 */
@SpringBootTest
@ActiveProfiles("dev")
@Testcontainers
@DisplayName("Análisis del administrador contra MySQL")
class AnalisisJdbcIntegracionTest {

    @Container
    @ServiceConnection
    static final MySQLContainer MYSQL =
            new MySQLContainer(DockerImageName.parse("mysql:8.4")).withEnv("TZ", "UTC");

    @Autowired
    private FuenteAnalisis fuente;

    @Autowired
    private RegistroUso registroUso;

    @Autowired
    private AlmacenAvisos almacenAvisos;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("lo que anota la app al usarla aparece en el análisis")
    void registraYCuenta() {
        LocalDate lunes = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        jdbcTemplate.update(
            "INSERT INTO usuario (firebase_uid, correo, perfil_completo) VALUES ('uid-analisis', 'analisis@dev.local', TRUE)");
        Long usuarioId = jdbcTemplate.queryForObject(
            "SELECT id FROM usuario WHERE firebase_uid = 'uid-analisis'", Long.class);
        jdbcTemplate.update("""
            INSERT INTO hogar (usuario_id, presupuesto_monto, periodicidad_ingreso, dia_pago_1, sin_restricciones)
            VALUES (?, 1000000, 'SEMANAL', 5, TRUE)
            """, usuarioId);
        Long hogarId = jdbcTemplate.queryForObject("SELECT id FROM hogar WHERE usuario_id = ?", Long.class, usuarioId);
        jdbcTemplate.update("INSERT INTO plan_semanal (hogar_id, semana_inicio) VALUES (?, ?)", hogarId, lunes);
        Long planId = jdbcTemplate.queryForObject(
            "SELECT id FROM plan_semanal WHERE hogar_id = ?", Long.class, hogarId);
        List<Long> recetas = jdbcTemplate.queryForList("SELECT id FROM receta ORDER BY id LIMIT 7", Long.class);

        // Con 13 comidas el plan todavía no está completo.
        for (int dia = 1; dia <= 7; dia++) {
            jdbcTemplate.update(
                "INSERT INTO comida_plan (plan_semanal_id, dia, tipo_comida, receta_id) VALUES (?, ?, 'ALMUERZO', ?)",
                planId, dia, recetas.get(dia - 1));
            if (dia < 7) {
                jdbcTemplate.update(
                    "INSERT INTO comida_plan (plan_semanal_id, dia, tipo_comida, receta_id) VALUES (?, ?, 'CENA', ?)",
                    planId, dia, recetas.get(dia % 7));
            }
        }
        registroUso.revisarPlanCompleto(planId);
        assertThat(completado(planId)).isZero();
        jdbcTemplate.update(
            "INSERT INTO comida_plan (plan_semanal_id, dia, tipo_comida, receta_id) VALUES (?, 7, 'CENA', ?)",
            planId, recetas.get(0));
        registroUso.revisarPlanCompleto(planId);
        assertThat(completado(planId)).isEqualTo(1);

        jdbcTemplate.update(
            "INSERT INTO item_despensa (hogar_id, producto_id, cantidad) SELECT ?, id, 500 FROM producto WHERE hogar_id IS NULL ORDER BY id LIMIT 6",
            hogarId);
        registroUso.preparacion(hogarId, recetas.get(0), null);
        registroUso.preparacion(hogarId, recetas.get(0), null);
        registroUso.preparacion(hogarId, recetas.get(1), null);
        registroUso.rescate(hogarId, 4);
        almacenAvisos.registrarApertura(hogarId, 3, 2, 5, 140);
        almacenAvisos.registrarApertura(hogarId, 1, 1, 4, 60);
        // Lo que mide la web (V16): tiempo para dejar la semana completa e inicios de sesión.
        jdbcTemplate.update("UPDATE plan_semanal SET segundos_planificacion = 150 WHERE id = ?", planId);
        jdbcTemplate.update(
            "INSERT INTO inicio_sesion (usuario_id, duracion_ms, fallidos_previos) VALUES (?, 900, 1), (?, 700, 0)",
            usuarioId, usuarioId);

        DatosAnalisis datos = fuente.leer(30, lunes, lunes.minusWeeks(7));

        assertThat(datos.usuarias()).isGreaterThanOrEqualTo(1);
        assertThat(datos.perfilesCompletos()).isGreaterThanOrEqualTo(1);
        assertThat(datos.activas7Dias()).isEqualTo(1);
        assertThat(datos.aperturas()).isEqualTo(2);
        assertThat(datos.avisosPorApertura()).isEqualTo(1.5);
        assertThat(datos.msPorApertura()).isEqualTo(100.0);
        assertThat(datos.aperturasCincoReglas()).isEqualTo(1);
        assertThat(datos.planesEstaSemana()).isEqualTo(1);
        assertThat(datos.preparaciones()).isEqualTo(3);
        assertThat(datos.preparacionesFuera()).isEqualTo(3);
        assertThat(datos.usosRescate()).isEqualTo(1);
        assertThat(datos.hogaresConDespensa()).isEqualTo(1);
        assertThat(datos.itemsDespensa()).isEqualTo(6);
        assertThat(datos.itemsDelCatalogo()).isEqualTo(6);
        assertThat(datos.planesCompletados()).isEqualTo(1);
        assertThat(datos.minutosParaArmar()).isEqualTo(2.5);
        assertThat(datos.planes()).singleElement()
            .satisfies(p -> assertThat(p.costo().signum()).isPositive());
        assertThat(datos.productosDisponibles()).isEqualTo(6);
        assertThat(datos.semanasPlaneadas()).isEqualTo(1);
        assertThat(datos.semanasConRescate()).isEqualTo(1);
        assertThat(datos.masPreparadas().get(0).veces()).isEqualTo(2);
        assertThat(datos.masPlaneadas()).isNotEmpty();
        assertThat(datos.sinUsar()).isNotEmpty();

        DatosLogin login = fuente.login(30);
        assertThat(login.exitosos()).isEqualTo(2);
        assertThat(login.fallidos()).isEqualTo(1);
        assertThat(login.msPromedio()).isEqualTo(800.0);
    }

    private Integer completado(Long planId) {
        return jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM plan_semanal WHERE id = ? AND completado_en IS NOT NULL", Integer.class, planId);
    }
}
