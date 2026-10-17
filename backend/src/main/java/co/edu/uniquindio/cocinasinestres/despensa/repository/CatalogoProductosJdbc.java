package co.edu.uniquindio.cocinasinestres.despensa.repository;

import co.edu.uniquindio.cocinasinestres.curaduria.domain.TipoCantidad;
import co.edu.uniquindio.cocinasinestres.curaduria.domain.UnidadBase;
import co.edu.uniquindio.cocinasinestres.despensa.domain.CatalogoProductos;
import co.edu.uniquindio.cocinasinestres.despensa.domain.ProductoCatalogo;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Lee la tabla {@code producto} para la despensa, sin usar la entidad ni el repositorio
 * de curaduría (RNF-26).
 *
 * <p>Es una solución provisional: cuando curaduría o recetario publiquen un servicio de
 * consulta del catálogo para las usuarias, esta clase se reemplaza por una que lo llame.</p>
 */
@Repository
public class CatalogoProductosJdbc implements CatalogoProductos {

    private static final String COLUMNAS = """
        id, nombre, categoria, tipo_cantidad, unidad_base, capacidad_referencia,
        unidad_compra, cantidad_unidad_compra, precio_unidad_compra
        """;

    private final JdbcClient jdbc;

    public CatalogoProductosJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<ProductoCatalogo> buscarDisponible(Long productoId, Long hogarId) {
        return jdbc.sql("SELECT " + COLUMNAS + """
                FROM producto
                WHERE id = :productoId
                  AND activo = TRUE
                  AND (hogar_id IS NULL OR hogar_id = :hogarId)
                """)
            .param("productoId", productoId)
            .param("hogarId", hogarId)
            .query(CatalogoProductosJdbc::aProducto)
            .optional();
    }

    @Override
    public Map<Long, ProductoCatalogo> buscarPorIds(Collection<Long> productoIds) {
        Map<Long, ProductoCatalogo> resultado = new LinkedHashMap<>();

        if (productoIds == null || productoIds.isEmpty()) {
            return resultado;
        }

        jdbc.sql("SELECT " + COLUMNAS + " FROM producto WHERE id IN (:ids)")
            .param("ids", productoIds)
            .query(CatalogoProductosJdbc::aProducto)
            .list()
            .forEach(producto -> resultado.put(producto.id(), producto));

        return resultado;
    }

    private static ProductoCatalogo aProducto(ResultSet rs, int fila) throws SQLException {
        return new ProductoCatalogo(
            rs.getLong("id"),
            rs.getString("nombre"),
            rs.getString("categoria"),
            TipoCantidad.valueOf(rs.getString("tipo_cantidad")),
            UnidadBase.valueOf(rs.getString("unidad_base")),
            rs.getBigDecimal("capacidad_referencia"),
            rs.getString("unidad_compra"),
            rs.getBigDecimal("cantidad_unidad_compra"),
            rs.getBigDecimal("precio_unidad_compra")
        );
    }
}
