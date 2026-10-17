package co.edu.uniquindio.cocinasinestres.recetario.repository;

import co.edu.uniquindio.cocinasinestres.recetario.model.Receta;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Acceso a las recetas. Solo el módulo {@code recetario} lo usa: los demás módulos
 * piden las recetas a su servicio.
 */
public interface RecetaRepository extends JpaRepository<Receta, Long> {

    /** Recetas activas por orden alfabético, sin cargar los ingredientes. */
    List<Receta> findByActivaTrueOrderByNombreAsc();

    /** Recetas activas por orden alfabético, con sus ingredientes en la misma consulta. */
    @EntityGraph(attributePaths = "ingredientes")
    List<Receta> findConIngredientesByActivaTrueOrderByNombreAsc();
}
