package co.edu.uniquindio.cocinasinestres.curaduria.dto;

/**
 * Un reemplazo de un producto, como lo ve el curador.
 *
 * @param origen {@code SISTEMA} si lo propuso la aplicación; {@code CURADOR} si un curador
 *               lo agregó, lo quitó o lo volvió a usar
 * @param activo se usa al adaptar recetas; uno inactivo es uno que el curador quitó y la
 *               aplicación no vuelve a proponer
 */
public record ReemplazoResponse(
    Long reemplazoId,
    String nombre,
    String categoria,
    String origen,
    boolean activo) {
}
