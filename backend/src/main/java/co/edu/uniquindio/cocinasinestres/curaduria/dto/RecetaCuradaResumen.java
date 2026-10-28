package co.edu.uniquindio.cocinasinestres.curaduria.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Una fila del listado de recetas del curador, activas y desactivadas (RNF-28).
 *
 * @param ingredientes cuántos ingredientes tiene
 * @param costo        costo de la receta completa con los precios de referencia
 */
public record RecetaCuradaResumen(
    Long id,
    String nombre,
    int tiempoMinutos,
    int porciones,
    List<String> momentos,
    boolean platoFuerte,
    boolean activa,
    int ingredientes,
    BigDecimal costo,
    boolean tieneVideo) {
}
