package co.edu.uniquindio.cocinasinestres.despensa.dto;

import co.edu.uniquindio.cocinasinestres.despensa.domain.NivelGranel;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Datos para agregar un producto a la despensa.
 *
 * <p>Según el tipo del producto se envía {@code cantidad} (contable) o {@code nivel}
 * (granel), nunca los dos. Lo mismo con {@code cantidadMinima} y {@code nivelMinimo}.</p>
 *
 * @param productoId       producto del catálogo que se agrega
 * @param cantidad         cantidad en la unidad base del producto; solo contables
 * @param nivel            AGOTADO, POCO, MEDIO o LLENO; solo a granel
 * @param cantidadMinima   mínimo opcional en la unidad base; solo contables
 * @param nivelMinimo      mínimo opcional (AGOTADO, POCO o MEDIO); solo a granel
 * @param fechaVencimiento fecha de vencimiento opcional, en formato AAAA-MM-DD
 * @param ubicacion        dónde se guarda, opcional (ej.: "Alacena")
 * @param cantidadEstimada {@code true} si la cantidad la puso la aplicación y no la usuaria
 *                         (carga rápida: una presentación de compra); se muestra como
 *                         estimada hasta que la usuaria la corrija
 */
public record ItemDespensaRequest(
    Long productoId,
    BigDecimal cantidad,
    NivelGranel nivel,
    BigDecimal cantidadMinima,
    NivelGranel nivelMinimo,
    LocalDate fechaVencimiento,
    String ubicacion,
    Boolean cantidadEstimada
) {

    /** Lo que la usuaria escribió: la cantidad no es estimada. */
    public ItemDespensaRequest(
        Long productoId, BigDecimal cantidad, NivelGranel nivel, BigDecimal cantidadMinima,
        NivelGranel nivelMinimo, LocalDate fechaVencimiento, String ubicacion) {
        this(productoId, cantidad, nivel, cantidadMinima, nivelMinimo, fechaVencimiento, ubicacion, null);
    }
}
