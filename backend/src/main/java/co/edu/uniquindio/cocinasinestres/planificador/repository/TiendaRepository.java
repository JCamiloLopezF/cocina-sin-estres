package co.edu.uniquindio.cocinasinestres.planificador.repository;

import co.edu.uniquindio.cocinasinestres.planificador.model.Tienda;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TiendaRepository extends JpaRepository<Tienda, Long> {

    List<Tienda> findByHogarIdOrderByNombreAsc(Long hogarId);

    Optional<Tienda> findByIdAndHogarId(Long id, Long hogarId);

    long countByHogarId(Long hogarId);

    boolean existsByHogarIdAndWhatsapp(Long hogarId, String whatsapp);
}
