package co.edu.uniquindio.cocinasinestres.curaduria.domain;

import co.edu.uniquindio.cocinasinestres.comun.error.DatosInvalidosException;

import java.math.BigDecimal;

/**
 * Valida las reglas de negocio de los productos del catálogo global.
 *
 * <p>Refleja lo que exige la base de datos ({@code V1__esquema_inicial.sql}) para que
 * un dato inválido responda 400 con un mensaje claro y no un 500 por una restricción
 * rota: longitudes de columna, rangos de los decimales y el precio de referencia
 * completo de todo producto del catálogo (RNF-02).</p>
 *
 * <p>No utiliza Spring ni JPA porque pertenece al dominio.</p>
 */
public final class ValidadorProducto {

    static final int LONGITUD_MAXIMA_NOMBRE = 120;
    static final int LONGITUD_MAXIMA_CATEGORIA = 60;
    static final int LONGITUD_MAXIMA_UNIDAD_COMPRA = 30;

    /** DECIMAL(12,3): hasta 9 enteros y 3 decimales. */
    private static final int ENTEROS_CANTIDAD = 9;
    private static final int DECIMALES_CANTIDAD = 3;

    /** DECIMAL(12,2): hasta 10 enteros y 2 decimales. */
    private static final int ENTEROS_PRECIO = 10;
    private static final int DECIMALES_PRECIO = 2;

    private ValidadorProducto() {
    }

    public static void validar(
        String nombre,
        String categoria,
        TipoCantidad tipoCantidad,
        UnidadBase unidadBase,
        String unidadCompra,
        BigDecimal cantidadUnidadCompra,
        BigDecimal precioUnidadCompra,
        BigDecimal capacidadReferencia) {

        if (nombre == null || nombre.isBlank()) {
            throw new DatosInvalidosException(
                "PRODUCTO_NOMBRE_REQUERIDO",
                "El nombre del producto es obligatorio."
            );
        }

        if (nombre.length() > LONGITUD_MAXIMA_NOMBRE) {
            throw new DatosInvalidosException(
                "PRODUCTO_NOMBRE_LARGO",
                "El nombre no puede superar " + LONGITUD_MAXIMA_NOMBRE + " caracteres."
            );
        }

        if (categoria == null || categoria.isBlank()) {
            throw new DatosInvalidosException(
                "PRODUCTO_CATEGORIA_REQUERIDA",
                "La categoría del producto es obligatoria."
            );
        }

        if (categoria.length() > LONGITUD_MAXIMA_CATEGORIA) {
            throw new DatosInvalidosException(
                "PRODUCTO_CATEGORIA_LARGA",
                "La categoría no puede superar " + LONGITUD_MAXIMA_CATEGORIA + " caracteres."
            );
        }

        if (tipoCantidad == null) {
            throw new DatosInvalidosException(
                "PRODUCTO_TIPO_CANTIDAD_REQUERIDO",
                "El tipo de cantidad es obligatorio."
            );
        }

        if (unidadBase == null) {
            throw new DatosInvalidosException(
                "PRODUCTO_UNIDAD_BASE_REQUERIDA",
                "La unidad base es obligatoria."
            );
        }

        // Precio de referencia completo (RNF-02): la base lo exige a todo producto global.
        if (unidadCompra == null || unidadCompra.isBlank()) {
            throw new DatosInvalidosException(
                "PRODUCTO_UNIDAD_COMPRA_REQUERIDA",
                "La unidad de compra es obligatoria."
            );
        }

        if (unidadCompra.length() > LONGITUD_MAXIMA_UNIDAD_COMPRA) {
            throw new DatosInvalidosException(
                "PRODUCTO_UNIDAD_COMPRA_LARGA",
                "La unidad de compra no puede superar " + LONGITUD_MAXIMA_UNIDAD_COMPRA + " caracteres."
            );
        }

        if (cantidadUnidadCompra == null) {
            throw new DatosInvalidosException(
                "PRODUCTO_CANTIDAD_COMPRA_REQUERIDA",
                "La cantidad de la unidad de compra es obligatoria."
            );
        }

        if (cantidadUnidadCompra.compareTo(BigDecimal.ZERO) <= 0
            || excedeDecimal(cantidadUnidadCompra, ENTEROS_CANTIDAD, DECIMALES_CANTIDAD)) {

            throw new DatosInvalidosException(
                "PRODUCTO_CANTIDAD_COMPRA_INVALIDA",
                "La cantidad de la unidad de compra debe ser mayor que cero y tener hasta "
                    + DECIMALES_CANTIDAD + " decimales."
            );
        }

        if (precioUnidadCompra == null) {
            throw new DatosInvalidosException(
                "PRODUCTO_PRECIO_REQUERIDO",
                "El precio de referencia es obligatorio."
            );
        }

        if (precioUnidadCompra.compareTo(BigDecimal.ZERO) < 0
            || excedeDecimal(precioUnidadCompra, ENTEROS_PRECIO, DECIMALES_PRECIO)) {

            throw new DatosInvalidosException(
                "PRODUCTO_PRECIO_INVALIDO",
                "El precio de referencia no puede ser negativo y admite hasta "
                    + DECIMALES_PRECIO + " decimales."
            );
        }

        if (tipoCantidad == TipoCantidad.GRANEL) {

            if (capacidadReferencia == null
                || capacidadReferencia.compareTo(BigDecimal.ZERO) <= 0
                || excedeDecimal(capacidadReferencia, ENTEROS_CANTIDAD, DECIMALES_CANTIDAD)) {

                throw new DatosInvalidosException(
                    "PRODUCTO_CAPACIDAD_REQUERIDA",
                    "Los productos a granel deben tener una capacidad de referencia mayor que cero."
                );
            }

        } else if (capacidadReferencia != null) {

            throw new DatosInvalidosException(
                "PRODUCTO_CAPACIDAD_NO_PERMITIDA",
                "Los productos contables no deben tener capacidad de referencia."
            );
        }
    }

    /**
     * Indica si el valor no cabe en una columna DECIMAL con esos enteros y decimales.
     * Los ceros a la derecha no cuentan: 500 y 500.000 son el mismo valor.
     */
    private static boolean excedeDecimal(BigDecimal valor, int maximoEnteros, int maximoDecimales) {
        BigDecimal limpio = valor.stripTrailingZeros();
        int enteros = limpio.precision() - limpio.scale();
        int decimales = Math.max(limpio.scale(), 0);

        return enteros > maximoEnteros || decimales > maximoDecimales;
    }
}
