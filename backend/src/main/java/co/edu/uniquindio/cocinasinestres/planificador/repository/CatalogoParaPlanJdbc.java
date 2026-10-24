package co.edu.uniquindio.cocinasinestres.planificador.repository;

import co.edu.uniquindio.cocinasinestres.planificador.domain.CatalogoParaPlan;
import co.edu.uniquindio.cocinasinestres.planificador.domain.RecetaCandidata;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Lee el catálogo de recetas con tres consultas (recetas, ingredientes y
 * restricciones), sin las entidades del recetario (RNF-26).
 *
 * <p>El catálogo es pequeño (decenas de recetas) y lo arma el equipo, así que se lee
 * completo cada vez que se genera un plan; si crece, aquí se pondría una caché.</p>
 */
@Repository
public class CatalogoParaPlanJdbc implements CatalogoParaPlan {

    private static final String SQL_RECETAS = """
        SELECT id, tiempo_minutos, activa, es_plato_fuerte, gusta_a_ninos
        FROM receta
        """;

    /** Solo ingredientes obligatorios: los opcionales no se compran (igual que el costo del plan). */
    private static final String SQL_INGREDIENTES = """
        SELECT ir.receta_id, ir.producto_id, ir.cantidad,
               p.cantidad_unidad_compra, p.precio_unidad_compra
        FROM ingrediente_receta ir
        JOIN producto p ON p.id = ir.producto_id
        WHERE ir.opcional = FALSE
        """;

    private static final String SQL_RESTRICCIONES = """
        SELECT DISTINCT ir.receta_id, r.codigo, r.tipo
        FROM ingrediente_receta ir
        JOIN producto_restriccion pr ON pr.producto_id = ir.producto_id
        JOIN restriccion r ON r.id = pr.restriccion_id
        WHERE ir.opcional = FALSE
        """;

    private final JdbcClient jdbc;

    public CatalogoParaPlanJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Map<Long, RecetaCandidata> recetas() {
        Map<Long, Fila> filas = new LinkedHashMap<>();
        jdbc.sql(SQL_RECETAS)
            .query((rs, n) -> new Fila(
                rs.getLong("id"),
                rs.getInt("tiempo_minutos"),
                rs.getBoolean("activa"),
                rs.getBoolean("es_plato_fuerte"),
                rs.getBoolean("gusta_a_ninos")))
            .list()
            .forEach(f -> filas.put(f.id, f));

        jdbc.sql(SQL_INGREDIENTES)
            .query(rs -> {
                Fila fila = filas.get(rs.getLong("receta_id"));
                if (fila != null) {
                    BigDecimal cantidad = rs.getBigDecimal("cantidad");
                    fila.ingredientes.merge(rs.getLong("producto_id"), cantidad, BigDecimal::add);
                    fila.costo = fila.costo.add(costo(
                        cantidad,
                        rs.getBigDecimal("cantidad_unidad_compra"),
                        rs.getBigDecimal("precio_unidad_compra")));
                }
            });

        jdbc.sql(SQL_RESTRICCIONES)
            .query(rs -> {
                Fila fila = filas.get(rs.getLong("receta_id"));
                if (fila != null) {
                    String codigo = rs.getString("codigo");
                    if ("ALERGIA".equals(rs.getString("tipo"))) {
                        fila.alergias.add(codigo);
                    } else {
                        fila.condiciones.add(codigo);
                    }
                }
            });

        Map<Long, RecetaCandidata> recetas = new LinkedHashMap<>();
        filas.values().forEach(f -> recetas.put(f.id, new RecetaCandidata(
            f.id, f.tiempoMinutos, f.costo, f.ingredientes, f.alergias, f.condiciones, f.activa, f.platoFuerte, f.gustaANinos)));
        return recetas;
    }

    /** {@code cantidad / cantidadUnidadCompra * precioUnidadCompra}; sin precio de referencia no suma. */
    private static BigDecimal costo(BigDecimal cantidad, BigDecimal cantidadCompra, BigDecimal precio) {
        if (cantidad == null || cantidadCompra == null || precio == null || cantidadCompra.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        return cantidad.divide(cantidadCompra, MathContext.DECIMAL64).multiply(precio);
    }

    private static final class Fila {
        private final long id;
        private final int tiempoMinutos;
        private final boolean activa;
        private final boolean platoFuerte;
        private final boolean gustaANinos;
        private final Map<Long, BigDecimal> ingredientes = new HashMap<>();
        private final Set<String> alergias = new HashSet<>();
        private final Set<String> condiciones = new HashSet<>();
        private BigDecimal costo = BigDecimal.ZERO;

        private Fila(long id, int tiempoMinutos, boolean activa, boolean platoFuerte, boolean gustaANinos) {
            this.id = id;
            this.tiempoMinutos = tiempoMinutos;
            this.activa = activa;
            this.platoFuerte = platoFuerte;
            this.gustaANinos = gustaANinos;
        }
    }
}
