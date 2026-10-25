package co.edu.uniquindio.cocinasinestres.planificador.domain;

import java.util.List;
import java.util.Set;

/**
 * Cómo queda una receta frente a las alergias del hogar (ADR-24).
 *
 * @param estado  de mejor a peor: {@code APTA} (todos la comen igual), {@code ADAPTADA}
 *                (a alguien se le cambia o se le quita un ingrediente), {@code PLATO_APARTE}
 *                (alguien no puede con el ingrediente principal y come otra cosa) o
 *                {@code NO_APTA} (alguien tiene una alergia grave a un ingrediente)
 * @param cambios lo que hay que hacer, persona por persona
 */
public record AdaptacionReceta(Estado estado, List<Cambio> cambios) {

    public enum Estado { APTA, ADAPTADA, PLATO_APARTE, NO_APTA }

    public enum TipoCambio {
        /** Cambiar el ingrediente por otro en su porción. */
        REEMPLAZO,
        /** Servir su porción sin ese ingrediente (no hay reemplazo y no es el principal). */
        SIN,
        /** Prepararle otra receta. */
        PLATO_APARTE,
        /** No se puede: alergia grave. */
        NO_APTA
    }

    /**
     * @param persona       a quién
     * @param alergias      las alergias que motivan el cambio
     * @param productoId    ingrediente que se cambia o se quita (en REEMPLAZO y SIN)
     * @param reemplazoId   por cuál (en REEMPLAZO)
     * @param enDespensa    el reemplazo ya está en la despensa
     * @param recetaAparteId la receta que se le propone aparte (puede faltar si ninguna le sirve)
     */
    public record Cambio(
        TipoCambio tipo,
        String persona,
        Set<String> alergias,
        Long productoId,
        Long reemplazoId,
        boolean enDespensa,
        Long recetaAparteId) {

        public Cambio {
            alergias = Set.copyOf(alergias);
        }
    }

    public static final AdaptacionReceta APTA = new AdaptacionReceta(Estado.APTA, List.of());

    public AdaptacionReceta {
        cambios = List.copyOf(cambios);
    }

    /** Cuántos cambios de un tipo trae. */
    public long contar(TipoCambio tipo) {
        return cambios.stream().filter(c -> c.tipo() == tipo).count();
    }
}
