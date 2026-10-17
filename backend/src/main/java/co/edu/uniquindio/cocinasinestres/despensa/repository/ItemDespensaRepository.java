package co.edu.uniquindio.cocinasinestres.despensa.repository;

import co.edu.uniquindio.cocinasinestres.despensa.model.ItemDespensa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Acceso a {@code item_despensa}. Toda consulta lleva el hogar: una usuaria nunca ve ni
 * toca la despensa de otra (RNF-22).
 */
public interface ItemDespensaRepository extends JpaRepository<ItemDespensa, Long> {

    List<ItemDespensa> findByHogarId(Long hogarId);

    Optional<ItemDespensa> findByIdAndHogarId(Long id, Long hogarId);

    boolean existsByHogarIdAndProductoId(Long hogarId, Long productoId);
}
