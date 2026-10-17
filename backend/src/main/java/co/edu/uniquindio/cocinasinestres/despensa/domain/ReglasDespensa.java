package co.edu.uniquindio.cocinasinestres.despensa.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * Reglas de la despensa que no dependen de Spring ni de la base de datos (ADR-09).
 *
 * <p>Son las que reutilizan el listado, el resumen del inicio y, más adelante, el
 * motor de avisos (BAJO_MINIMO y POR_VENCER): así "bajo el mínimo" y "por vencer"
 * significan lo mismo en toda la aplicación.</p>
 */
public final class ReglasDespensa {

    /** Días de anticipación con los que un producto se considera "por vencer". */
    public static final int DIAS_POR_VENCER = 7;

    private ReglasDespensa() {
    }

    /**
     * Situación de un producto frente a su vencimiento.
     *
     * <p>Un producto que vence hoy todavía se puede usar hoy, así que está
     * {@link EstadoVencimiento#POR_VENCER}; solo es {@link EstadoVencimiento#VENCIDO}
     * desde el día siguiente.</p>
     *
     * @param fechaVencimiento fecha registrada, o {@code null} si no hay
     * @param hoy              fecha actual en la zona horaria de la usuaria
     */
    public static EstadoVencimiento estadoVencimiento(LocalDate fechaVencimiento, LocalDate hoy) {
        Objects.requireNonNull(hoy, "hoy");

        if (fechaVencimiento == null) {
            return EstadoVencimiento.SIN_FECHA;
        }

        if (fechaVencimiento.isBefore(hoy)) {
            return EstadoVencimiento.VENCIDO;
        }

        if (fechaVencimiento.isAfter(hoy.plusDays(DIAS_POR_VENCER))) {
            return EstadoVencimiento.VIGENTE;
        }

        return EstadoVencimiento.POR_VENCER;
    }

    /**
     * Días que faltan para el vencimiento; negativo si ya venció.
     *
     * @return los días, o {@code null} si el producto no tiene fecha de vencimiento
     */
    public static Integer diasParaVencer(LocalDate fechaVencimiento, LocalDate hoy) {
        Objects.requireNonNull(hoy, "hoy");

        if (fechaVencimiento == null) {
            return null;
        }

        return (int) ChronoUnit.DAYS.between(hoy, fechaVencimiento);
    }

    /**
     * Indica si la cantidad llegó al mínimo o quedó por debajo.
     *
     * <p>El mínimo se avisa al <strong>alcanzarlo</strong>, no cuando ya se pasó: la
     * pantalla 18 dice "avisarme cuando quede en Poco" y un producto en "Poco" ya
     * cuenta como bajo el mínimo (pantalla 14). Sin mínimo registrado nunca está bajo
     * el mínimo.</p>
     *
     * @param cantidad       cantidad actual en unidad base
     * @param cantidadMinima mínimo en unidad base, o {@code null} si no hay
     */
    public static boolean estaBajoMinimo(BigDecimal cantidad, BigDecimal cantidadMinima) {
        Objects.requireNonNull(cantidad, "cantidad");

        return cantidadMinima != null && cantidad.compareTo(cantidadMinima) <= 0;
    }

    /**
     * Indica si un producto aparece en la pestaña del listado.
     *
     * @param filtro      pestaña elegida
     * @param vencimiento situación del producto frente a su fecha de vencimiento
     * @param bajoMinimo  si el producto está en su mínimo o por debajo
     */
    public static boolean cumpleFiltro(
        FiltroDespensa filtro,
        EstadoVencimiento vencimiento,
        boolean bajoMinimo) {

        return switch (Objects.requireNonNull(filtro, "filtro")) {
            case TODO -> true;
            // Los vencidos también: son los que más urgen y no deben desaparecer del filtro
            case POR_VENCER -> vencimiento == EstadoVencimiento.POR_VENCER
                || vencimiento == EstadoVencimiento.VENCIDO;
            case BAJO_MINIMO -> bajoMinimo;
        };
    }
}
