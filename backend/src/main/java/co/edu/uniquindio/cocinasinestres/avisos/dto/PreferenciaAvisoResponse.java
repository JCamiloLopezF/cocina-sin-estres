package co.edu.uniquindio.cocinasinestres.avisos.dto;

/**
 * Un tipo de aviso y si la usuaria lo quiere recibir.
 *
 * @param titulo      nombre del tipo en la pantalla
 * @param descripcion cuándo llega, en una frase
 */
public record PreferenciaAvisoResponse(String tipo, String titulo, String descripcion, boolean activo) {
}
