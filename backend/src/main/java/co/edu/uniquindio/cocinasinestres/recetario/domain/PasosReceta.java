package co.edu.uniquindio.cocinasinestres.recetario.domain;

import java.util.List;
import java.util.Objects;

/**
 * Formato de los pasos de una receta.
 *
 * <p>La columna {@code receta.pasos} es un solo texto: un paso por línea y sin numerar
 * (la pantalla pone los números). Esta clase convierte entre ese texto y la lista de
 * pasos, para que ninguna otra capa tenga que saber cómo se guardan.</p>
 *
 * <p>Las líneas vacías y los espacios sobrantes se ignoran. No utiliza Spring ni JPA
 * porque pertenece al dominio.</p>
 */
public final class PasosReceta {

    /** Separador de pasos dentro del texto guardado. */
    public static final String SEPARADOR = "\n";

    private PasosReceta() {
    }

    /**
     * Convierte el texto guardado en la lista ordenada de pasos.
     *
     * @param texto contenido de {@code receta.pasos}; puede ser {@code null}
     * @return los pasos en orden; lista vacía si no hay ninguno
     */
    public static List<String> dividir(String texto) {
        if (texto == null || texto.isBlank()) {
            return List.of();
        }

        return texto.lines()
            .map(String::strip)
            .filter(paso -> !paso.isEmpty())
            .toList();
    }

    /**
     * Convierte una lista de pasos en el texto que se guarda en {@code receta.pasos}.
     *
     * @param pasos pasos en orden
     * @return un paso por línea; texto vacío si no hay ninguno
     */
    public static String unir(List<String> pasos) {
        Objects.requireNonNull(pasos, "pasos");

        return String.join(
            SEPARADOR,
            pasos.stream()
                .filter(Objects::nonNull)
                .map(String::strip)
                .filter(paso -> !paso.isEmpty())
                .toList()
        );
    }
}
