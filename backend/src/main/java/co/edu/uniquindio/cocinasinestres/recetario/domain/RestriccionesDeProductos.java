package co.edu.uniquindio.cocinasinestres.recetario.domain;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Qué alergias y condiciones tiene cada producto del catálogo (tabla
 * {@code producto_restriccion}, V6). El recetario la usa para decir qué restricciones
 * tiene una receta: las de sus ingredientes.
 */
public interface RestriccionesDeProductos {

    /**
     * @return los códigos de restricción de cada producto (por ejemplo {@code lactosa});
     *         los productos sin restricciones no aparecen en el mapa
     */
    Map<Long, List<String>> codigosPorProducto(Collection<Long> productoIds);
}
