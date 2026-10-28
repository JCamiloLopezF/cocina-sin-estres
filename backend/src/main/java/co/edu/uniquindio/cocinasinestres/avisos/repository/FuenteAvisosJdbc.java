package co.edu.uniquindio.cocinasinestres.avisos.repository;

import co.edu.uniquindio.cocinasinestres.avisos.domain.EstadoSemana;
import co.edu.uniquindio.cocinasinestres.avisos.domain.FuenteAvisos;
import co.edu.uniquindio.cocinasinestres.avisos.domain.PreparacionPendiente;
import co.edu.uniquindio.cocinasinestres.avisos.domain.ProductoAviso;
import co.edu.uniquindio.cocinasinestres.avisos.domain.TipoAviso;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Lee la despensa, el plan y las preferencias directamente de sus tablas, sin usar las
 * entidades de otros módulos (RNF-26). Solo consultas.
 */
@Repository
public class FuenteAvisosJdbc implements FuenteAvisos {

    private final JdbcClient jdbc;

    public FuenteAvisosJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Long> hogarDe(Long usuarioId) {
        return jdbc.sql("SELECT id FROM hogar WHERE usuario_id = :usuario")
            .param("usuario", usuarioId)
            .query(Long.class)
            .optional();
    }

    @Override
    public Set<TipoAviso> apagados(Long hogarId) {
        Set<TipoAviso> apagados = EnumSet.noneOf(TipoAviso.class);
        jdbc.sql("SELECT tipo FROM hogar_preferencia_aviso WHERE hogar_id = :hogar AND activo = FALSE")
            .param("hogar", hogarId)
            .query(String.class)
            .list()
            .stream()
            .map(TipoAviso::desde)
            .filter(Objects::nonNull)
            .forEach(apagados::add);
        return apagados;
    }

    @Override
    public List<ProductoAviso> bajoMinimo(Long hogarId) {
        // Misma regla que ReglasDespensa.estaBajoMinimo: se avisa al llegar al mínimo.
        return jdbc.sql("""
                SELECT i.producto_id, p.nombre
                FROM item_despensa i
                JOIN producto p ON p.id = i.producto_id
                WHERE i.hogar_id = :hogar
                  AND i.cantidad_minima IS NOT NULL
                  AND i.cantidad <= i.cantidad_minima
                ORDER BY p.nombre
                """)
            .param("hogar", hogarId)
            .query((rs, n) -> new ProductoAviso(rs.getLong("producto_id"), rs.getString("nombre"), null))
            .list();
    }

    @Override
    public List<ProductoAviso> porVencer(Long hogarId, LocalDate desde, LocalDate hasta) {
        return jdbc.sql("""
                SELECT i.producto_id, p.nombre, i.vence_en
                FROM item_despensa i
                JOIN producto p ON p.id = i.producto_id
                WHERE i.hogar_id = :hogar
                  AND i.cantidad > 0
                  AND i.vence_en BETWEEN :desde AND :hasta
                ORDER BY i.vence_en, p.nombre
                """)
            .param("hogar", hogarId)
            .param("desde", desde)
            .param("hasta", hasta)
            .query((rs, n) -> new ProductoAviso(
                rs.getLong("producto_id"), rs.getString("nombre"), rs.getObject("vence_en", LocalDate.class)))
            .list();
    }

    @Override
    public List<PreparacionPendiente> preparacionesPara(Long hogarId, LocalDate fecha) {
        return jdbc.sql("""
                SELECT c.dia, c.tipo_comida, r.nombre, r.preparacion_previa
                FROM comida_plan c
                JOIN plan_semanal p ON p.id = c.plan_semanal_id
                JOIN receta r ON r.id = c.receta_id
                WHERE p.hogar_id = :hogar
                  AND p.semana_inicio = :lunes
                  AND c.dia = :dia
                  AND c.preparada = FALSE
                  AND r.preparacion_previa IS NOT NULL
                  AND TRIM(r.preparacion_previa) <> ''
                ORDER BY FIELD(c.tipo_comida, 'DESAYUNO', 'ALMUERZO', 'CENA')
                """)
            .param("hogar", hogarId)
            .param("lunes", fecha.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)))
            .param("dia", fecha.getDayOfWeek().getValue())
            .query((rs, n) -> new PreparacionPendiente(
                rs.getInt("dia"), rs.getString("tipo_comida"), rs.getString("nombre"), rs.getString("preparacion_previa")))
            .list();
    }

    @Override
    public EstadoSemana semana(Long hogarId, LocalDate lunes) {
        return jdbc.sql("""
                SELECT p.borrador,
                       (SELECT COUNT(*) FROM comida_plan c WHERE c.plan_semanal_id = p.id) AS comidas
                FROM plan_semanal p
                WHERE p.hogar_id = :hogar
                  AND p.semana_inicio = :lunes
                """)
            .param("hogar", hogarId)
            .param("lunes", lunes)
            .query((rs, n) -> {
                int comidas = rs.getInt("comidas");
                EstadoSemana.Situacion situacion = comidas == 0 ? EstadoSemana.Situacion.VACIA
                    : rs.getBoolean("borrador") ? EstadoSemana.Situacion.BORRADOR
                    : EstadoSemana.Situacion.PLANEADA;
                return new EstadoSemana(situacion, comidas);
            })
            .optional()
            .orElse(EstadoSemana.SIN_PLAN);
    }
}
