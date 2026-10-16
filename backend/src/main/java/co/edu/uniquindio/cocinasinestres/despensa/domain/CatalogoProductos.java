package co.edu.uniquindio.cocinasinestres.despensa.domain;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * Acceso de lectura al catálogo de productos, visto desde la despensa.
 *
 * <p>Es el único punto por el que la despensa llega a la tabla {@code producto}, que
 * pertenece a curaduría. Cuando el módulo recetario o curaduría publique un servicio de
 * consulta, basta con cambiar la implementación; nada más en despensa se entera.</p>
 */
public interface CatalogoProductos {

    /**
     * Busca un producto que el hogar puede agregar a su despensa: activo y del catálogo
     * global o registrado por ese mismo hogar.
     *
     * @return el producto, o vacío si no existe, está desactivado o es de otro hogar
     */
    Optional<ProductoCatalogo> buscarDisponible(Long productoId, Long hogarId);

    /**
     * Busca varios productos por identificador, sin filtrar por activo: un producto
     * desactivado en el catálogo sigue apareciendo en las despensas que ya lo tenían.
     *
     * @return los productos encontrados, indexados por su identificador
     */
    Map<Long, ProductoCatalogo> buscarPorIds(Collection<Long> productoIds);
}
