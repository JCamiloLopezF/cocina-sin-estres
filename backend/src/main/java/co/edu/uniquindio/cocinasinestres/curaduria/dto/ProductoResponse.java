package co.edu.uniquindio.cocinasinestres.curaduria.dto;

import co.edu.uniquindio.cocinasinestres.curaduria.domain.TipoCantidad;
import co.edu.uniquindio.cocinasinestres.curaduria.domain.UnidadBase;

import java.math.BigDecimal;

/**
 * Producto del catálogo tal como lo devuelve la API.
 *
 * @param id                   identificador del producto
 * @param nombre               nombre del producto
 * @param categoria            categoría del producto
 * @param tipoCantidad         si se maneja por unidades o a granel
 * @param unidadBase           unidad base: g, ml o unidad
 * @param unidadCompra         presentación de compra
 * @param cantidadUnidadCompra cantidad que trae la unidad de compra, en unidad base
 * @param precioUnidadCompra   precio de referencia de la unidad de compra, en pesos
 * @param capacidadReferencia  cantidad que equivale a "Lleno"; solo para granel
 * @param esBasico             si es un producto básico de la despensa
 * @param activo               si el producto está activo en el catálogo
 */
public record ProductoResponse(
    Long id,
    String nombre,
    String categoria,
    TipoCantidad tipoCantidad,
    UnidadBase unidadBase,
    String unidadCompra,
    BigDecimal cantidadUnidadCompra,
    BigDecimal precioUnidadCompra,
    BigDecimal capacidadReferencia,
    boolean esBasico,
    boolean activo
) {
}
