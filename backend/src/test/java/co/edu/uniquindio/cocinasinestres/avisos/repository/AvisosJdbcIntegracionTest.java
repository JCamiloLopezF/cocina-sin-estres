package co.edu.uniquindio.cocinasinestres.avisos.repository;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.uniquindio.cocinasinestres.avisos.domain.AlmacenAvisos;
import co.edu.uniquindio.cocinasinestres.avisos.domain.Aviso;
import co.edu.uniquindio.cocinasinestres.avisos.domain.EstadoSemana;
import co.edu.uniquindio.cocinasinestres.avisos.domain.FuenteAvisos;
import co.edu.uniquindio.cocinasinestres.avisos.domain.ProductoAviso;
import co.edu.uniquindio.cocinasinestres.avisos.domain.TipoAviso;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
 * Las consultas del motor de avisos contra un MySQL de verdad (V11): la idempotencia con
 * la clave UNIQUE, las preferencias y la lectura de la despensa y del plan.
 *
 * <p>Necesita Docker en ejecución, como las demás pruebas de integración.</p>
 */
@SpringBootTest
@ActiveProfiles("dev")
@Testcontainers
@DisplayName("Motor de avisos contra MySQL")
class AvisosJdbcIntegracionTest {

    private static final String UID = "uid-avisos-de-prueba";
    private static final LocalDate HOY = LocalDate.of(2026, 10, 23);
    private static final LocalDate LUNES = LocalDate.of(2026, 10, 26);

    @Container
    @ServiceConnection
    static final MySQLContainer MYSQL =
            new MySQLContainer(DockerImageName.parse("mysql:8.4")).withEnv("TZ", "UTC");

    @Autowired
    private FuenteAvisos fuente;

    @Autowired
    private AlmacenAvisos almacen;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long usuarioId;
    private Long hogarId;

    @BeforeEach
    void crearHogarDePrueba() {
        jdbcTemplate.update("INSERT INTO usuario (firebase_uid, correo) VALUES (?, 'avisos@dev.local')", UID);
        usuarioId = jdbcTemplate.queryForObject("SELECT id FROM usuario WHERE firebase_uid = ?", Long.class, UID);
        jdbcTemplate.update("""
            INSERT INTO hogar (usuario_id, presupuesto_monto, periodicidad_ingreso,
                               dia_pago_1, sin_restricciones)
            VALUES (?, 500000, 'SEMANAL', 1, TRUE)
            """, usuarioId);
        hogarId = jdbcTemplate.queryForObject("SELECT id FROM hogar WHERE usuario_id = ?", Long.class, usuarioId);
    }

    @AfterEach
    void borrarHogarDePrueba() {
        jdbcTemplate.update("DELETE FROM hogar WHERE id = ?", hogarId);
        jdbcTemplate.update("DELETE FROM usuario WHERE firebase_uid = ?", UID);
    }

    private Long agregarADespensa(String nombre, String cantidad, String minimo, LocalDate venceEn) {
        jdbcTemplate.update("""
            INSERT INTO producto (hogar_id, nombre, categoria, tipo_cantidad,
                                  unidad_base, capacidad_referencia)
            VALUES (?, ?, 'Granos', 'GRANEL', 'g', 1000)
            """, hogarId, nombre);
        Long productoId = jdbcTemplate.queryForObject(
            "SELECT id FROM producto WHERE hogar_id = ? AND nombre = ?", Long.class, hogarId, nombre);
        jdbcTemplate.update(
            "INSERT INTO item_despensa (hogar_id, producto_id, cantidad, cantidad_minima, vence_en) VALUES (?, ?, ?, ?, ?)",
            hogarId, productoId, cantidad, minimo, venceEn);
        return productoId;
    }

    private static Aviso aviso(String clave, String titulo) {
        return new Aviso(TipoAviso.POR_VENCER, clave, titulo, "Úsalo primero.", "/despensa?filtro=POR_VENCER", "Ver en la despensa");
    }

    @Test
    @DisplayName("encuentra el hogar de la usuaria")
    void hogar() {
        assertThat(fuente.hogarDe(usuarioId)).contains(hogarId);
        assertThat(fuente.hogarDe(-1L)).isEmpty();
    }

    @Test
    @DisplayName("bajo el mínimo es llegar al mínimo, como en la despensa")
    void bajoMinimo() {
        agregarADespensa("Arroz", "100.000", "100.000", null);
        agregarADespensa("Sal", "50.000", "100.000", null);
        agregarADespensa("Lentejas", "500.000", "100.000", null);
        agregarADespensa("Azúcar", "10.000", null, null);

        assertThat(fuente.bajoMinimo(hogarId)).extracting(ProductoAviso::nombre).containsExactly("Arroz", "Sal");
    }

    @Test
    @DisplayName("por vencer: con cantidad y dentro de la ventana, del más próximo al último")
    void porVencer() {
        agregarADespensa("Leche", "1000.000", null, HOY.plusDays(1));
        agregarADespensa("Pollo", "800.000", null, HOY);
        agregarADespensa("Queso", "0.000", null, HOY);
        agregarADespensa("Yogur", "500.000", null, HOY.minusDays(1));
        agregarADespensa("Avena", "500.000", null, HOY.plusDays(10));

        assertThat(fuente.porVencer(hogarId, HOY, HOY.plusDays(3)))
            .extracting(ProductoAviso::nombre, ProductoAviso::venceEn)
            .containsExactly(
                org.assertj.core.groups.Tuple.tuple("Pollo", HOY),
                org.assertj.core.groups.Tuple.tuple("Leche", HOY.plusDays(1)));
    }

    @Test
    @DisplayName("el mismo aviso no se repite; sin leer se actualiza, leído se queda como está")
    void idempotencia() {
        String clave = "POR_VENCER:" + hogarId + ":" + HOY;
        almacen.guardar(hogarId, aviso(clave, "Leche vence mañana"));
        almacen.guardar(hogarId, aviso(clave, "2 productos vencen pronto"));

        List<AlmacenAvisos.AvisoGuardado> sinLeer = almacen.sinLeer(hogarId, List.of(clave));
        assertThat(sinLeer).singleElement().satisfies(a -> {
            assertThat(a.titulo()).isEqualTo("2 productos vencen pronto");
            assertThat(a.tipo()).isEqualTo(TipoAviso.POR_VENCER);
            assertThat(a.enlace()).isEqualTo("/despensa?filtro=POR_VENCER");
        });

        assertThat(almacen.marcarLeido(hogarId, sinLeer.get(0).id())).isTrue();
        almacen.guardar(hogarId, aviso(clave, "3 productos vencen pronto"));

        assertThat(almacen.sinLeer(hogarId, List.of(clave))).isEmpty();
        assertThat(almacen.desde(hogarId, LocalDateTime.of(2000, 1, 1, 0, 0), 10)).singleElement()
            .satisfies(a -> {
                assertThat(a.leido()).isTrue();
                assertThat(a.titulo()).isEqualTo("2 productos vencen pronto");
            });
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM aviso WHERE hogar_id = ?", Integer.class, hogarId))
            .isEqualTo(1);
    }

    @Test
    @DisplayName("no se marca como leído el aviso de otro hogar")
    void avisoDeOtroHogar() {
        almacen.guardar(hogarId, aviso("POR_VENCER:" + hogarId + ":x", "Mío"));
        Long id = almacen.sinLeer(hogarId, List.of("POR_VENCER:" + hogarId + ":x")).get(0).id();

        assertThat(almacen.marcarLeido(hogarId + 1000, id)).isFalse();
        assertThat(almacen.marcarTodosLeidos(hogarId)).isEqualTo(1);
        assertThat(almacen.sinLeer(hogarId, List.of())).isEmpty();
    }

    @Test
    @DisplayName("un tipo se apaga y se vuelve a encender")
    void preferencias() {
        assertThat(fuente.apagados(hogarId)).isEmpty();

        almacen.cambiarPreferencia(hogarId, TipoAviso.POR_VENCER, false);
        almacen.cambiarPreferencia(hogarId, TipoAviso.BAJO_MINIMO, false);
        assertThat(fuente.apagados(hogarId)).containsExactlyInAnyOrder(TipoAviso.POR_VENCER, TipoAviso.BAJO_MINIMO);

        almacen.cambiarPreferencia(hogarId, TipoAviso.POR_VENCER, true);
        assertThat(fuente.apagados(hogarId)).containsExactly(TipoAviso.BAJO_MINIMO);
    }

    @Test
    @DisplayName("lee la semana: sin plan, vacía, borrador y la preparación previa de mañana")
    void semanaYPreparacion() {
        assertThat(fuente.semana(hogarId, LUNES)).isEqualTo(EstadoSemana.SIN_PLAN);

        jdbcTemplate.update("INSERT INTO plan_semanal (hogar_id, semana_inicio, borrador) VALUES (?, ?, TRUE)", hogarId, LUNES);
        Long planId = jdbcTemplate.queryForObject(
            "SELECT id FROM plan_semanal WHERE hogar_id = ? AND semana_inicio = ?", Long.class, hogarId, LUNES);
        assertThat(fuente.semana(hogarId, LUNES).situacion()).isEqualTo(EstadoSemana.Situacion.VACIA);

        Long conRemojo = jdbcTemplate.queryForObject(
            "SELECT id FROM receta WHERE preparacion_previa IS NOT NULL AND activa = TRUE ORDER BY id LIMIT 1", Long.class);
        jdbcTemplate.update(
            "INSERT INTO comida_plan (plan_semanal_id, dia, tipo_comida, receta_id) VALUES (?, 1, 'ALMUERZO', ?)", planId, conRemojo);

        assertThat(fuente.semana(hogarId, LUNES)).isEqualTo(new EstadoSemana(EstadoSemana.Situacion.BORRADOR, 1));
        assertThat(fuente.preparacionesPara(hogarId, LUNES)).singleElement().satisfies(p -> {
            assertThat(p.dia()).isEqualTo(1);
            assertThat(p.indicacion()).isNotBlank();
        });
        assertThat(fuente.preparacionesPara(hogarId, LUNES.plusDays(1))).isEmpty();

        jdbcTemplate.update("UPDATE plan_semanal SET borrador = FALSE WHERE id = ?", planId);
        assertThat(fuente.semana(hogarId, LUNES).situacion()).isEqualTo(EstadoSemana.Situacion.PLANEADA);
    }
}
