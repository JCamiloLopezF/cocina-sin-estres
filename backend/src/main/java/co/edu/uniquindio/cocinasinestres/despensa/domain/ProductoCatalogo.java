package co.edu.uniquindio.cocinasinestres.despensa.domain;

import co.edu.uniquindio.cocinasinestres.curaduria.domain.TipoCantidad;
import co.edu.uniquindio.cocinasinestres.curaduria.domain.UnidadBase;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Lo que la despensa necesita saber de un producto del catálogo.
 *
 * <p>Es una vista de solo lectura: la despensa no usa la entidad {@code Producto} ni
 * su repositorio (RNF-26), sino este registro que le entrega {@link CatalogoProductos}.</p>
 *
 * @param id                   identificador del producto
 * @param nombre               nombre del producto
 * @param categoria            categoría del producto
 * @param tipoCantidad         si se cuenta por cantidad o se registra por nivel
 * @param unidadBase           unidad en la que se expresan las cantidades
 * @param capacidadReferencia  cantidad que equivale a "Lleno"; solo en productos a granel
 * @param unidadCompra         presentación de compra (ej.: "Bolsa"); puede faltar en un
 *                             producto adicional del hogar
 * @param cantidadUnidadCompra cuánto trae la unidad de compra, en unidad base
 * @param precioUnidadCompra   precio de referencia de la unidad de compra, en pesos
 */
public record ProductoCatalogo(
    Long id,
    String nombre,
    String categoria,
    TipoCantidad tipoCantidad,
    UnidadBase unidadBase,
    BigDecimal capacidadReferencia,
    String unidadCompra,
    BigDecimal cantidadUnidadCompra,
    BigDecimal precioUnidadCompra) {

    public ProductoCatalogo {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(nombre, "nombre");
        Objects.requireNonNull(categoria, "categoria");
        Objects.requireNonNull(tipoCantidad, "tipoCantidad");
        Objects.requireNonNull(unidadBase, "unidadBase");
    }

    /** Indica si el producto se registra por nivel (Poco, Medio, Lleno) y no por cantidad. */
    public boolean esGranel() {
        return tipoCantidad == TipoCantidad.GRANEL;
    }
}
