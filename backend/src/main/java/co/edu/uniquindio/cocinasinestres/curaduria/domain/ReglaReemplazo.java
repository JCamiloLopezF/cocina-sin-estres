package co.edu.uniquindio.cocinasinestres.curaduria.domain;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Regla con la que la aplicación propone reemplazos por su cuenta (ADR-24): un
 * producto con alérgenos se puede cambiar por otro de la misma categoría que no
 * tenga ninguno de esos alérgenos. Huevos (huevo) → pollo, carne, atún; atún
 * (pescado) → huevos, pollo, carne.
 *
 * <p>Sin Spring ni base de datos, para probarla de forma unitaria.</p>
 */
public final class ReglaReemplazo {

    /** Un producto del catálogo como lo ve la regla. */
    public record ProductoCatalogo(Long id, String categoria, Set<String> alergenos) {
        public ProductoCatalogo {
            alergenos = Set.copyOf(alergenos);
        }
    }

    /** Un par (producto, reemplazo). */
    public record Par(Long productoId, Long reemplazoId) {
    }

    private ReglaReemplazo() {
    }

    /** Los pares que la aplicación propone para el catálogo dado. */
    public static Set<Par> proponer(Collection<ProductoCatalogo> catalogo) {
        Set<Par> pares = new HashSet<>();
        for (ProductoCatalogo producto : catalogo) {
            if (producto.alergenos().isEmpty()) {
                continue;
            }
            for (ProductoCatalogo otro : catalogo) {
                if (!otro.id().equals(producto.id())
                    && Objects.equals(otro.categoria(), producto.categoria())
                    && Collections.disjoint(otro.alergenos(), producto.alergenos())) {
                    pares.add(new Par(producto.id(), otro.id()));
                }
            }
        }
        return pares;
    }

    /** Arma el catálogo a partir de categorías y alérgenos por producto. */
    public static Set<ProductoCatalogo> catalogo(Map<Long, String> categorias, Map<Long, Set<String>> alergenos) {
        Set<ProductoCatalogo> productos = new HashSet<>();
        categorias.forEach((id, categoria) ->
            productos.add(new ProductoCatalogo(id, categoria, alergenos.getOrDefault(id, Set.of()))));
        return productos;
    }
}
