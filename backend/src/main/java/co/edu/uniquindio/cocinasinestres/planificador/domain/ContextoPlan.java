package co.edu.uniquindio.cocinasinestres.planificador.domain;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

/**
 * Todo lo que el generador sabe del hogar en el momento de armar la semana.
 *
 * @param recetas            catálogo de recetas (activas o no: las inactivas solo cuentan
 *                           para el costo de lo que ya está en el plan)
 * @param restricciones      alergias y condiciones de las personas del hogar
 * @param diasSinTiempo      días en que solo caben recetas rápidas
 * @param presupuestoSemanal presupuesto prorrateado a la semana (ADR-14)
 * @param stock              lo que hay en la despensa sin vencer, por producto
 * @param porVencer          productos que vencen en los próximos días y conviene gastar
 * @param fijas              lo que ya tiene el plan: el generador no lo cambia salvo que
 *                           se le pida (ajustar al presupuesto, otra opción)
 * @param hayNinos           en el hogar vive al menos un niño (12 años o menos)
 * @param alergias           alergias por persona, alérgenos y reemplazos para adaptar recetas
 * @param diasDesdeElPago    por cada día de la semana (1 = lunes … 7 = domingo), cuántos días
 *                           van desde el último pago ({@link CicloIngreso}); vacío si no se sabe
 */
public record ContextoPlan(
    Map<Long, RecetaCandidata> recetas,
    Set<String> restricciones,
    Set<Integer> diasSinTiempo,
    BigDecimal presupuestoSemanal,
    Map<Long, BigDecimal> stock,
    Set<Long> porVencer,
    Map<Hueco, Long> fijas,
    boolean hayNinos,
    ReglasAlergia alergias,
    Map<Integer, Integer> diasDesdeElPago) {

    public ContextoPlan {
        recetas = Map.copyOf(recetas);
        restricciones = Set.copyOf(restricciones);
        diasSinTiempo = Set.copyOf(diasSinTiempo);
        presupuestoSemanal = presupuestoSemanal == null ? BigDecimal.ZERO : presupuestoSemanal;
        stock = Map.copyOf(stock);
        porVencer = Set.copyOf(porVencer);
        fijas = Map.copyOf(fijas);
        alergias = alergias == null ? ReglasAlergia.SIN_DETALLE : alergias;
        diasDesdeElPago = diasDesdeElPago == null ? Map.of() : Map.copyOf(diasDesdeElPago);
    }

    /** Un hogar del que no se sabe cuándo le pagan. */
    public ContextoPlan(
        Map<Long, RecetaCandidata> recetas,
        Set<String> restricciones,
        Set<Integer> diasSinTiempo,
        BigDecimal presupuestoSemanal,
        Map<Long, BigDecimal> stock,
        Set<Long> porVencer,
        Map<Hueco, Long> fijas,
        boolean hayNinos,
        ReglasAlergia alergias) {
        this(recetas, restricciones, diasSinTiempo, presupuestoSemanal, stock, porVencer, fijas, hayNinos, alergias,
            Map.of());
    }

    /** Un hogar sin detalle de alergias por persona. */
    public ContextoPlan(
        Map<Long, RecetaCandidata> recetas,
        Set<String> restricciones,
        Set<Integer> diasSinTiempo,
        BigDecimal presupuestoSemanal,
        Map<Long, BigDecimal> stock,
        Set<Long> porVencer,
        Map<Hueco, Long> fijas,
        boolean hayNinos) {
        this(recetas, restricciones, diasSinTiempo, presupuestoSemanal, stock, porVencer, fijas, hayNinos,
            ReglasAlergia.SIN_DETALLE);
    }

    /** Un hogar sin niños. */
    public ContextoPlan(
        Map<Long, RecetaCandidata> recetas,
        Set<String> restricciones,
        Set<Integer> diasSinTiempo,
        BigDecimal presupuestoSemanal,
        Map<Long, BigDecimal> stock,
        Set<Long> porVencer,
        Map<Hueco, Long> fijas) {
        this(recetas, restricciones, diasSinTiempo, presupuestoSemanal, stock, porVencer, fijas, false,
            ReglasAlergia.SIN_DETALLE);
    }

    /** El mismo hogar con otro plan de partida. */
    public ContextoPlan conFijas(Map<Hueco, Long> otras) {
        return new ContextoPlan(recetas, restricciones, diasSinTiempo, presupuestoSemanal, stock, porVencer, otras, hayNinos, alergias, diasDesdeElPago);
    }

    /** El mismo hogar con el detalle de alergias por persona. */
    public ContextoPlan conAlergias(ReglasAlergia reglas) {
        return new ContextoPlan(recetas, restricciones, diasSinTiempo, presupuestoSemanal, stock, porVencer, fijas, hayNinos, reglas, diasDesdeElPago);
    }

    /** El mismo hogar, sabiendo cuántos días van desde el pago en cada día de la semana. */
    public ContextoPlan conIngreso(Map<Integer, Integer> dias) {
        return new ContextoPlan(recetas, restricciones, diasSinTiempo, presupuestoSemanal, stock, porVencer, fijas, hayNinos, alergias, dias);
    }

    /** El mismo hogar, con o sin niños. */
    public ContextoPlan conNinos(boolean ninos) {
        return new ContextoPlan(recetas, restricciones, diasSinTiempo, presupuestoSemanal, stock, porVencer, fijas, ninos, alergias, diasDesdeElPago);
    }
}
