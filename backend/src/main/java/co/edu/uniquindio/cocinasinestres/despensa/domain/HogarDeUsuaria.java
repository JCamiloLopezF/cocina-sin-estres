package co.edu.uniquindio.cocinasinestres.despensa.domain;

import java.util.Optional;

/**
 * Resuelve cuál es el hogar de una usuaria.
 *
 * <p>Toda la despensa se consulta por hogar (RNF-22). Mientras el módulo perfil no
 * publique un servicio con esta consulta, la implementación lee la tabla {@code hogar};
 * al publicarlo, solo cambia esa implementación.</p>
 */
public interface HogarDeUsuaria {

    /**
     * @param usuarioId identificador de la fila en {@code usuario}
     * @return el identificador del hogar, o vacío si la usuaria aún no lo ha creado
     */
    Optional<Long> hogarIdDe(Long usuarioId);
}
