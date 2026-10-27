package co.edu.uniquindio.cocinasinestres.admin.dto;

import co.edu.uniquindio.cocinasinestres.admin.domain.DatosAnalisis.RecetaUsada;
import co.edu.uniquindio.cocinasinestres.admin.domain.Indicador;
import java.time.LocalDate;
import java.util.List;

/**
 * {@code GET /api/admin/analisis} (KAN-54): cómo se está usando la app y los indicadores del
 * Plan de Gestión de la Calidad.
 *
 * @param desde         primer día del periodo de los eventos (aperturas, preparaciones, avisos)
 * @param hasta         hoy
 * @param semanasDesde  lunes de la primera semana que cuenta para los planes
 * @param uso           cifras de uso
 * @param indicadores   indicadores del plan de calidad, por proceso
 * @param masPreparadas recetas más cocinadas en el periodo
 * @param masPlaneadas  recetas más puestas en el plan en las últimas semanas
 * @param sinUsar       recetas activas que no aparecen en ningún plan de esas semanas
 */
public record AnalisisResponse(
    LocalDate desde,
    LocalDate hasta,
    LocalDate semanasDesde,
    Uso uso,
    List<Indicador> indicadores,
    List<RecetaUsada> masPreparadas,
    List<RecetaUsada> masPlaneadas,
    List<String> sinUsar) {

    /**
     * @param usuarias           cuentas activas con rol USUARIA
     * @param hogares            hogares registrados
     * @param activas7Dias       hogares que abrieron la app en los últimos 7 días
     * @param aperturas          aperturas de la app en el periodo
     * @param planesEstaSemana   hogares con plan esta semana
     * @param preparaciones      recetas cocinadas en el periodo
     * @param preparacionesFuera de esas, fuera del plan
     * @param usosRescate        veces que se abrió el modo rescate
     */
    public record Uso(
        long usuarias,
        long hogares,
        long activas7Dias,
        long aperturas,
        long planesEstaSemana,
        long preparaciones,
        long preparacionesFuera,
        long usosRescate) {
    }
}
