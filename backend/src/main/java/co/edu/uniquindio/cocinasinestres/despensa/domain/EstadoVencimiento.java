package co.edu.uniquindio.cocinasinestres.despensa.domain;

/**
 * Situación de un producto de la despensa frente a su fecha de vencimiento.
 */
public enum EstadoVencimiento {

    /** La usuaria no registró fecha de vencimiento. */
    SIN_FECHA,

    /** Vence en más de {@link ReglasDespensa#DIAS_POR_VENCER} días. */
    VIGENTE,

    /** Vence hoy o dentro de los próximos {@link ReglasDespensa#DIAS_POR_VENCER} días. */
    POR_VENCER,

    /** La fecha de vencimiento ya pasó. */
    VENCIDO
}
