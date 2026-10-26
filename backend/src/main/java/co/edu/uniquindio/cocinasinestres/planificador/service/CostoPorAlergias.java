package co.edu.uniquindio.cocinasinestres.planificador.service;

import co.edu.uniquindio.cocinasinestres.perfil.model.Hogar;
import co.edu.uniquindio.cocinasinestres.planificador.model.ComidaPlan;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Cuánto cuesta lo que se agrega al plan por quien tiene alergia (KAN-24, ADR-24): el
 * reemplazo de su porción y su plato aparte, con la misma fórmula del costo del plan
 * ({@code cantidad / cantidadUnidadCompra * precioUnidadCompra}).
 */
@Component
public class CostoPorAlergias {

    private final ComprasPorAlergias compras;
    private final JdbcClient jdbc;

    public CostoPorAlergias(ComprasPorAlergias compras, JdbcClient jdbc) {
        this.compras = compras;
        this.jdbc = jdbc;
    }

    /** Costo extra por día de la semana (1 = lunes … 7 = domingo); vacío si nadie tiene alergias. */
    public Map<Integer, BigDecimal> porDia(Hogar hogar, List<ComidaPlan> comidas) {
        Map<Integer, Map<Long, BigDecimal>> extras = compras.extrasPorDia(hogar, comidas, false);
        if (extras.isEmpty()) {
            return Map.of();
        }
        Set<Long> productos = new HashSet<>();
        extras.values().forEach(delDia -> productos.addAll(delDia.keySet()));
        Map<Long, BigDecimal> precioPorUnidadBase = new HashMap<>();
        jdbc.sql("""
                SELECT id, cantidad_unidad_compra, precio_unidad_compra
                FROM producto
                WHERE id IN (:ids)
                """)
            .param("ids", productos)
            .query(rs -> {
                BigDecimal presentacion = rs.getBigDecimal("cantidad_unidad_compra");
                BigDecimal precio = rs.getBigDecimal("precio_unidad_compra");
                if (presentacion != null && precio != null && presentacion.signum() > 0) {
                    precioPorUnidadBase.put(rs.getLong("id"), precio.divide(presentacion, 6, RoundingMode.HALF_UP));
                }
            });

        Map<Integer, BigDecimal> porDia = new HashMap<>();
        extras.forEach((dia, delDia) -> {
            BigDecimal costo = BigDecimal.ZERO;
            for (Map.Entry<Long, BigDecimal> extra : delDia.entrySet()) {
                BigDecimal precio = precioPorUnidadBase.get(extra.getKey());
                if (precio != null) {
                    costo = costo.add(extra.getValue().multiply(precio));
                }
            }
            if (costo.signum() > 0) {
                porDia.put(dia, costo.setScale(2, RoundingMode.HALF_UP));
            }
        });
        return porDia;
    }

    /** Costo extra de toda la semana. */
    public BigDecimal total(Hogar hogar, List<ComidaPlan> comidas) {
        return porDia(hogar, comidas).values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
