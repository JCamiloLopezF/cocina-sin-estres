package co.edu.uniquindio.cocinasinestres.planificador.repository;

import co.edu.uniquindio.cocinasinestres.planificador.model.ListaCompras;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ListaComprasRepository extends JpaRepository<ListaCompras, Long> {

    Optional<ListaCompras> findByPlanSemanal_Id(Long planSemanalId);
}
