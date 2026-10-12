package co.edu.uniquindio.cocinasinestres.curaduria.service;

import co.edu.uniquindio.cocinasinestres.curaduria.domain.InstantaneaProducto;
import co.edu.uniquindio.cocinasinestres.curaduria.dto.ProductoResponse;
import co.edu.uniquindio.cocinasinestres.curaduria.model.Producto;

/**
 * Conversiones de {@link Producto} a sus representaciones de salida y de auditoría.
 */
final class ProductoMapper {

    private ProductoMapper() {
    }

    static ProductoResponse aResponse(Producto producto) {
        return new ProductoResponse(
            producto.getId(),
            producto.getNombre(),
            producto.getCategoria(),
            producto.getTipoCantidad(),
            producto.getUnidadBase(),
            producto.getUnidadCompra(),
            producto.getCantidadUnidadCompra(),
            producto.getPrecioUnidadCompra(),
            producto.getCapacidadReferencia(),
            producto.isEsBasico(),
            producto.isActivo()
        );
    }

    static InstantaneaProducto aInstantanea(Producto producto) {
        return new InstantaneaProducto(
            producto.getNombre(),
            producto.getCategoria(),
            producto.getTipoCantidad(),
            producto.getUnidadBase(),
            producto.getUnidadCompra(),
            producto.getCantidadUnidadCompra(),
            producto.getPrecioUnidadCompra(),
            producto.getCapacidadReferencia(),
            producto.isEsBasico(),
            producto.isActivo()
        );
    }
}
