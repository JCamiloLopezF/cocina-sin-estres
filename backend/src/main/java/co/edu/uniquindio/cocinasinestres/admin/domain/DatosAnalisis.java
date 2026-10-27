package co.edu.uniquindio.cocinasinestres.admin.domain;

import java.math.BigDecimal;
import java.util.List;

/**
 * Las cifras crudas del análisis del administrador (KAN-54), como salen de la base. Los
 * porcentajes y si se cumple la meta los calcula {@link IndicadoresCalidad}.
 *
 * @param usuarias               cuentas activas con rol USUARIA
 * @param perfilesCompletos      de esas, las que completaron el perfil del hogar
 * @param hogares                hogares registrados
 * @param activas7Dias           hogares que abrieron la app en los últimos 7 días
 * @param aperturas              aperturas de la app (motor de avisos) en el periodo
 * @param avisosPorApertura      promedio de avisos mostrados por apertura
 * @param msPorApertura          promedio de milisegundos del motor de avisos
 * @param aperturasCincoReglas   aperturas en que se evaluaron las cinco reglas
 * @param planesEstaSemana       planes de la semana actual con al menos una comida
 * @param preparaciones          recetas cocinadas en el periodo
 * @param preparacionesFuera     de esas, las cocinadas fuera del plan
 * @param usosRescate            veces que se abrió el modo rescate en el periodo
 * @param hogaresConDespensa     hogares con algo en la despensa
 * @param diasDesdeActualizacion promedio de días desde la última actualización de la despensa
 * @param itemsDespensa          productos en las despensas
 * @param itemsDelCatalogo       de esos, los que vienen del catálogo predefinido
 * @param planesCompletados      planes completados en el periodo (en menos de un día)
 * @param minutosParaArmar       promedio de minutos entre crear el plan y completarlo
 * @param planes                 costo y presupuesto de los planes de las últimas semanas
 * @param productosDisponibles   productos con existencias en los hogares con plan esta semana
 * @param productosUsados        de esos, los que pide alguna receta del plan
 * @param semanasPlaneadas       semanas planeadas en el periodo
 * @param semanasConRescate      de esas, en las que se usó el modo rescate
 * @param avisosVencimiento      avisos de producto por vencer generados en el periodo
 * @param avisosVencimientoLeidos de esos, los que la usuaria marcó como leídos
 * @param masPreparadas          recetas más cocinadas
 * @param masPlaneadas           recetas más puestas en el plan
 * @param sinUsar                recetas activas que no aparecen en ningún plan del periodo
 */
public record DatosAnalisis(
    long usuarias,
    long perfilesCompletos,
    long hogares,
    long activas7Dias,
    long aperturas,
    Double avisosPorApertura,
    Double msPorApertura,
    long aperturasCincoReglas,
    long planesEstaSemana,
    long preparaciones,
    long preparacionesFuera,
    long usosRescate,
    long hogaresConDespensa,
    Double diasDesdeActualizacion,
    long itemsDespensa,
    long itemsDelCatalogo,
    long planesCompletados,
    Double minutosParaArmar,
    List<PlanConCosto> planes,
    long productosDisponibles,
    long productosUsados,
    long semanasPlaneadas,
    long semanasConRescate,
    long avisosVencimiento,
    long avisosVencimientoLeidos,
    List<RecetaUsada> masPreparadas,
    List<RecetaUsada> masPlaneadas,
    List<String> sinUsar) {

    public DatosAnalisis {
        planes = List.copyOf(planes);
        masPreparadas = List.copyOf(masPreparadas);
        masPlaneadas = List.copyOf(masPlaneadas);
        sinUsar = List.copyOf(sinUsar);
    }

    /** El costo de un plan y el presupuesto del hogar como lo guardó (ADR-14). */
    public record PlanConCosto(BigDecimal costo, BigDecimal presupuestoMonto, String periodicidad) {
    }

    /** Una receta y cuántas veces se usó. */
    public record RecetaUsada(String nombre, long veces) {
    }
}
