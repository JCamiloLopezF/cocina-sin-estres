package co.edu.uniquindio.cocinasinestres.curaduria.dto;

/**
 * Video de una receta.
 *
 * @param enlace    enlace de YouTube como se copia del celular o del computador
 * @param inicioSeg segundo en que empieza la parte útil (0 si es desde el principio)
 * @param finSeg    segundo en que termina; el fragmento dura como máximo la mitad de la receta
 */
public record RecetaVideoRequest(String enlace, Integer inicioSeg, Integer finSeg) {
}
