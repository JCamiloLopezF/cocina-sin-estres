package co.edu.uniquindio.cocinasinestres.planificador.domain;

/**
 * Lo que el planificador anota para el análisis del administrador (KAN-54, V15). Son
 * cifras de uso, no datos de la usuaria. Si anotar falla, la acción de la usuaria sigue:
 * una métrica nunca debe impedir que cocine o planee.
 */
public interface RegistroUso {

    /** Se cocinó una receta: del plan ({@code comidaPlanId}) o desde el recetario ({@code null}). */
    void preparacion(Long hogarId, Long recetaId, Long comidaPlanId);

    /** Se abrió el modo rescate y salieron {@code opciones} recetas. */
    void rescate(Long hogarId, int opciones);

    /**
     * Si el plan ya tiene almuerzo y cena los 7 días y aún no tenía fecha de completado,
     * se la pone (para el tiempo de planificación semanal).
     */
    void revisarPlanCompleto(Long planId);
}
