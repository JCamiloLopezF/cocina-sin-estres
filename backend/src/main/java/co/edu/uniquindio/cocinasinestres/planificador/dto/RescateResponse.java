package co.edu.uniquindio.cocinasinestres.planificador.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Modo rescate (KAN-25): lo que se puede cocinar hoy sin comprar nada.
 *
 * @param opciones primero las que salvan el producto que vence antes
 */
public record RescateResponse(List<Opcion> opciones) {

    /**
     * @param recetaId      receta
     * @param nombre        su nombre
     * @param tiempoMinutos tiempo total
     * @param estado        frente a las alergias: {@code APTA}, {@code ADAPTADA} o {@code PLATO_APARTE}
     * @param aprovecha     productos por vencer que usa, del que vence primero al último
     */
    public record Opcion(Long recetaId, String nombre, int tiempoMinutos, String estado, List<PorVencer> aprovecha) {
    }

    /**
     * @param dias días que faltan: 0 es hoy
     */
    public record PorVencer(Long productoId, String nombre, LocalDate venceEn, int dias) {
    }
}
