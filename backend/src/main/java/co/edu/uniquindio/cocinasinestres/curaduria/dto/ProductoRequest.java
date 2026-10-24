package co.edu.uniquindio.cocinasinestres.curaduria.dto;

import co.edu.uniquindio.cocinasinestres.curaduria.domain.TipoCantidad;
import co.edu.uniquindio.cocinasinestres.curaduria.domain.UnidadBase;

import java.math.BigDecimal;

/**
 * Datos para crear o actualizar un producto del catálogo.
 *
 * @param nombre               nombre del producto
 * @param categoria            categoría del producto
 * @param tipoCantidad         si se maneja por unidades o a granel
 * @param unidadBase           unidad base: g, ml o unidad
 * @param unidadCompra         presentación de compra (ej.: "bolsa 500 g")
 * @param cantidadUnidadCompra cantidad que trae la unidad de compra, en unidad base
 * @param precioUnidadCompra   precio de referencia de la unidad de compra, en pesos
 * @param capacidadReferencia  cantidad que equivale a "Lleno"; solo para granel
 * @param esBasico             si es un producto básico de la despensa
 * @param vidaUtilDias         cuántos días dura desde que se compra (1 a 3650); opcional. Con
 *                             esto lo comprado entra a la despensa con su vencimiento (ADR-24)
 */
public record ProductoRequest(
    String nombre,
    String categoria,
    TipoCantidad tipoCantidad,
    UnidadBase unidadBase,
    String unidadCompra,
    BigDecimal cantidadUnidadCompra,
    BigDecimal precioUnidadCompra,
    BigDecimal capacidadReferencia,
    boolean esBasico,
    Integer vidaUtilDias
) {
}
