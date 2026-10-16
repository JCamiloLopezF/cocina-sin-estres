package co.edu.uniquindio.cocinasinestres.despensa.service;

import co.edu.uniquindio.cocinasinestres.despensa.domain.EstadoVencimiento;
import co.edu.uniquindio.cocinasinestres.despensa.domain.NivelGranel;
import co.edu.uniquindio.cocinasinestres.despensa.domain.ProductoCatalogo;
import co.edu.uniquindio.cocinasinestres.despensa.domain.ReglasDespensa;
import co.edu.uniquindio.cocinasinestres.despensa.dto.ItemDespensaResponse;
import co.edu.uniquindio.cocinasinestres.despensa.model.ItemDespensa;

import java.time.LocalDate;

/**
 * Arma la respuesta de la API a partir del registro de la despensa y su producto,
 * calculando lo que no se guarda: el nivel, los días para vencer y si está bajo el mínimo.
 */
final class ItemDespensaMapper {

    private ItemDespensaMapper() {
    }

    /**
     * @param item     registro de la despensa
     * @param producto producto del catálogo al que apunta el registro
     * @param hoy      fecha actual en la zona horaria de la usuaria
     */
    static ItemDespensaResponse aResponse(ItemDespensa item, ProductoCatalogo producto, LocalDate hoy) {

        boolean esGranel = producto.esGranel();

        NivelGranel nivel = esGranel
            ? NivelGranel.desde(item.getCantidad(), producto.capacidadReferencia())
            : null;

        NivelGranel nivelMinimo = esGranel && item.getCantidadMinima() != null
            ? NivelGranel.desde(item.getCantidadMinima(), producto.capacidadReferencia())
            : null;

        EstadoVencimiento estado = ReglasDespensa.estadoVencimiento(item.getVenceEn(), hoy);

        return new ItemDespensaResponse(
            item.getId(),
            item.getProductoId(),
            producto.nombre(),
            producto.categoria(),
            producto.tipoCantidad(),
            producto.unidadBase(),
            item.getCantidad(),
            item.isEsEstimada(),
            nivel,
            item.getCantidadMinima(),
            nivelMinimo,
            item.getVenceEn(),
            ReglasDespensa.diasParaVencer(item.getVenceEn(), hoy),
            estado,
            ReglasDespensa.estaBajoMinimo(item.getCantidad(), item.getCantidadMinima()),
            item.getUbicacion(),
            producto.unidadCompra(),
            producto.cantidadUnidadCompra(),
            producto.precioUnidadCompra()
        );
    }
}
