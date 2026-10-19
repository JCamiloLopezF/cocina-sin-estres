package co.edu.uniquindio.cocinasinestres.perfil.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Pruebas del prorrateo del presupuesto semanal según ADR-14:
 * el monto del ciclo de ingreso se reparte sobre los días del ciclo.
 */
@DisplayName("Calculadora de presupuesto semanal (ADR-14)")
class CalculadoraPresupuestoTest {

    @Test
    @DisplayName("semanal: el monto del ciclo es el presupuesto de la semana")
    void semanalNoSeProrratea() {
        BigDecimal semanal = CalculadoraPresupuesto.presupuestoSemanal(
            new BigDecimal("700000.00"), "SEMANAL");

        assertEquals(0, semanal.compareTo(new BigDecimal("700000.00")));
        assertEquals(2, semanal.scale());
    }

    @Test
    @DisplayName("quincenal: prorratea sobre 15.2 días")
    void quincenalProrrateaSobreQuincePuntoDosDias() {
        // 700000 * 7 / 15.2 = 322368.421... → 322368.42
        BigDecimal semanal = CalculadoraPresupuesto.presupuestoSemanal(
            new BigDecimal("700000.00"), "QUINCENAL");

        assertEquals(0, semanal.compareTo(new BigDecimal("322368.42")));
        assertEquals(2, semanal.scale());
    }

    @Test
    @DisplayName("mensual: prorratea sobre 30.4 días")
    void mensualProrrateaSobreTreintaPuntoCuatroDias() {
        // 700000 * 7 / 30.4 = 161184.210... → 161184.21
        BigDecimal semanal = CalculadoraPresupuesto.presupuestoSemanal(
            new BigDecimal("700000.00"), "MENSUAL");

        assertEquals(0, semanal.compareTo(new BigDecimal("161184.21")));
        assertEquals(2, semanal.scale());
    }

    @Test
    @DisplayName("monto nulo: responde 0.00 en vez de fallar")
    void montoNuloRespondeCero() {
        BigDecimal semanal = CalculadoraPresupuesto.presupuestoSemanal(null, "MENSUAL");

        assertEquals(0, semanal.compareTo(BigDecimal.ZERO));
        assertEquals(2, semanal.scale());
    }
}
