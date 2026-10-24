package co.edu.uniquindio.cocinasinestres.planificador.domain;

import java.math.BigDecimal;
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

    /** Porciones de cada receta, para comprar una sola porción de un reemplazo o un plato aparte. */
    default Map<Long, Integer> porciones() {
        return Map.of();
    }

    /**
     * Unidad base y presentación de compra de cada producto del catálogo.
     *
     * @param unidadBase   g, ml o unidad
     * @param presentacion cuánto trae la unidad de compra, en unidad base
     */
    record ProductoPlan(String unidadBase, BigDecimal presentacion) {
    }

    default Map<Long, ProductoPlan> productos() {
        return Map.of();
    }

    /** Nombre de cada receta, para proponer un plato aparte. */
    default Map<Long, String> nombresReceta() {
        return Map.of();
    }
}
