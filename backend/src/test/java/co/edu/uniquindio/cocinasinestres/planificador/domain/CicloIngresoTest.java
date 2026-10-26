package co.edu.uniquindio.cocinasinestres.planificador.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Ciclo de ingreso: cuándo le pagan a la usuaria")
class CicloIngresoTest {

    /** Lunes 26 de octubre de 2026. */
    private static final LocalDate LUNES = LocalDate.of(2026, 10, 26);

    @Test
    @DisplayName("semanal: cuenta desde el último día de pago de la semana")
    void semanal() {
        CicloIngreso viernes = new CicloIngreso("SEMANAL", 5, null);

        assertThat(viernes.ultimoPago(LUNES)).isEqualTo(LocalDate.of(2026, 10, 23));
        assertThat(viernes.diasDesdeElPagoEnLaSemana(LUNES))
            .containsEntry(1, 3).containsEntry(4, 6).containsEntry(5, 0).containsEntry(6, 1).containsEntry(7, 2);
    }

    @Test
    @DisplayName("mensual: el día del pago reinicia la cuenta")
    void mensual() {
        CicloIngreso el30 = new CicloIngreso("MENSUAL", 30, null);

        assertThat(el30.diasDesdeElPagoEnLaSemana(LUNES))
            .containsEntry(1, 26).containsEntry(5, 0).containsEntry(6, 1);
    }

    @Test
    @DisplayName("mensual: si el mes no tiene ese día, cuenta el último día del mes")
    void mesCorto() {
        CicloIngreso el31 = new CicloIngreso("MENSUAL", 31, null);

        assertThat(el31.ultimoPago(LocalDate.of(2026, 11, 30))).isEqualTo(LocalDate.of(2026, 11, 30));
        assertThat(el31.ultimoPago(LocalDate.of(2026, 11, 29))).isEqualTo(LocalDate.of(2026, 10, 31));
        assertThat(el31.ultimoPago(LocalDate.of(2026, 3, 1))).isEqualTo(LocalDate.of(2026, 2, 28));
    }

    @Test
    @DisplayName("quincenal: toma el más reciente de los dos pagos")
    void quincenal() {
        CicloIngreso quincena = new CicloIngreso("QUINCENAL", 15, 30);

        assertThat(quincena.ultimoPago(LocalDate.of(2026, 10, 20))).isEqualTo(LocalDate.of(2026, 10, 15));
        assertThat(quincena.ultimoPago(LocalDate.of(2026, 11, 2))).isEqualTo(LocalDate.of(2026, 10, 30));
        assertThat(quincena.ultimoPago(LocalDate.of(2026, 1, 10))).isEqualTo(LocalDate.of(2025, 12, 30));
        assertThat(quincena.diasDesdeElPago(LocalDate.of(2026, 10, 15))).isZero();
    }
}
