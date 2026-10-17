package co.edu.uniquindio.cocinasinestres.despensa.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Datos de un producto de la despensa ya validados y listos para guardar.
 *
 * @param cantidad         cantidad en unidad base, con 3 decimales
 * @param esEstimada       {@code true} cuando la cantidad sale de un nivel y no de un conteo
 * @param cantidadMinima   mínimo en unidad base, con 3 decimales; {@code null} si no hay
 * @param fechaVencimiento fecha de vencimiento; {@code null} si no hay
 * @param ubicacion        dónde se guarda (ej.: "Nevera"); {@code null} si no se indicó
 */
public record DatosItemDespensa(
    BigDecimal cantidad,
    boolean esEstimada,
    BigDecimal cantidadMinima,
    LocalDate fechaVencimiento,
    String ubicacion) {
}
