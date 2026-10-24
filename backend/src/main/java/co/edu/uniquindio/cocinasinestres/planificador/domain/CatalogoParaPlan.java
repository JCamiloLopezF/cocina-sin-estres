package co.edu.uniquindio.cocinasinestres.planificador.domain;

import java.util.Map;
import java.util.Set;

/**
 * Las recetas del catálogo como las necesita el generador del plan (ADR-24): costo,
 * ingredientes obligatorios y restricciones.
 *
 * <p>Puerto propio del planificador: no usa las entidades del recetario (RNF-26).</p>
 */
public interface CatalogoParaPlan {

    /** Todas las recetas, activas o no, por id. */
    Map<Long, RecetaCandidata> recetas();

    /** Alérgenos (códigos de tipo ALERGIA) de cada producto que tiene alguno. */
    default Map<Long, Set<String>> alergenos() {
        return Map.of();
    }

    /** Reemplazos activos de cada producto (tabla {@code reemplazo_producto}). */
    default Map<Long, Set<Long>> reemplazos() {
        return Map.of();
    }

    /** Nombre de cada producto, para los avisos ("cambia el huevo por pollo"). */
    default Map<Long, String> nombresProducto() {
        return Map.of();
    }

    /** Nombre de cada receta, para proponer un plato aparte. */
    default Map<Long, String> nombresReceta() {
        return Map.of();
    }
}
