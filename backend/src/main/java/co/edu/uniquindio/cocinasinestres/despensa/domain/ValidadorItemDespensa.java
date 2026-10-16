package co.edu.uniquindio.cocinasinestres.despensa.domain;

import co.edu.uniquindio.cocinasinestres.comun.error.DatosInvalidosException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Aplica la regla de <strong>cantidad mixta</strong> (ADR-11) y valida los datos de un
 * producto de la despensa.
 *
 * <ul>
 *   <li>Producto <strong>contable</strong>: se registra una {@code cantidad} numérica y
 *       un mínimo opcional también numérico.</li>
 *   <li>Producto <strong>a granel</strong>: se registra un {@code nivel} y un nivel
 *       mínimo opcional. Se convierten a cantidades con la capacidad de referencia del
 *       producto y la cantidad queda marcada como estimada.</li>
 * </ul>
 *
 * <p>Mezclar los dos modos (un nivel en un producto contable, o una cantidad en uno a
 * granel) se rechaza con un error claro: aceptarlo en silencio dejaría datos que el
 * cruce con el plan no sabría interpretar.</p>
 *
 * <p>Refleja las restricciones de {@code item_despensa} en {@code V1} para que un dato
 * inválido responda 400 y no un 500 por una restricción rota. Sin Spring ni JPA.</p>
 */
public final class ValidadorItemDespensa {

    static final int LONGITUD_MAXIMA_UBICACION = 40;

    /** DECIMAL(12,3): hasta 9 enteros y 3 decimales. */
    private static final int ENTEROS_CANTIDAD = 9;
    private static final int DECIMALES_CANTIDAD = 3;

    private ValidadorItemDespensa() {
    }

    /**
     * Valida y normaliza los datos que llegan para un producto de la despensa.
     *
     * @param producto         producto del catálogo al que corresponde el registro
     * @param cantidad         cantidad numérica; solo para productos contables
     * @param nivel            nivel; solo para productos a granel
     * @param cantidadMinima   mínimo numérico opcional; solo para productos contables
     * @param nivelMinimo      nivel mínimo opcional; solo para productos a granel
     * @param fechaVencimiento fecha de vencimiento opcional
     * @param ubicacion        ubicación opcional; los espacios sobrantes se quitan
     * @throws DatosInvalidosException si algún dato incumple las reglas
     */
    public static DatosItemDespensa validar(
        ProductoCatalogo producto,
        BigDecimal cantidad,
        NivelGranel nivel,
        BigDecimal cantidadMinima,
        NivelGranel nivelMinimo,
        LocalDate fechaVencimiento,
        String ubicacion) {

        Objects.requireNonNull(producto, "producto");

        BigDecimal cantidadFinal;
        BigDecimal minimaFinal;
        boolean esEstimada;

        if (producto.esGranel()) {
            exigirCapacidad(producto);

            if (cantidad != null) {
                throw new DatosInvalidosException(
                    CodigoDespensa.CANTIDAD_NO_PERMITIDA,
                    "Este producto se registra por nivel (Poco, Medio o Lleno), no por cantidad."
                );
            }

            if (cantidadMinima != null) {
                throw new DatosInvalidosException(
                    CodigoDespensa.MINIMO_NO_PERMITIDO,
                    "El mínimo de este producto se elige por nivel, no por cantidad."
                );
            }

            if (nivel == null) {
                throw new DatosInvalidosException(
                    CodigoDespensa.NIVEL_REQUERIDO,
                    "Elige cuánto te queda: Poco, Medio o Lleno."
                );
            }

            if (nivelMinimo == NivelGranel.LLENO) {
                throw new DatosInvalidosException(
                    CodigoDespensa.NIVEL_MINIMO_INVALIDO,
                    "El nivel mínimo no puede ser Lleno; elige Poco o Medio."
                );
            }

            cantidadFinal = nivel.aCantidad(producto.capacidadReferencia());
            minimaFinal = nivelMinimo == null
                ? null
                : nivelMinimo.aCantidad(producto.capacidadReferencia());
            esEstimada = true;

        } else {

            if (nivel != null) {
                throw new DatosInvalidosException(
                    CodigoDespensa.NIVEL_NO_PERMITIDO,
                    "Este producto se registra por cantidad, no por nivel."
                );
            }

            if (nivelMinimo != null) {
                throw new DatosInvalidosException(
                    CodigoDespensa.NIVEL_MINIMO_NO_PERMITIDO,
                    "El mínimo de este producto se indica como cantidad, no como nivel."
                );
            }

            if (cantidad == null) {
                throw new DatosInvalidosException(
                    CodigoDespensa.CANTIDAD_REQUERIDA,
                    "Indica cuánto tienes de este producto."
                );
            }

            if (esInvalida(cantidad)) {
                throw new DatosInvalidosException(
                    CodigoDespensa.CANTIDAD_INVALIDA,
                    "La cantidad no puede ser negativa y admite hasta "
                        + DECIMALES_CANTIDAD + " decimales."
                );
            }

            if (cantidadMinima != null && esInvalida(cantidadMinima)) {
                throw new DatosInvalidosException(
                    CodigoDespensa.MINIMO_INVALIDO,
                    "El mínimo no puede ser negativo y admite hasta "
                        + DECIMALES_CANTIDAD + " decimales."
                );
            }

            cantidadFinal = escalar(cantidad);
            minimaFinal = cantidadMinima == null ? null : escalar(cantidadMinima);
            esEstimada = false;
        }

        return new DatosItemDespensa(
            cantidadFinal,
            esEstimada,
            minimaFinal,
            fechaVencimiento,
            normalizarUbicacion(ubicacion)
        );
    }

    private static void exigirCapacidad(ProductoCatalogo producto) {
        // La base lo garantiza (ck_producto_capacidad); si falla, es un dato corrupto
        // del catálogo y no un error de la usuaria.
        if (producto.capacidadReferencia() == null || producto.capacidadReferencia().signum() <= 0) {
            throw new IllegalStateException(
                "El producto a granel " + producto.id() + " no tiene capacidad de referencia."
            );
        }
    }

    private static String normalizarUbicacion(String ubicacion) {
        if (ubicacion == null || ubicacion.isBlank()) {
            return null;
        }

        String limpia = ubicacion.trim();

        if (limpia.length() > LONGITUD_MAXIMA_UBICACION) {
            throw new DatosInvalidosException(
                CodigoDespensa.UBICACION_LARGA,
                "La ubicación no puede superar " + LONGITUD_MAXIMA_UBICACION + " caracteres."
            );
        }

        return limpia;
    }

    /** Una cantidad es inválida si es negativa o no cabe en DECIMAL(12,3). */
    private static boolean esInvalida(BigDecimal valor) {
        if (valor.signum() < 0) {
            return true;
        }

        // Los ceros a la derecha no cuentan: 500 y 500.000 son el mismo valor.
        BigDecimal limpio = valor.signum() == 0 ? BigDecimal.ZERO : valor.stripTrailingZeros();
        int enteros = limpio.precision() - limpio.scale();
        int decimales = Math.max(limpio.scale(), 0);

        return enteros > ENTEROS_CANTIDAD || decimales > DECIMALES_CANTIDAD;
    }

    private static BigDecimal escalar(BigDecimal valor) {
        return valor.setScale(DECIMALES_CANTIDAD, RoundingMode.UNNECESSARY);
    }
}
