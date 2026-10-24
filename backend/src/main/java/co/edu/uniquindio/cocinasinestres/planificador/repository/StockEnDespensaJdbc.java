package co.edu.uniquindio.cocinasinestres.planificador.repository;

import co.edu.uniquindio.cocinasinestres.planificador.domain.StockEnDespensa;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Lee el stock del hogar directamente de la tabla {@code item_despensa}, sin usar
 * la entidad de despensa (RNF-26).
 *
 * <p>La regla de vencimiento es la misma que en despensa: un producto sirve hasta
 * el día que vence inclusive; desde el día siguiente ya no se cuenta.</p>
 */
@Repository
public class StockEnDespensaJdbc implements StockEnDespensa {

    private static final String SQL = """
        SELECT producto_id, SUM(cantidad) AS disponible
        FROM item_despensa
        WHERE hogar_id = :hogarId
          AND (vence_en IS NULL OR vence_en >= :hoy)
        GROUP BY producto_id
        """;

    private static final String SQL_POR_VENCER = """
        SELECT i.producto_id, p.nombre
        FROM item_despensa i
        JOIN producto p ON p.id = i.producto_id
        WHERE i.hogar_id = :hogarId
          AND i.cantidad > 0
          AND i.vence_en BETWEEN :hoy AND :hasta
        ORDER BY i.vence_en, p.nombre
        """;

    private final JdbcClient jdbc;

    public StockEnDespensaJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Map<Long, BigDecimal> disponibles(Long hogarId, LocalDate hoy) {
        Map<Long, BigDecimal> stock = new HashMap<>();
        jdbc.sql(SQL)
            .param("hogarId", hogarId)
            .param("hoy", hoy)
            .query((rs, rowNum) -> Map.entry(
                rs.getLong("producto_id"),
                rs.getBigDecimal("disponible")))
            .list()
            .forEach(fila -> stock.put(fila.getKey(), fila.getValue()));
        return stock;
    }

    @Override
    public Map<Long, String> porVencer(Long hogarId, LocalDate hoy, LocalDate hasta) {
        Map<Long, String> productos = new LinkedHashMap<>();
        jdbc.sql(SQL_POR_VENCER)
            .param("hogarId", hogarId)
            .param("hoy", hoy)
            .param("hasta", hasta)
            .query((rs, rowNum) -> Map.entry(rs.getLong("producto_id"), rs.getString("nombre")))
            .list()
            .forEach(fila -> productos.putIfAbsent(fila.getKey(), fila.getValue()));
        return productos;
    }
}
