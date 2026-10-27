package co.edu.uniquindio.cocinasinestres.curaduria.dto;

/**
 * Una receta del catálogo con su video y sus etiquetas, como la ve el curador.
 *
 * @param maximoSegundos lo más que puede durar el fragmento: la mitad del tiempo de la receta
 * @param youtubeId      {@code null} si todavía no tiene video
 * @param platoFuerte    sirve como almuerzo o cena: "Armar mi semana" solo usa estas (V7)
 * @param gustaANinos    en general les gusta a los niños: se prefiere si en el hogar hay niños (V7)
 */
public record RecetaVideoResponse(
    Long id,
    String nombre,
    int tiempoMinutos,
    int maximoSegundos,
    String youtubeId,
    Integer inicioSeg,
    Integer finSeg,
    boolean platoFuerte,
    boolean gustaANinos) {
}
