package co.edu.uniquindio.cocinasinestres.planificador.domain;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Cuándo le pagan a la usuaria, como lo guardó en el perfil del hogar (V1):
 *
 * <ul>
 *   <li>{@code SEMANAL}: {@code diaPago1} es el día de la semana (1 = lunes … 7 = domingo).</li>
 *   <li>{@code MENSUAL}: {@code diaPago1} es el día del mes (1 a 31).</li>
 *   <li>{@code QUINCENAL}: {@code diaPago1} y {@code diaPago2} son dos días del mes, en orden.</li>
 * </ul>
 *
 * <p>Si el mes no tiene ese día (un pago el 31 en un mes de 30 días), se toma el último
 * día del mes. Dominio puro: se prueba sin Spring.</p>
 *
 * @param periodicidad {@code SEMANAL}, {@code QUINCENAL} o {@code MENSUAL}
 * @param diaPago1     primer día de pago
 * @param diaPago2     segundo día de pago; solo en {@code QUINCENAL}
 */
public record CicloIngreso(String periodicidad, int diaPago1, Integer diaPago2) {

    public CicloIngreso {
        Objects.requireNonNull(periodicidad, "periodicidad");
    }

    /** El último día de pago en o antes de {@code fecha}. */
    public LocalDate ultimoPago(LocalDate fecha) {
        return switch (periodicidad) {
            case "SEMANAL" -> fecha.with(TemporalAdjusters.previousOrSame(DayOfWeek.of(diaPago1)));
            case "QUINCENAL" -> {
                LocalDate primero = ultimoDelMes(fecha, diaPago1);
                LocalDate segundo = diaPago2 == null ? primero : ultimoDelMes(fecha, diaPago2);
                yield primero.isAfter(segundo) ? primero : segundo;
            }
            default -> ultimoDelMes(fecha, diaPago1);
        };
    }

    /** Días que van desde el último pago hasta {@code fecha}: 0 el mismo día del pago. */
    public long diasDesdeElPago(LocalDate fecha) {
        return ChronoUnit.DAYS.between(ultimoPago(fecha), fecha);
    }

    /**
     * Para cada día de la semana que empieza en {@code lunes} (1 = lunes … 7 = domingo),
     * cuántos días han pasado desde el último pago.
     */
    public Map<Integer, Integer> diasDesdeElPagoEnLaSemana(LocalDate lunes) {
        Map<Integer, Integer> dias = new LinkedHashMap<>();
        for (int dia = 1; dia <= 7; dia++) {
            dias.put(dia, (int) diasDesdeElPago(lunes.plusDays(dia - 1L)));
        }
        return dias;
    }

    /** La última vez que fue el día {@code dia} del mes, en o antes de {@code fecha}. */
    private static LocalDate ultimoDelMes(LocalDate fecha, int dia) {
        LocalDate esteMes = enElMes(fecha, dia);
        return esteMes.isAfter(fecha) ? enElMes(fecha.minusMonths(1), dia) : esteMes;
    }

    private static LocalDate enElMes(LocalDate fecha, int dia) {
        return fecha.withDayOfMonth(Math.min(dia, fecha.lengthOfMonth()));
    }
}
