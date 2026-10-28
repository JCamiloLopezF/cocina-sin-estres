package co.edu.uniquindio.cocinasinestres.admin.repository;

import co.edu.uniquindio.cocinasinestres.admin.domain.DatosAnalisis;
import co.edu.uniquindio.cocinasinestres.admin.domain.DatosAnalisis.PlanConCosto;
import co.edu.uniquindio.cocinasinestres.admin.domain.DatosAnalisis.RecetaUsada;
import co.edu.uniquindio.cocinasinestres.admin.domain.DatosLogin;
import co.edu.uniquindio.cocinasinestres.admin.domain.FuenteAnalisis;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Lee las cifras del análisis del administrador directamente de las tablas (KAN-54). Es un
 * reporte: cruza módulos, pero solo cuenta y promedia, y no usa entidades de otros módulos
 * (RNF-26). Las fechas del periodo se calculan en la base ({@code NOW(3)}) para compararlas
 * con las fechas que la misma base puso por defecto.
 */
@Repository
public class FuenteAnalisisJdbc implements FuenteAnalisis {

    private static final int TOP_RECETAS = 5;

    /**
     * Costo de cada receta completa con la fórmula del costo del plan (ADR-14): solo
     * obligatorios. En los planes se multiplica por personas / porciones, como en la app.
     */
    private static final String COSTO_RECETA = """
        SELECT ir.receta_id,
               SUM(ir.cantidad / pr.cantidad_unidad_compra * pr.precio_unidad_compra) AS costo
        FROM ingrediente_receta ir
        JOIN producto pr ON pr.id = ir.producto_id
        WHERE ir.opcional = FALSE
          AND pr.cantidad_unidad_compra > 0
          AND pr.precio_unidad_compra IS NOT NULL
        GROUP BY ir.receta_id
        """;

    private final JdbcClient jdbc;

    public FuenteAnalisisJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public DatosAnalisis leer(int dias, LocalDate lunes, LocalDate desdeLunes) {
        long[] cuentas = jdbc.sql("""
                SELECT COUNT(*), COALESCE(SUM(perfil_completo), 0)
                FROM usuario
                WHERE rol = 'USUARIA' AND activo = TRUE
                  AND firebase_uid <> 'sistema-carga-inicial'
                """)
            .query((rs, n) -> new long[] {rs.getLong(1), rs.getLong(2)})
            .single();

        long hogares = contar("SELECT COUNT(*) FROM hogar", dias);
        long activas = contar(
            "SELECT COUNT(DISTINCT hogar_id) FROM apertura_app WHERE creada_en >= NOW(3) - INTERVAL 7 DAY", dias);

        Aperturas aperturas = jdbc.sql("""
                SELECT COUNT(*) AS n,
                       AVG(avisos_mostrados) AS avisos,
                       AVG(duracion_ms) AS ms,
                       COALESCE(SUM(reglas_evaluadas >= 5), 0) AS cinco
                FROM apertura_app
                WHERE creada_en >= NOW(3) - INTERVAL :dias DAY
                """)
            .param("dias", dias)
            .query((rs, n) -> new Aperturas(rs.getLong("n"), doble(rs, "avisos"), doble(rs, "ms"), rs.getLong("cinco")))
            .single();

        long planesSemana = jdbc.sql("""
                SELECT COUNT(DISTINCT p.id)
                FROM plan_semanal p
                JOIN comida_plan c ON c.plan_semanal_id = p.id
                WHERE p.semana_inicio = :lunes
                """)
            .param("lunes", lunes)
            .query(Long.class)
            .single();

        long[] preparaciones = jdbc.sql("""
                SELECT COUNT(*), COALESCE(SUM(comida_plan_id IS NULL), 0)
                FROM preparacion
                WHERE creada_en >= NOW(3) - INTERVAL :dias DAY
                """)
            .param("dias", dias)
            .query((rs, n) -> new long[] {rs.getLong(1), rs.getLong(2)})
            .single();

        long rescates = contar(
            "SELECT COUNT(*) FROM uso_rescate WHERE creado_en >= NOW(3) - INTERVAL :dias DAY", dias);

        Promedio actualidad = jdbc.sql("""
                SELECT COUNT(*) AS n, AVG(dias) AS promedio
                FROM (
                    SELECT DATEDIFF(NOW(3), MAX(actualizado_en)) AS dias
                    FROM item_despensa
                    GROUP BY hogar_id
                ) por_hogar
                """)
            .query((rs, n) -> new Promedio(rs.getLong("n"), doble(rs, "promedio")))
            .single();

        long[] catalogo = jdbc.sql("""
                SELECT COUNT(*), COALESCE(SUM(p.hogar_id IS NULL), 0)
                FROM item_despensa i
                JOIN producto p ON p.id = i.producto_id
                """)
            .query((rs, n) -> new long[] {rs.getLong(1), rs.getLong(2)})
            .single();

        // Lo mide la web: desde que la usuaria abre la semana sin completar hasta que queda
        // con almuerzo y cena los 7 días (V16). Contar desde que se creó el plan daba 0 min,
        // porque «Armar mi semana» lo crea y lo completa en un segundo.
        Promedio armado = jdbc.sql("""
                SELECT COUNT(*) AS n,
                       AVG(segundos_planificacion) / 60 AS promedio
                FROM plan_semanal
                WHERE segundos_planificacion IS NOT NULL
                  AND creado_en >= NOW(3) - INTERVAL :dias DAY
                """)
            .param("dias", dias)
            .query((rs, n) -> new Promedio(rs.getLong("n"), doble(rs, "promedio")))
            .single();

        List<PlanConCosto> planes = jdbc.sql("""
                SELECT p.id, h.presupuesto_monto, h.periodicidad_ingreso,
                       COALESCE(SUM(rc.costo * COALESCE(NULLIF(pe.personas, 0), r.porciones) / r.porciones), 0) AS costo
                FROM plan_semanal p
                JOIN hogar h ON h.id = p.hogar_id
                JOIN comida_plan c ON c.plan_semanal_id = p.id
                JOIN receta r ON r.id = c.receta_id
                LEFT JOIN (SELECT hogar_id, COUNT(*) AS personas FROM persona GROUP BY hogar_id) pe
                       ON pe.hogar_id = h.id
                LEFT JOIN (%s) rc ON rc.receta_id = c.receta_id
                WHERE p.semana_inicio BETWEEN :desde AND :lunes
                GROUP BY p.id, h.presupuesto_monto, h.periodicidad_ingreso
                """.formatted(COSTO_RECETA))
            .param("desde", desdeLunes)
            .param("lunes", lunes)
            .query((rs, n) -> new PlanConCosto(
                rs.getBigDecimal("costo"), rs.getBigDecimal("presupuesto_monto"), rs.getString("periodicidad_ingreso")))
            .list();

        long[] aprovechamiento = jdbc.sql("""
                SELECT COUNT(*) AS disponibles,
                       COALESCE(SUM(EXISTS (
                           SELECT 1
                           FROM comida_plan c
                           JOIN ingrediente_receta ir ON ir.receta_id = c.receta_id
                           WHERE c.plan_semanal_id = p.id AND ir.producto_id = i.producto_id
                       )), 0) AS usados
                FROM plan_semanal p
                JOIN item_despensa i ON i.hogar_id = p.hogar_id AND i.cantidad > 0
                WHERE p.semana_inicio = :lunes
                  AND EXISTS (SELECT 1 FROM comida_plan c2 WHERE c2.plan_semanal_id = p.id)
                """)
            .param("lunes", lunes)
            .query((rs, n) -> new long[] {rs.getLong("disponibles"), rs.getLong("usados")})
            .single();

        long[] semanasRescate = jdbc.sql("""
                SELECT COUNT(*) AS semanas,
                       COALESCE(SUM(EXISTS (
                           SELECT 1
                           FROM uso_rescate u
                           WHERE u.hogar_id = p.hogar_id
                             AND u.creado_en >= p.semana_inicio
                             AND u.creado_en < p.semana_inicio + INTERVAL 7 DAY
                       )), 0) AS con_rescate
                FROM plan_semanal p
                WHERE p.semana_inicio BETWEEN :desde AND :lunes
                  AND EXISTS (SELECT 1 FROM comida_plan c WHERE c.plan_semanal_id = p.id)
                """)
            .param("desde", desdeLunes)
            .param("lunes", lunes)
            .query((rs, n) -> new long[] {rs.getLong("semanas"), rs.getLong("con_rescate")})
            .single();

        long[] vencimiento = jdbc.sql("""
                SELECT COUNT(*), COALESCE(SUM(leido), 0)
                FROM aviso
                WHERE tipo = 'POR_VENCER'
                  AND creado_en >= NOW(3) - INTERVAL :dias DAY
                """)
            .param("dias", dias)
            .query((rs, n) -> new long[] {rs.getLong(1), rs.getLong(2)})
            .single();

        List<RecetaUsada> masPreparadas = jdbc.sql("""
                SELECT r.nombre, COUNT(*) AS veces
                FROM preparacion pr
                JOIN receta r ON r.id = pr.receta_id
                WHERE pr.creada_en >= NOW(3) - INTERVAL :dias DAY
                GROUP BY r.id, r.nombre
                ORDER BY veces DESC, r.nombre
                LIMIT :top
                """)
            .param("dias", dias)
            .param("top", TOP_RECETAS)
            .query(FuenteAnalisisJdbc::recetaUsada)
            .list();

        List<RecetaUsada> masPlaneadas = jdbc.sql("""
                SELECT r.nombre, COUNT(*) AS veces
                FROM comida_plan c
                JOIN plan_semanal p ON p.id = c.plan_semanal_id
                JOIN receta r ON r.id = c.receta_id
                WHERE p.semana_inicio BETWEEN :desde AND :lunes
                GROUP BY r.id, r.nombre
                ORDER BY veces DESC, r.nombre
                LIMIT :top
                """)
            .param("desde", desdeLunes)
            .param("lunes", lunes)
            .param("top", TOP_RECETAS)
            .query(FuenteAnalisisJdbc::recetaUsada)
            .list();

        List<String> sinUsar = jdbc.sql("""
                SELECT r.nombre
                FROM receta r
                WHERE r.activa = TRUE
                  AND NOT EXISTS (
                      SELECT 1
                      FROM comida_plan c
                      JOIN plan_semanal p ON p.id = c.plan_semanal_id
                      WHERE c.receta_id = r.id AND p.semana_inicio BETWEEN :desde AND :lunes
                  )
                ORDER BY r.nombre
                """)
            .param("desde", desdeLunes)
            .param("lunes", lunes)
            .query(String.class)
            .list();

        return new DatosAnalisis(
            cuentas[0], cuentas[1], hogares, activas,
            aperturas.n(), aperturas.avisos(), aperturas.ms(), aperturas.cinco(),
            planesSemana, preparaciones[0], preparaciones[1], rescates,
            actualidad.n(), actualidad.promedio(), catalogo[0], catalogo[1],
            armado.n(), armado.promedio(), planes,
            aprovechamiento[0], aprovechamiento[1],
            semanasRescate[0], semanasRescate[1],
            vencimiento[0], vencimiento[1],
            masPreparadas, masPlaneadas, sinUsar);
    }

    @Override
    public DatosLogin login(int dias) {
        return jdbc.sql("""
                SELECT COUNT(*) AS exitosos,
                       COALESCE(SUM(fallidos_previos), 0) AS fallidos,
                       AVG(duracion_ms) AS ms
                FROM inicio_sesion
                WHERE creado_en >= NOW(3) - INTERVAL :dias DAY
                """)
            .param("dias", dias)
            .query((rs, n) -> new DatosLogin(rs.getLong("exitosos"), rs.getLong("fallidos"), doble(rs, "ms")))
            .single();
    }

    private long contar(String sql, int dias) {
        JdbcClient.StatementSpec consulta = jdbc.sql(sql);
        if (sql.contains(":dias")) {
            consulta = consulta.param("dias", dias);
        }
        return consulta.query(Long.class).single();
    }

    private static Double doble(ResultSet rs, String columna) throws SQLException {
        BigDecimal valor = rs.getBigDecimal(columna);
        return valor == null ? null : valor.doubleValue();
    }

    private static RecetaUsada recetaUsada(ResultSet rs, int fila) throws SQLException {
        return new RecetaUsada(rs.getString("nombre"), rs.getLong("veces"));
    }

    private record Aperturas(long n, Double avisos, Double ms, long cinco) {
    }

    private record Promedio(long n, Double promedio) {
    }
}
