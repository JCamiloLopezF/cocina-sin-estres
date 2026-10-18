package co.edu.uniquindio.cocinasinestres.recetario.repository;

import co.edu.uniquindio.cocinasinestres.recetario.model.IngredienteReceta;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repositorio para ingredientes de receta.
 */
public interface IngredienteRecetaRepository extends JpaRepository<IngredienteReceta, Long> {

    /**
     * Obtiene los ingredientes de una receta.
     * Nota: el nombre sigue la convención del proyecto (campo receta).
     */
    List<IngredienteReceta> findByReceta_Id(Long recetaId);
}
