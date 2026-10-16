package co.edu.uniquindio.cocinasinestres.perfil.repository;

import co.edu.uniquindio.cocinasinestres.perfil.model.Persona;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso a los datos de las personas de cada hogar.
 */
public interface PersonaRepository extends JpaRepository<Persona, Long> {

    /**
     * Obtiene las personas de un hogar, ordenadas por identificador.
     */
    List<Persona> findByHogar_IdOrderByIdAsc(Long hogarId);

    /**
     * Elimina todas las personas asociadas a un hogar.
     */
    void deleteByHogar_Id(Long hogarId);

    /**
     * Cuenta cuántas personas están registradas en un hogar.
     */
    long countByHogar_Id(Long hogarId);
}
