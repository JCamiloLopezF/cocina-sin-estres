package co.edu.uniquindio.cocinasinestres.planificador.repository;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.uniquindio.cocinasinestres.planificador.domain.MovimientosDespensa;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
 * Evidencia de la métrica «Exactitud de existencias» (meta > 98 %) y de RNF-05 (0
 * desviaciones en al menos 20 casos de descuento): cada receta del catálogo se prepara
 * para un hogar de 1 a 4 personas y lo que queda en la despensa se compara con la cuenta
 * hecha a mano, {@code existencia - cantidad × personas / porciones}.
 *
 * <p>Necesita Docker en ejecución, como las demás pruebas de integración.</p>
 */
@SpringBootTest
@ActiveProfiles("dev")
@Testcontainers
@DisplayName("Exactitud del descuento de existencias (RNF-05)")
class ExactitudExistenciasIntegracionTest {

    private static final String UID = "uid-exactitud";
    private static final BigDecimal EXISTENCIA = new BigDecimal("10000");
    /** Lo que se tolera por redondear a 3 decimales, como guarda la base. */
    private static final BigDecimal TOLERANCIA = new BigDecimal("0.001");

    @Container
    @ServiceConnection
    static final MySQLContainer MYSQL =
            new MySQLContainer(DockerImageName.parse("mysql:8.4")).withEnv("TZ", "UTC");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MovimientosDespensa movimientos;

    private Long hogarId;

    @BeforeEach
    void crearHogar() {
        jdbcTemplate.update("INSERT INTO usuario (firebase_uid, correo) VALUES (?, 'exactitud@dev.local')", UID);
        Long usuarioId = jdbcTemplate.queryForObject("SELECT id FROM usuario WHERE firebase_uid = ?", Long.class, UID);
        jdbcTemplate.update("""
            INSERT INTO hogar (usuario_id, presupuesto_monto, periodicidad_ingreso, dia_pago_1, sin_restricciones)
            VALUES (?, 500000, 'SEMANAL', 1, TRUE)
            """, usuarioId);
        hogarId = jdbcTemplate.queryForObject("SELECT id FROM hogar WHERE usuario_id = ?", Long.class, usuarioId);
    }

    @AfterEach
    void borrarHogar() {
        jdbcTemplate.update("DELETE FROM hogar WHERE id = ?", hogarId);
        jdbcTemplate.update("DELETE FROM usuario WHERE firebase_uid = ?", UID);
    }

    @Test
    @DisplayName("cada receta del catálogo, para 1 a 4 personas: 0 desviaciones en más de 20 casos")
    void ceroDesviaciones() {
        List<Map<String, Object>> recetas = jdbcTemplate.queryForList(
            "SELECT id, nombre, porciones FROM receta WHERE activa = TRUE ORDER BY id");
        List<String> desviaciones = new ArrayList<>();
        int casos = 0;

        for (int i = 0; i < recetas.size(); i++) {
            Long receta = ((Number) recetas.get(i).get("id")).longValue();
            int porciones = ((Number) recetas.get(i).get("porciones")).intValue();
            int personas = i % 4 + 1;
            prepararHogar(personas);

            Map<Long, BigDecimal> pide = new HashMap<>();
            jdbcTemplate.queryForList(
                    "SELECT producto_id, cantidad FROM ingrediente_receta WHERE receta_id = ? AND opcional = FALSE", receta)
                .forEach(fila -> pide.merge(((Number) fila.get("producto_id")).longValue(),
                    new BigDecimal(fila.get("cantidad").toString()), BigDecimal::add));
            pide.keySet().forEach(producto -> jdbcTemplate.update(
                "INSERT INTO item_despensa (hogar_id, producto_id, cantidad) VALUES (?, ?, ?)", hogarId, producto, EXISTENCIA));

            movimientos.descontarReceta(hogarId, receta);

            for (Map.Entry<Long, BigDecimal> ingrediente : pide.entrySet()) {
                BigDecimal esperado = EXISTENCIA.subtract(ingrediente.getValue()
                    .multiply(BigDecimal.valueOf(personas))
                    .divide(BigDecimal.valueOf(porciones), 3, RoundingMode.HALF_UP));
                BigDecimal queda = jdbcTemplate.queryForObject(
                    "SELECT cantidad FROM item_despensa WHERE hogar_id = ? AND producto_id = ?",
                    BigDecimal.class, hogarId, ingrediente.getKey());
                if (queda.subtract(esperado).abs().compareTo(TOLERANCIA) > 0) {
                    desviaciones.add(recetas.get(i).get("nombre") + " (" + personas + " personas), producto "
                        + ingrediente.getKey() + ": quedó " + queda + ", se esperaba " + esperado);
                }
            }
            casos++;
        }

        assertThat(casos).isGreaterThanOrEqualTo(20);
        assertThat(desviaciones).as("Descuentos que no cuadran").isEmpty();
    }

    /** El hogar con ese número de personas y la despensa vacía. */
    private void prepararHogar(int personas) {
        jdbcTemplate.update("DELETE FROM item_despensa WHERE hogar_id = ?", hogarId);
        jdbcTemplate.update("DELETE FROM persona WHERE hogar_id = ?", hogarId);
        for (int p = 0; p < personas; p++) {
            jdbcTemplate.update("INSERT INTO persona (hogar_id, nombre, edad) VALUES (?, NULL, 30)", hogarId);
        }
    }
}
