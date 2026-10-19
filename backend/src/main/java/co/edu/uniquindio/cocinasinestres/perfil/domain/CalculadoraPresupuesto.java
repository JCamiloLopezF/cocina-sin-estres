package co.edu.uniquindio.cocinasinestres.perfil.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Calculadora del presupuesto semanal según ADR-14.
 * Presupuesto semanal = monto * 7 / díasDelCiclo
 */
public final class CalculadoraPresupuesto {

    private CalculadoraPresupuesto() {}

    /**
     * Calcula el presupuesto semanal prorrateando el monto del ciclo de ingreso.
     *
     * @param monto                 monto del ciclo (hogar.presupuesto_monto). Si es null devuelve 0.00
     * @param periodicidadIngreso   valor String: "SEMANAL", "QUINCENAL" o "MENSUAL"
     * @return presupuesto semanal con escala 2
     */
    public static BigDecimal presupuestoSemanal(final BigDecimal monto, final String periodicidadIngreso) {
        if (monto == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        final BigDecimal diasDelCiclo;
        if (Objects.equals(periodicidadIngreso, "QUINCENAL")) {
            diasDelCiclo = BigDecimal.valueOf(15.2);
        } else if (Objects.equals(periodicidadIngreso, "MENSUAL")) {
            diasDelCiclo = BigDecimal.valueOf(30.4);
        } else {
            // SEMANAL por defecto
            diasDelCiclo = BigDecimal.valueOf(7);
        }

        return monto.multiply(BigDecimal.valueOf(7))
            .divide(diasDelCiclo, 2, RoundingMode.HALF_UP);
    }
}
