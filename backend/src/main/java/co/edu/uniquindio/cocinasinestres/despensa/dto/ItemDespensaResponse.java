package co.edu.uniquindio.cocinasinestres.despensa.dto;

import co.edu.uniquindio.cocinasinestres.curaduria.domain.TipoCantidad;
import co.edu.uniquindio.cocinasinestres.curaduria.domain.UnidadBase;
import co.edu.uniquindio.cocinasinestres.despensa.domain.EstadoVencimiento;
import co.edu.uniquindio.cocinasinestres.despensa.domain.NivelGranel;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Un producto de la despensa tal como lo devuelve la API.
 *
 * <p>Los campos opcionales que no aplican se omiten del JSON: {@code nivel} y
 * {@code nivelMinimo} solo existen en productos a granel.</p>
 *
 * @param id                   identificador del registro en la despensa
 * @param productoId           producto del catálogo
 * @param nombre               nombre del producto
 * @param categoria            categoría del producto
 * @param tipoCantidad         CONTABLE (se cuenta) o GRANEL (se registra por nivel)
 * @param unidadBase           unidad en la que se expresan las cantidades
 * @param cantidad             cantidad actual en unidad base; estimada si es a granel
 * @param esEstimada           {@code true} cuando la cantidad sale de un nivel
 * @param nivel                nivel actual; solo a granel
 * @param cantidadMinima       mínimo en unidad base; ausente si no hay
 * @param nivelMinimo          nivel del mínimo; solo a granel y si hay mínimo
 * @param fechaVencimiento     fecha de vencimiento; ausente si no hay
 * @param diasParaVencer       días que faltan, negativo si ya venció; ausente sin fecha
 * @param estadoVencimiento    SIN_FECHA, VIGENTE, POR_VENCER o VENCIDO
 * @param bajoMinimo           {@code true} si llegó al mínimo o quedó por debajo
 * @param ubicacion            dónde se guarda; ausente si no se indicó
 * @param unidadCompra         presentación de compra del precio de referencia
 * @param cantidadUnidadCompra cuánto trae la unidad de compra, en unidad base
 * @param precioUnidadCompra   precio de referencia de la unidad de compra, en pesos
 */
public record ItemDespensaResponse(
    Long id,
    Long productoId,
    String nombre,
    String categoria,
    TipoCantidad tipoCantidad,
    UnidadBase unidadBase,
    BigDecimal cantidad,
    boolean esEstimada,
    NivelGranel nivel,
    BigDecimal cantidadMinima,
    NivelGranel nivelMinimo,
    LocalDate fechaVencimiento,
    Integer diasParaVencer,
    EstadoVencimiento estadoVencimiento,
    boolean bajoMinimo,
    String ubicacion,
    String unidadCompra,
    BigDecimal cantidadUnidadCompra,
    BigDecimal precioUnidadCompra
) {
}
