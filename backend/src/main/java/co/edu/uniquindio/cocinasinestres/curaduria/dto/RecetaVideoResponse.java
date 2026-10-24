package co.edu.uniquindio.cocinasinestres.curaduria.dto;

/**
 * Una receta del catálogo con su video, como la ve el curador.
 *
 * @param maximoSegundos lo más que puede durar el fragmento: la mitad del tiempo de la receta
 * @param youtubeId      {@code null} si todavía no tiene video
 */
public record RecetaVideoResponse(
    Long id,
    String nombre,
    int tiempoMinutos,
    int maximoSegundos,
    String youtubeId,
    Integer inicioSeg,
    Integer finSeg) {
}
