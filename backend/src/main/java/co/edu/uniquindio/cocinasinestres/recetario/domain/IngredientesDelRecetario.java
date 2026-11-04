package co.edu.uniquindio.cocinasinestres.recetario.domain;

import java.util.List;

/**
 * Los ingredientes que usan las recetas activas, para que el perfil deje marcar una
 * alergia a uno que no está en la lista corta (auditoría del 4 de noviembre). Solo los
 * del catálogo general: los productos propios de un hogar no van en recetas.
 */
public interface IngredientesDelRecetario {

    /** Ingrediente con su categoría, ordenado por nombre. */
    record Ingrediente(Long id, String nombre, String categoria) {
    }

    List<Ingrediente> listar();
}
