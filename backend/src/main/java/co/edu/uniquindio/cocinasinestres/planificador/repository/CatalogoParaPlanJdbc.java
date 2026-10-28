package co.edu.uniquindio.cocinasinestres.planificador.repository;

import co.edu.uniquindio.cocinasinestres.planificador.domain.CatalogoParaPlan;
import co.edu.uniquindio.cocinasinestres.planificador.domain.RecetaCandidata;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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
        SELECT id, tiempo_minutos, activa, es_plato_fuerte, gusta_a_ninos,
               para_desayuno, para_almuerzo, para_cena
        FROM receta
        """;

    /** Solo ingredientes obligatorios: los opcionales no se compran (igual que el costo del plan). */
    private static final String SQL_INGREDIENTES = """
        SELECT ir.receta_id, ir.producto_id, ir.cantidad, ir.es_principal,
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
                rs.getBoolean("gusta_a_ninos"),
                momentos(rs.getBoolean("para_desayuno"), rs.getBoolean("para_almuerzo"), rs.getBoolean("para_cena"))))
            .list()
            .forEach(f -> filas.put(f.id, f));

        jdbc.sql(SQL_INGREDIENTES)
            .query(rs -> {
                Fila fila = filas.get(rs.getLong("receta_id"));
                if (fila != null) {
                    BigDecimal cantidad = rs.getBigDecimal("cantidad");
                    fila.ingredientes.merge(rs.getLong("producto_id"), cantidad, BigDecimal::add);
                    if (rs.getBoolean("es_principal")) {
                        fila.principales.add(rs.getLong("producto_id"));
                    }
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
            f.id, f.tiempoMinutos, f.costo, f.ingredientes, f.alergias, f.condiciones, f.activa, f.platoFuerte, f.gustaANinos,
            f.principales, f.momentos)));
        return recetas;
    }

    @Override
    public Map<Long, Set<String>> alergenos() {
        Map<Long, Set<String>> alergenos = new HashMap<>();
        jdbc.sql("""
                SELECT pr.producto_id, r.codigo
                FROM producto_restriccion pr
                JOIN restriccion r ON r.id = pr.restriccion_id
                WHERE r.tipo = 'ALERGIA'
                """)
            .query(rs -> {
                alergenos.computeIfAbsent(rs.getLong("producto_id"), k -> new HashSet<>()).add(rs.getString("codigo"));
            });
        return alergenos;
    }

    @Override
    public Map<Long, Set<Long>> reemplazos() {
        Map<Long, Set<Long>> reemplazos = new HashMap<>();
        jdbc.sql("""
                SELECT rp.producto_id, rp.reemplazo_id
                FROM reemplazo_producto rp
                JOIN producto p ON p.id = rp.reemplazo_id
                WHERE rp.activo = TRUE AND p.activo = TRUE
                """)
            .query(rs -> {
                reemplazos.computeIfAbsent(rs.getLong("producto_id"), k -> new HashSet<>()).add(rs.getLong("reemplazo_id"));
            });
        return reemplazos;
    }

    @Override
    public Map<Long, String> nombresProducto() {
        Map<Long, String> nombres = new HashMap<>();
        jdbc.sql("SELECT id, nombre FROM producto WHERE hogar_id IS NULL")
            .query(rs -> {
                nombres.put(rs.getLong("id"), rs.getString("nombre"));
            });
        return nombres;
    }

    @Override
    public Map<Long, Integer> porciones() {
        Map<Long, Integer> porciones = new HashMap<>();
        jdbc.sql("SELECT id, porciones FROM receta")
            .query(rs -> {
                porciones.put(rs.getLong("id"), rs.getInt("porciones"));
            });
        return porciones;
    }

    @Override
    public Map<Long, ProductoPlan> productos() {
        Map<Long, ProductoPlan> productos = new HashMap<>();
        jdbc.sql("SELECT id, unidad_base, cantidad_unidad_compra FROM producto WHERE hogar_id IS NULL")
            .query(rs -> {
                productos.put(rs.getLong("id"),
                    new ProductoPlan(rs.getString("unidad_base"), rs.getBigDecimal("cantidad_unidad_compra")));
            });
        return productos;
    }

    @Override
    public Map<Long, String> nombresReceta() {
        Map<Long, String> nombres = new HashMap<>();
        jdbc.sql("SELECT id, nombre FROM receta")
            .query(rs -> {
                nombres.put(rs.getLong("id"), rs.getString("nombre"));
            });
        return nombres;
    }

    /** Comidas en que va la receta (V17), en el orden del día. */
    static Set<String> momentos(boolean desayuno, boolean almuerzo, boolean cena) {
        Set<String> momentos = new LinkedHashSet<>();
        if (desayuno) {
            momentos.add("DESAYUNO");
        }
        if (almuerzo) {
            momentos.add("ALMUERZO");
        }
        if (cena) {
            momentos.add("CENA");
        }
        return momentos;
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
        private final Set<String> momentos;
        private final Map<Long, BigDecimal> ingredientes = new HashMap<>();
        private final Set<String> alergias = new HashSet<>();
        private final Set<String> condiciones = new HashSet<>();
        private final Set<Long> principales = new HashSet<>();
        private BigDecimal costo = BigDecimal.ZERO;

        private Fila(
            long id, int tiempoMinutos, boolean activa, boolean platoFuerte, boolean gustaANinos, Set<String> momentos) {
            this.id = id;
            this.tiempoMinutos = tiempoMinutos;
            this.activa = activa;
            this.platoFuerte = platoFuerte;
            this.gustaANinos = gustaANinos;
            this.momentos = momentos;
        }
    }
}
