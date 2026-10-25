package co.edu.uniquindio.cocinasinestres.planificador.dto;

import java.util.List;

/**
 * Cómo adaptar una receta a las alergias de cada persona del hogar (ADR-24).
 *
 * @param estado  {@code APTA}, {@code ADAPTADA}, {@code PLATO_APARTE} o {@code NO_APTA}
 * @param cambios lo que hay que hacer, persona por persona
 */
public record AdaptacionResponse(Long recetaId, String estado, List<Cambio> cambios) {

    /**
     * @param tipo           {@code REEMPLAZO}, {@code SIN}, {@code PLATO_APARTE} o {@code NO_APTA}
     * @param persona        a quién ("Samuel", "ti")
     * @param alergias       códigos de las alergias que lo motivan
     * @param producto       ingrediente que se cambia o se quita
     * @param reemplazo      por cuál se cambia
     * @param enDespensa     el reemplazo ya está en la despensa
     * @param recetaAparteId receta que se le propone aparte
     * @param recetaAparte   su nombre
     */
    public record Cambio(
        String tipo,
        String persona,
        List<String> alergias,
        Long productoId,
        String producto,
        Long reemplazoId,
        String reemplazo,
        boolean enDespensa,
        Long recetaAparteId,
        String recetaAparte) {
    }
}
