package co.edu.uniquindio.cocinasinestres.avisos.repository;

import co.edu.uniquindio.cocinasinestres.avisos.model.Aviso;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AvisoRepository extends JpaRepository<Aviso, Long> {
    boolean existsByClaveIdempotencia(String claveIdempotencia);
    Optional<Aviso> findByClaveIdempotencia(String claveIdempotencia);
}
