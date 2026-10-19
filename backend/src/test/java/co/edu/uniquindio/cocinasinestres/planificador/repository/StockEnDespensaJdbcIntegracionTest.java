package co.edu.uniquindio.cocinasinestres.planificador.repository;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.uniquindio.cocinasinestres.planificador.domain.StockEnDespensa;
import java.math.BigDecimal;
import java.time.LocalDate;
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
 * Prueba el cruce del planificador con la despensa contra un MySQL de verdad,
 * sobre todo la regla de vencimiento del SQL.
 *
 * <p>Necesita Docker en ejecución, como la prueba de seguridad.</p>
 */
@SpringBootTest
@ActiveProfiles("dev")
@Testcontainers
@DisplayName("Stock en despensa que descuenta la lista de compras")
class StockEnDespensaJdbcIntegracionTest {

    private static final String UID = "uid-stock-de-prueba";

    /** Día fijo de la prueba; los vencimientos se calculan a partir de él. */
    private static final LocalDate HOY = LocalDate.of(2026, 10, 18);

    @Container
    @ServiceConnection
    static final MySQLContainer MYSQL =
            new MySQLContainer(DockerImageName.parse("mysql:8.4")).withEnv("TZ", "UTC");

    @Autowired
    private StockEnDespensa stockEnDespensa;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long hogarId;

    @BeforeEach
    void crearHogarDePrueba() {
        jdbcTemplate.update(
            "INSERT INTO usuario (firebase_uid, correo) VALUES (?, 'stock@dev.local')", UID);
        Long usuarioId = jdbcTemplate.queryForObject(
            "SELECT id FROM usuario WHERE firebase_uid = ?", Long.class, UID);
        jdbcTemplate.update("""
            INSERT INTO hogar (usuario_id, presupuesto_monto, periodicidad_ingreso,
                               dia_pago_1, sin_restricciones)
            VALUES (?, 500000, 'SEMANAL', 1, TRUE)
            """, usuarioId);
        hogarId = jdbcTemplate.queryForObject(
            "SELECT id FROM hogar WHERE usuario_id = ?", Long.class, usuarioId);
    }

    @AfterEach
    void borrarHogarDePrueba() {
        // Los productos y items del hogar se borran en cascada con el hogar.
        jdbcTemplate.update("DELETE FROM hogar WHERE id = ?", hogarId);
        jdbcTemplate.update("DELETE FROM usuario WHERE firebase_uid = ?", UID);
    }

    /** Registra un producto "granel" del hogar y un ítem de despensa con su vencimiento. */
    private Long agregarADespensa(String nombre, String cantidad, LocalDate venceEn) {
        jdbcTemplate.update("""
            INSERT INTO producto (hogar_id, nombre, categoria, tipo_cantidad,
                                  unidad_base, capacidad_referencia)
            VALUES (?, ?, 'Granos', 'GRANEL', 'g', 1000)
            """, hogarId, nombre);
        Long productoId = jdbcTemplate.queryForObject(
            "SELECT id FROM producto WHERE hogar_id = ? AND nombre = ?", Long.class, hogarId, nombre);
        jdbcTemplate.update(
            "INSERT INTO item_despensa (hogar_id, producto_id, cantidad, vence_en) VALUES (?, ?, ?, ?)",
            hogarId, productoId, cantidad, venceEn);
        return productoId;
    }

    @Test
    @DisplayName("lo vencido no cuenta; lo que vence hoy y lo sin fecha sí")
    void descuentaLoVigentePeroNoLoVencido() {
        Long vigente = agregarADespensa("Arroz", "300.000", HOY.plusDays(3));
        Long venceHoy = agregarADespensa("Lentejas", "150.000", HOY);
        Long vencido = agregarADespensa("Leche", "200.000", HOY.minusDays(1));
        Long sinFecha = agregarADespensa("Sal", "50.000", null);

        var stock = stockEnDespensa.disponibles(hogarId, HOY);

        assertThat(stock).containsOnlyKeys(vigente, venceHoy, sinFecha);
        assertThat(stock).containsEntry(vigente, new BigDecimal("300.000"));
        assertThat(stock).containsEntry(venceHoy, new BigDecimal("150.000"));
        assertThat(stock).doesNotContainKey(vencido);
    }

    @Test
    @DisplayName("el stock de otro hogar no se cuenta")
    void noCuentaLaDespensaDeOtroHogar() {
        Long producto = agregarADespensa("Arroz", "300.000", null);

        jdbcTemplate.update(
            "INSERT INTO usuario (firebase_uid, correo) VALUES ('uid-otro-hogar', 'otra@dev.local')");
        Long otroUsuarioId = jdbcTemplate.queryForObject(
            "SELECT id FROM usuario WHERE firebase_uid = 'uid-otro-hogar'", Long.class);
        jdbcTemplate.update("""
            INSERT INTO hogar (usuario_id, presupuesto_monto, periodicidad_ingreso,
                               dia_pago_1, sin_restricciones)
            VALUES (?, 300000, 'MENSUAL', 1, TRUE)
            """, otroUsuarioId);
        Long otroHogarId = jdbcTemplate.queryForObject(
            "SELECT id FROM hogar WHERE usuario_id = ?", Long.class, otroUsuarioId);
        jdbcTemplate.update(
            "INSERT INTO item_despensa (hogar_id, producto_id, cantidad) VALUES (?, ?, 999.000)",
            otroHogarId, producto);

        var stock = stockEnDespensa.disponibles(hogarId, HOY);

        assertThat(stock).containsOnlyKeys(producto);
        assertThat(stock).containsEntry(producto, new BigDecimal("300.000"));

        jdbcTemplate.update("DELETE FROM hogar WHERE id = ?", otroHogarId);
        jdbcTemplate.update("DELETE FROM usuario WHERE firebase_uid = 'uid-otro-hogar'");
    }
}
