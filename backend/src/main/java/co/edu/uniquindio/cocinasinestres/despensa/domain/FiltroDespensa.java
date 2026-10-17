package co.edu.uniquindio.cocinasinestres.despensa.domain;

import co.edu.uniquindio.cocinasinestres.comun.error.DatosInvalidosException;

import java.util.Locale;

/**
 * Pestañas del listado de la despensa (pantallas 14 y 20).
 */
public enum FiltroDespensa {

    /** Todos los productos. */
    TODO,

    /** Productos que vencen pronto o que ya vencieron. */
    POR_VENCER,

    /** Productos que llegaron a su nivel mínimo o están por debajo. */
    BAJO_MINIMO;

    /**
     * Interpreta el texto que llega en la consulta. Vacío o ausente equivale a
     * {@link #TODO}; no distingue mayúsculas.
     *
     * <p>Se interpreta a mano, y no con el conversor de Spring, para que un valor
     * desconocido responda 400 con un código estable y no un error inesperado.</p>
     *
     * @throws DatosInvalidosException si el texto no es ninguno de los filtros
     */
    public static FiltroDespensa desde(String texto) {
        if (texto == null || texto.isBlank()) {
            return TODO;
        }

        try {
            return valueOf(texto.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new DatosInvalidosException(
                CodigoDespensa.FILTRO_INVALIDO,
                "El filtro debe ser TODO, POR_VENCER o BAJO_MINIMO."
            );
        }
    }
}
