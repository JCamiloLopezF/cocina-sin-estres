package co.edu.uniquindio.cocinasinestres.planificador.domain;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Alergia o intolerancia a un ingrediente concreto del catálogo, que no está en la lista
 * corta de alergias (lactosa, gluten, maní…). Auditoría del 4 de noviembre: «si la alergia
 * de alguien no está en la lista, el recetario no lo protege».
 *
 * <p>Se guarda con las demás restricciones de la persona, cifrada igual (ADR-20), con el
 * código {@code producto:<id>}. Cada producto tiene implícito su propio código, así que el
 * adaptador la trata como cualquier alergia: si es el ingrediente principal, plato aparte;
 * si no, se cambia o se sirve sin él; si es grave, la receta no se propone (ADR-24).</p>
 */
public final class AlergiaIngrediente {

    public static final String PREFIJO = "producto:";

    private AlergiaIngrediente() {
    }

    /** {@code producto:12}. */
    public static String codigo(Long productoId) {
        return PREFIJO + productoId;
    }

    /** El producto de un código {@code producto:<id>}; vacío si es otra restricción. */
    public static Optional<Long> producto(String codigo) {
        if (codigo == null || !codigo.startsWith(PREFIJO)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Long.parseLong(codigo.substring(PREFIJO.length())));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /** Los alérgenos del catálogo más el código del propio producto. */
    public static Set<String> conElPropio(Long productoId, Collection<String> alergenos) {
        Set<String> todos = new HashSet<>(alergenos);
        todos.add(codigo(productoId));
        return todos;
    }
}
