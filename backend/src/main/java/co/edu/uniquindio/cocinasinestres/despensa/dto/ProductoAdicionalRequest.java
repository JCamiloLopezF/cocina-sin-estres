package co.edu.uniquindio.cocinasinestres.despensa.dto;

import co.edu.uniquindio.cocinasinestres.curaduria.domain.TipoCantidad;
import co.edu.uniquindio.cocinasinestres.curaduria.domain.UnidadBase;
import co.edu.uniquindio.cocinasinestres.despensa.domain.NivelGranel;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Registrar un producto que no está en el catálogo y agregarlo de una vez a la
 * despensa (proceso 2, ADR-18). Solo lo ve el hogar que lo registra.
 *
 * <p>Los datos de la despensa siguen la misma regla de cantidad mixta que
 * {@link ItemDespensaRequest}: {@code cantidad} si se cuenta, {@code nivel} si va a granel.</p>
 *
 * @param nombre           nombre del producto, hasta 120 caracteres
 * @param categoria        categoría; si no llega, queda en OTROS
 * @param tipoCantidad     CONTABLE o GRANEL
 * @param unidadBase       unidad: {@code unidad} si se cuenta; {@code g} o {@code ml} a granel.
 *                         Si no llega, se toma {@code unidad} o {@code g} según el tipo
 * @param cantidad         cantidad inicial; solo contables
 * @param nivel            nivel inicial; solo a granel
 * @param cantidadMinima   mínimo opcional; solo contables
 * @param nivelMinimo      mínimo opcional; solo a granel
 * @param fechaVencimiento vencimiento opcional
 * @param ubicacion        dónde se guarda, opcional
 */
public record ProductoAdicionalRequest(
    String nombre,
    String categoria,
    TipoCantidad tipoCantidad,
    UnidadBase unidadBase,
    BigDecimal cantidad,
    NivelGranel nivel,
    BigDecimal cantidadMinima,
    NivelGranel nivelMinimo,
    LocalDate fechaVencimiento,
    String ubicacion
) {
}
