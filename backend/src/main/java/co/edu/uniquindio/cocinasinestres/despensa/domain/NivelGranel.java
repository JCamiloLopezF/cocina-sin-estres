package co.edu.uniquindio.cocinasinestres.despensa.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Nivel cualitativo de un producto a granel (ADR-11, ADR-15).
 *
 * <p>La usuaria no pesa el arroz: dice si queda poco, medio o lleno. Para poder
 * comparar, descontar y costear, cada nivel equivale a una proporción de la
 * {@code capacidad_referencia} del producto y esa cantidad se guarda con
 * {@code es_estimada = TRUE}. El nivel que se muestra no se guarda: se calcula de
 * nuevo desde la cantidad, así sigue siendo correcto cuando otro proceso (por
 * ejemplo, marcar una comida como preparada) cambia la cantidad.</p>
 *
 * <p>Sin Spring ni JPA (ADR-09).</p>
 */
public enum NivelGranel {

    /** No queda nada. */
    AGOTADO("0"),

    /** Queda una cuarta parte de la capacidad. */
    POCO("0.25"),

    /** Queda la mitad de la capacidad. */
    MEDIO("0.5"),

    /** Está completo. */
    LLENO("1");

    /**
     * Hasta aquí (inclusive) una cantidad se muestra como "Poco": es el punto medio
     * entre Poco (0,25) y Medio (0,5), es decir, se elige el nivel más cercano.
     */
    static final BigDecimal LIMITE_POCO = new BigDecimal("0.375");

    /** Hasta aquí (inclusive) se muestra como "Medio": punto medio entre Medio y Lleno. */
    static final BigDecimal LIMITE_MEDIO = new BigDecimal("0.75");

    private static final int DECIMALES_CANTIDAD = 3;
    private static final int DECIMALES_PROPORCION = 6;

    private final BigDecimal proporcion;

    NivelGranel(String proporcion) {
        this.proporcion = new BigDecimal(proporcion);
    }

    /** Fracción de la capacidad de referencia que representa este nivel (de 0 a 1). */
    public BigDecimal proporcion() {
        return proporcion;
    }

    /**
     * Cantidad estimada, en unidad base, que equivale a este nivel.
     *
     * @param capacidadReferencia cantidad que equivale a "Lleno"; mayor que cero
     * @return la proporción de la capacidad, con 3 decimales (escala de la columna)
     */
    public BigDecimal aCantidad(BigDecimal capacidadReferencia) {
        validarCapacidad(capacidadReferencia);

        return capacidadReferencia
            .multiply(proporcion)
            .setScale(DECIMALES_CANTIDAD, RoundingMode.HALF_UP);
    }

    /**
     * Nivel que le corresponde a una cantidad: el más cercano entre los cuatro.
     *
     * <p>Una cantidad de cero es {@link #AGOTADO}; cualquier cantidad positiva es al
     * menos {@link #POCO}, así que un producto nunca se muestra agotado si todavía queda
     * algo.</p>
     *
     * @param cantidad            cantidad en unidad base; no negativa
     * @param capacidadReferencia cantidad que equivale a "Lleno"; mayor que cero
     */
    public static NivelGranel desde(BigDecimal cantidad, BigDecimal capacidadReferencia) {
        Objects.requireNonNull(cantidad, "cantidad");
        validarCapacidad(capacidadReferencia);

        if (cantidad.signum() <= 0) {
            return AGOTADO;
        }

        BigDecimal proporcionActual =
            cantidad.divide(capacidadReferencia, DECIMALES_PROPORCION, RoundingMode.HALF_UP);

        if (proporcionActual.compareTo(LIMITE_POCO) <= 0) {
            return POCO;
        }

        if (proporcionActual.compareTo(LIMITE_MEDIO) <= 0) {
            return MEDIO;
        }

        return LLENO;
    }

    private static void validarCapacidad(BigDecimal capacidadReferencia) {
        if (capacidadReferencia == null || capacidadReferencia.signum() <= 0) {
            throw new IllegalArgumentException(
                "La capacidad de referencia debe ser mayor que cero."
            );
        }
    }
}
