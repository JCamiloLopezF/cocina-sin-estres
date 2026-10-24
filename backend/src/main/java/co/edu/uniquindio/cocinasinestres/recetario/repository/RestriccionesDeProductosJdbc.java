package co.edu.uniquindio.cocinasinestres.recetario.repository;

import co.edu.uniquindio.cocinasinestres.recetario.domain.RestriccionesDeProductos;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Lee {@code producto_restriccion} por JDBC, sin usar entidades de curaduría (RNF-26). */
@Repository
public class RestriccionesDeProductosJdbc implements RestriccionesDeProductos {

    private final JdbcClient jdbc;

    public RestriccionesDeProductosJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Map<Long, List<String>> codigosPorProducto(Collection<Long> productoIds) {
        Map<Long, List<String>> resultado = new LinkedHashMap<>();
        if (productoIds == null || productoIds.isEmpty()) {
            return resultado;
        }
        jdbc.sql("""
                SELECT pr.producto_id AS producto_id, r.codigo AS codigo
                FROM producto_restriccion pr
                JOIN restriccion r ON r.id = pr.restriccion_id
                WHERE pr.producto_id IN (:ids)
                ORDER BY r.codigo
                """)
            .param("ids", productoIds)
            .query((rs, fila) -> Map.entry(rs.getLong("producto_id"), rs.getString("codigo")))
            .list()
            .forEach(par -> resultado.computeIfAbsent(par.getKey(), id -> new ArrayList<>()).add(par.getValue()));
        return resultado;
    }
}
