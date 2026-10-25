package co.edu.uniquindio.cocinasinestres.avisos.dto;

import java.time.LocalDateTime;

/**
 * Un aviso para la usuaria (ADR-17).
 *
 * @param tipo    {@code BAJO_MINIMO}, {@code POR_VENCER}, {@code PREPARACION_PREVIA},
 *                {@code SEMANA_SIN_PLANIFICAR} o {@code APROVECHAR_PRODUCTO}
 * @param enlace  ruta de la aplicación a la que lleva el botón
 * @param accion  texto del botón
 */
public record AvisoResponse(
    Long id,
    String tipo,
    String titulo,
    String mensaje,
    String enlace,
    String accion,
    boolean leido,
    LocalDateTime creadoEn) {
}
