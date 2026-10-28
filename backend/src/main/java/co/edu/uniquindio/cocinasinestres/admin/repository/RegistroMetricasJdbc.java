package co.edu.uniquindio.cocinasinestres.admin.repository;

import java.time.LocalDate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Guarda las mediciones que manda la web para el análisis del administrador (V16). */
@Repository
public class RegistroMetricasJdbc {

    private final JdbcClient jdbc;

    public RegistroMetricasJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public void inicioSesion(Long usuarioId, int duracionMs, int fallidosPrevios) {
        jdbc.sql("""
                INSERT INTO inicio_sesion (usuario_id, duracion_ms, fallidos_previos)
                VALUES (:usuario, :ms, :fallidos)
                """)
            .param("usuario", usuarioId)
            .param("ms", duracionMs)
            .param("fallidos", fallidosPrevios)
            .update();
    }

    /**
     * Anota el tiempo de planificación del plan de esa semana, solo la primera vez y solo en
     * el hogar de la usuaria.
     *
     * @return 1 si lo anotó; 0 si no hay plan o ya tenía tiempo
     */
    public int planificacion(Long usuarioId, LocalDate semanaInicio, int segundos) {
        return jdbc.sql("""
                UPDATE plan_semanal p
                JOIN hogar h ON h.id = p.hogar_id
                SET p.segundos_planificacion = :segundos
                WHERE h.usuario_id = :usuario
                  AND p.semana_inicio = :semana
                  AND p.segundos_planificacion IS NULL
                """)
            .param("segundos", segundos)
            .param("usuario", usuarioId)
            .param("semana", semanaInicio)
            .update();
    }
}
