package co.edu.uniquindio.cocinasinestres.planificador.repository;

import co.edu.uniquindio.cocinasinestres.planificador.model.PlanSemanal;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanSemanalRepository extends JpaRepository<PlanSemanal, Long> {

    Optional<PlanSemanal> findByHogar_IdAndSemanaInicio(Long hogarId, LocalDate semanaInicio);
}
