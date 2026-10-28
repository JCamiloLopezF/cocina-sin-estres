package co.edu.uniquindio.cocinasinestres.curaduria.domain;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

/**
 * Categorías del catálogo de productos (V17, RNF-24). La base las restringe con
 * {@code ck_producto_categoria}; aquí se validan antes para responder 400 y no 500.
 *
 * <p>OTROS es para lo que una usuaria registra en su despensa y no encaja en las demás.
 * También define cómo se compara un nombre para no repetir productos: sin tildes, sin
 * mayúsculas y sin espacios de más ("  ARROZ " es lo mismo que "Arroz").</p>
 */
public final class CategoriaProducto {

    /** En el orden en que se muestran. */
    public static final List<String> CODIGOS = List.of(
        "GRANOS", "LEGUMBRES", "TUBERCULOS", "HARINAS", "PROTEINAS", "LACTEOS",
        "VERDURAS", "FRUTAS", "ACEITES", "CONDIMENTOS", "BEBIDAS", "OTROS");

    private CategoriaProducto() {
    }

    /**
     * La categoría en mayúsculas si es de la lista ("granos" → GRANOS); {@code null} si no.
     */
    public static String normalizar(String categoria) {
        if (categoria == null) {
            return null;
        }
        String codigo = sinTildes(categoria.trim()).toUpperCase(Locale.ROOT);
        return CODIGOS.contains(codigo) ? codigo : null;
    }

    /** El nombre como se guarda: sin espacios al inicio, al final ni repetidos. */
    public static String limpiarNombre(String nombre) {
        return nombre == null ? null : nombre.trim().replaceAll("\\s+", " ");
    }

    /** Clave para comparar nombres: "  ARROZ " y "arroz" tienen la misma. */
    public static String claveNombre(String nombre) {
        return nombre == null ? "" : sinTildes(limpiarNombre(nombre)).toLowerCase(Locale.ROOT);
    }

    private static String sinTildes(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }
}
