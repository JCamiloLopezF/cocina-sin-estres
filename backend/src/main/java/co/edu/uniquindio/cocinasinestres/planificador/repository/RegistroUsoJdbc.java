package co.edu.uniquindio.cocinasinestres.planificador.repository;

import co.edu.uniquindio.cocinasinestres.planificador.domain.RegistroUso;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Escribe el registro de uso (V15) con SQL directo. Corre dentro de la transacción de quien
 * lo llama, para ver las comidas que se acaban de guardar; si falla, solo deja la falla en
 * el log.
 */
@Repository
public class RegistroUsoJdbc implements RegistroUso {

    private static final Logger log = LoggerFactory.getLogger(RegistroUsoJdbc.class);

    /** Almuerzo y cena de los 7 días. */
    static final int COMIDAS_PLAN_COMPLETO = 14;

    private final JdbcClient jdbc;

    public RegistroUsoJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void preparacion(Long hogarId, Long recetaId, Long comidaPlanId) {
        try {
            jdbc.sql("INSERT INTO preparacion (hogar_id, receta_id, comida_plan_id) VALUES (:hogar, :receta, :comida)")
                .param("hogar", hogarId)
                .param("receta", recetaId)
                .param("comida", comidaPlanId)
                .update();
        } catch (DataAccessException e) {
            log.warn("No se pudo registrar la preparación: hogarId={} recetaId={}", hogarId, recetaId, e);
        }
    }

    @Override
    public void rescate(Long hogarId, int opciones) {
        try {
            jdbc.sql("INSERT INTO uso_rescate (hogar_id, opciones) VALUES (:hogar, :opciones)")
                .param("hogar", hogarId)
                .param("opciones", Math.max(0, opciones))
                .update();
        } catch (DataAccessException e) {
            log.warn("No se pudo registrar el uso del modo rescate: hogarId={}", hogarId, e);
        }
    }

    @Override
    public void revisarPlanCompleto(Long planId) {
        try {
            jdbc.sql("""
                    UPDATE plan_semanal
                    SET completado_en = CURRENT_TIMESTAMP(3)
                    WHERE id = :plan
                      AND completado_en IS NULL
                      AND (SELECT COUNT(*)
                           FROM comida_plan
                           WHERE plan_semanal_id = :plan
                             AND tipo_comida IN ('ALMUERZO', 'CENA')) >= :completo
                    """)
                .param("plan", planId)
                .param("completo", COMIDAS_PLAN_COMPLETO)
                .update();
        } catch (DataAccessException e) {
            log.warn("No se pudo revisar si el plan quedó completo: planId={}", planId, e);
        }
    }
}
