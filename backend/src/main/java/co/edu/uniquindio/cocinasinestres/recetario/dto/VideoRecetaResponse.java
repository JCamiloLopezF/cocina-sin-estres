package co.edu.uniquindio.cocinasinestres.recetario.dto;

/**
 * Video de YouTube que muestra cómo se hace la receta, y el fragmento que se ve. Dura
 * como máximo la mitad del tiempo de la receta (V10).
 *
 * @param youtubeId id del video (11 caracteres)
 * @param inicioSeg segundo en que empieza el fragmento
 * @param finSeg    segundo en que termina
 */
public record VideoRecetaResponse(String youtubeId, int inicioSeg, int finSeg) {
}
