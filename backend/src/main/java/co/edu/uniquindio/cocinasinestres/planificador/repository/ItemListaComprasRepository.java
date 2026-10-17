package co.edu.uniquindio.cocinasinestres.planificador.repository;

import co.edu.uniquindio.cocinasinestres.planificador.model.ItemListaCompras;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemListaComprasRepository extends JpaRepository<ItemListaCompras, Long> {

    List<ItemListaCompras> findByListaCompras_Id(Long listaComprasId);

    void deleteByListaCompras_Id(Long listaComprasId);
}
