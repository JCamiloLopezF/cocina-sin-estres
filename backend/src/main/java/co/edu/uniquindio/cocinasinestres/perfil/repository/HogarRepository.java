package co.edu.uniquindio.cocinasinestres.perfil.repository;

import co.edu.uniquindio.cocinasinestres.perfil.model.Hogar;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso a los datos de los hogares.
 */
public interface HogarRepository extends JpaRepository<Hogar, Long> {

    /**
     * Busca el hogar asociado al identificador de un usuario.
     */
    Optional<Hogar> findByUsuario_Id(Long usuarioId);
}
