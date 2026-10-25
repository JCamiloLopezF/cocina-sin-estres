package co.edu.uniquindio.cocinasinestres.planificador.repository;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.uniquindio.cocinasinestres.planificador.domain.MovimientosDespensa;
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

    @Autowired
    private MovimientosDespensa movimientos;

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

    @Test
    @DisplayName("por vencer: lo que vence entre hoy y el tope, con cantidad, en orden de vencimiento")
    void productosPorVencer() {
        Long manana = agregarADespensa("Arroz", "300.000", HOY.plusDays(1));
        Long hoy = agregarADespensa("Lentejas", "150.000", HOY);
        agregarADespensa("Frijol", "100.000", HOY.plusDays(5));
        agregarADespensa("Leche", "200.000", HOY.minusDays(1));
        agregarADespensa("Pasta", "0.000", HOY.plusDays(1));
        agregarADespensa("Sal", "50.000", null);

        var porVencer = stockEnDespensa.porVencer(hogarId, HOY, HOY.plusDays(3));

        assertThat(porVencer.keySet()).containsExactly(hoy, manana);
        assertThat(porVencer).containsEntry(manana, "Arroz");
    }

    // ---------------------------------------------------------------------
    // Despensa automática (ADR-24, KAN-16)
    // ---------------------------------------------------------------------

    private BigDecimal cantidadDe(Long productoId) {
        return jdbcTemplate.queryForObject(
            "SELECT cantidad FROM item_despensa WHERE hogar_id = ? AND producto_id = ?",
            BigDecimal.class, hogarId, productoId);
    }

    private LocalDate venceDe(Long productoId) {
        return jdbcTemplate.queryForObject(
            "SELECT vence_en FROM item_despensa WHERE hogar_id = ? AND producto_id = ?",
            LocalDate.class, hogarId, productoId);
    }

    @Test
    @DisplayName("lo comprado entra a la despensa; si ya había, suma y conserva el vencimiento más próximo")
    void agregarLoComprado() {
        Long arroz = agregarADespensa("Arroz", "200.000", HOY.plusDays(10));

        LocalDate vence = movimientos.agregar(hogarId, arroz, new BigDecimal("1000.000"), HOY.plusDays(365), HOY);

        assertThat(cantidadDe(arroz)).isEqualByComparingTo("1200");
        assertThat(vence).isEqualTo(HOY.plusDays(10));
        assertThat(venceDe(arroz)).isEqualTo(HOY.plusDays(10));
    }

    @Test
    @DisplayName("si lo que había ya se venció o se acabó, queda el vencimiento de lo nuevo")
    void agregarSobreLoVencido() {
        Long leche = agregarADespensa("Leche", "200.000", HOY.minusDays(2));

        movimientos.agregar(hogarId, leche, new BigDecimal("1000.000"), HOY.plusDays(7), HOY);

        assertThat(venceDe(leche)).isEqualTo(HOY.plusDays(7));
    }

    @Test
    @DisplayName("un producto que no estaba en la despensa se crea, y deshacer la compra lo resta")
    void agregarNuevoYQuitar() {
        Long sal = agregarADespensa("Sal", "0.000", null);
        jdbcTemplate.update("DELETE FROM item_despensa WHERE hogar_id = ? AND producto_id = ?", hogarId, sal);

        movimientos.agregar(hogarId, sal, new BigDecimal("500.000"), null, HOY);
        assertThat(cantidadDe(sal)).isEqualByComparingTo("500");

        movimientos.quitar(hogarId, sal, new BigDecimal("800.000"));
        assertThat(cantidadDe(sal)).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("al preparar una receta se descuentan sus ingredientes obligatorios, sin bajar de cero")
    void descontarReceta() {
        Long receta = jdbcTemplate.queryForObject("SELECT id FROM receta WHERE nombre = 'Huevos pericos'", Long.class);
        Long huevos = jdbcTemplate.queryForObject(
            "SELECT id FROM producto WHERE hogar_id IS NULL AND nombre = 'Huevos'", Long.class);
        Long cebolla = jdbcTemplate.queryForObject(
            "SELECT id FROM producto WHERE hogar_id IS NULL AND nombre = 'Cebolla'", Long.class);
        BigDecimal huevosReceta = jdbcTemplate.queryForObject(
            "SELECT cantidad FROM ingrediente_receta WHERE receta_id = ? AND producto_id = ?",
            BigDecimal.class, receta, huevos);
        jdbcTemplate.update("INSERT INTO item_despensa (hogar_id, producto_id, cantidad) VALUES (?, ?, 30)", hogarId, huevos);
        jdbcTemplate.update("INSERT INTO item_despensa (hogar_id, producto_id, cantidad) VALUES (?, ?, 1)", hogarId, cebolla);

        int descontados = movimientos.descontarReceta(hogarId, receta);

        assertThat(descontados).isEqualTo(2);
        assertThat(cantidadDe(huevos)).isEqualByComparingTo(new BigDecimal("30").subtract(huevosReceta));
        assertThat(cantidadDe(cebolla)).isGreaterThanOrEqualTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("lo que quedó por debajo del mínimo se reporta con lo que falta para llegar a él")
    void bajoMinimo() {
        Long arroz = agregarADespensa("Arroz", "200.000", null);
        Long lentejas = agregarADespensa("Lentejas", "900.000", null);
        jdbcTemplate.update("UPDATE item_despensa SET cantidad_minima = 500 WHERE hogar_id = ?", hogarId);

        var faltan = stockEnDespensa.bajoMinimo(hogarId);

        assertThat(faltan).containsOnlyKeys(arroz);
        assertThat(faltan.get(arroz)).isEqualByComparingTo("300");
        assertThat(faltan).doesNotContainKey(lentejas);
    }

    @Test
    @DisplayName("los datos de compra traen la presentación y la vida útil del catálogo")
    void datosCompra() {
        Long arroz = jdbcTemplate.queryForObject(
            "SELECT id FROM producto WHERE hogar_id IS NULL AND nombre = 'Arroz'", Long.class);

        var datos = movimientos.datosCompra(arroz);

        assertThat(datos.presentacion()).isEqualByComparingTo("1000");
        assertThat(datos.vidaUtilDias()).isEqualTo(365);
    }
}
