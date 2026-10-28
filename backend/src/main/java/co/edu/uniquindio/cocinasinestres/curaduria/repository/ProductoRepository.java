package co.edu.uniquindio.cocinasinestres.curaduria.repository;

import co.edu.uniquindio.cocinasinestres.curaduria.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    List<Producto> findByHogarIdIsNullAndActivoTrueOrderByCategoriaAscNombreAsc();

    Optional<Producto> findByIdAndHogarIdIsNull(Long id);

    /** Productos activos del catálogo global, para no repetir nombres (RNF-24). */
    List<Producto> findByHogarIdIsNullAndActivoTrue();

}
