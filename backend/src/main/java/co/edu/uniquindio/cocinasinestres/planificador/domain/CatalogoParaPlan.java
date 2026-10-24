package co.edu.uniquindio.cocinasinestres.planificador.domain;

import java.util.Map;

/**
 * Las recetas del catálogo como las necesita el generador del plan (ADR-24): costo,
 * ingredientes obligatorios y restricciones.
 *
 * <p>Puerto propio del planificador: no usa las entidades del recetario (RNF-26).</p>
 */
public interface CatalogoParaPlan {

    /** Todas las recetas, activas o no, por id. */
    Map<Long, RecetaCandidata> recetas();
}
