package co.edu.uniquindio.cocinasinestres.despensa.dto;

import co.edu.uniquindio.cocinasinestres.despensa.domain.NivelGranel;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Datos para editar un producto de la despensa. Reemplaza todos los campos: lo que no
 * se envía (mínimo, vencimiento, ubicación) queda sin valor. El producto no se cambia.
 *
 * @param cantidad         cantidad en la unidad base del producto; solo contables
 * @param nivel            AGOTADO, POCO, MEDIO o LLENO; solo a granel
 * @param cantidadMinima   mínimo opcional en la unidad base; solo contables
 * @param nivelMinimo      mínimo opcional (AGOTADO, POCO o MEDIO); solo a granel
 * @param fechaVencimiento fecha de vencimiento opcional, en formato AAAA-MM-DD
 * @param ubicacion        dónde se guarda, opcional (ej.: "Alacena")
 */
public record EdicionItemDespensaRequest(
    BigDecimal cantidad,
    NivelGranel nivel,
    BigDecimal cantidadMinima,
    NivelGranel nivelMinimo,
    LocalDate fechaVencimiento,
    String ubicacion
) {
}
