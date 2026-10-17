package co.edu.uniquindio.cocinasinestres.curaduria.domain;

/**
 * Entidades del catálogo que se auditan.
 *
 * <p>Deben coincidir con el CHECK {@code ck_auditoria_catalogo_entidad} de V1.
 * Este módulo solo audita {@link #PRODUCTO}.</p>
 */
public enum EntidadCatalogo {
    PRODUCTO,
    RECETA,
    INGREDIENTE_RECETA,
    RESTRICCION
}
