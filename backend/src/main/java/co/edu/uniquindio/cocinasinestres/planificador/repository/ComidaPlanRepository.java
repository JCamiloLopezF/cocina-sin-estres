package co.edu.uniquindio.cocinasinestres.planificador.repository;

import co.edu.uniquindio.cocinasinestres.planificador.model.ComidaPlan;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComidaPlanRepository extends JpaRepository<ComidaPlan, Long> {

    Optional<ComidaPlan> findByPlanSemanal_IdAndDiaAndTipoComida(Long planId, Integer dia, String tipoComida);

    Optional<ComidaPlan> findByIdCambioCliente(String idCambioCliente);

    List<ComidaPlan> findByPlanSemanal_IdOrderByDiaAscTipoComidaAsc(Long planId);
}
