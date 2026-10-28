package co.edu.uniquindio.cocinasinestres.planificador.repository;

import co.edu.uniquindio.cocinasinestres.planificador.domain.MovimientosDespensa;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Mueve la despensa con SQL directo sobre {@code item_despensa}, sin la entidad de
 * despensa (RNF-26).
 */
@Repository
public class MovimientosDespensaJdbc implements MovimientosDespensa {

    /**
     * MySQL 8.0.19+: {@code AS nuevo} nombra la fila que se intenta insertar. El
     * vencimiento se calcula antes que la cantidad porque las asignaciones se evalúan en
     * orden y necesita la cantidad que había.
     */
    private static final String SQL_AGREGAR = """
        INSERT INTO item_despensa (hogar_id, producto_id, cantidad, es_estimada, vence_en)
        VALUES (:hogar, :producto, :cantidad, FALSE, :vence) AS nuevo
        ON DUPLICATE KEY UPDATE
            vence_en = CASE
                WHEN item_despensa.cantidad > 0
                     AND item_despensa.vence_en IS NOT NULL
                     AND item_despensa.vence_en >= :hoy
                     AND (nuevo.vence_en IS NULL OR item_despensa.vence_en < nuevo.vence_en)
                    THEN item_despensa.vence_en
                WHEN nuevo.vence_en IS NOT NULL THEN nuevo.vence_en
                WHEN item_despensa.vence_en >= :hoy THEN item_despensa.vence_en
                ELSE NULL
            END,
            cantidad = item_despensa.cantidad + nuevo.cantidad
        """;

    /** Lo que gasta la receta para las personas del hogar (personas / porciones). */
    private static final String SQL_DESCONTAR_RECETA = """
        UPDATE item_despensa i
        JOIN ingrediente_receta ir ON ir.producto_id = i.producto_id
        JOIN receta r ON r.id = ir.receta_id
        SET i.cantidad = GREATEST(i.cantidad - ir.cantidad * COALESCE(
                NULLIF((SELECT COUNT(*) FROM persona pe WHERE pe.hogar_id = :hogar), 0), r.porciones) / r.porciones, 0)
        WHERE i.hogar_id = :hogar
          AND ir.receta_id = :receta
          AND ir.opcional = FALSE
        """;

    private final JdbcClient jdbc;

    public MovimientosDespensaJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public DatosCompra datosCompra(Long productoId) {
        return jdbc.sql("SELECT cantidad_unidad_compra, vida_util_dias FROM producto WHERE id = :id")
            .param("id", productoId)
            .query((rs, n) -> new DatosCompra(
                rs.getBigDecimal("cantidad_unidad_compra"),
                (Integer) rs.getObject("vida_util_dias", Integer.class)))
            .optional()
            .orElse(new DatosCompra(null, null));
    }

    @Override
    public LocalDate agregar(Long hogarId, Long productoId, BigDecimal cantidad, LocalDate venceEn, LocalDate hoy) {
        jdbc.sql(SQL_AGREGAR)
            .param("hogar", hogarId)
            .param("producto", productoId)
            .param("cantidad", cantidad)
            .param("vence", venceEn)
            .param("hoy", hoy)
            .update();
        return jdbc.sql("SELECT vence_en FROM item_despensa WHERE hogar_id = :hogar AND producto_id = :producto")
            .param("hogar", hogarId)
            .param("producto", productoId)
            .query((rs, n) -> rs.getObject("vence_en", LocalDate.class))
            .optional()
            .orElse(null);
    }

    @Override
    public void quitar(Long hogarId, Long productoId, BigDecimal cantidad) {
        jdbc.sql("""
                UPDATE item_despensa
                SET cantidad = GREATEST(cantidad - :cantidad, 0)
                WHERE hogar_id = :hogar AND producto_id = :producto
                """)
            .param("cantidad", cantidad)
            .param("hogar", hogarId)
            .param("producto", productoId)
            .update();
    }

    @Override
    public boolean recetaActiva(Long recetaId) {
        return jdbc.sql("SELECT COUNT(*) FROM receta WHERE id = :id AND activa = TRUE")
            .param("id", recetaId)
            .query(Long.class)
            .single() > 0;
    }

    @Override
    public int descontarReceta(Long hogarId, Long recetaId) {
        return jdbc.sql(SQL_DESCONTAR_RECETA)
            .param("hogar", hogarId)
            .param("receta", recetaId)
            .update();
    }
}
