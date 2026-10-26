package co.edu.uniquindio.cocinasinestres.recetario.domain;

import java.util.List;
import java.util.Objects;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

/**
 * Formato de los pasos de una receta.
 *
 * <p>La columna {@code receta.pasos} es un solo texto: un paso por línea y sin numerar
 * (la pantalla pone los números). Esta clase convierte entre ese texto y la lista de
 * pasos, para que ninguna otra capa tenga que saber cómo se guardan.</p>
 *
 * <p>Las líneas vacías y los espacios sobrantes se ignoran. No utiliza Spring ni JPA
 * porque pertenece al dominio.</p>
 *
 * <p>Regla del Modo cocinar (ADR-25): cada paso dice <b>como máximo un tiempo</b>
 * ("cocina 20 minutos"), porque la pantalla lo ofrece como temporizador. Un paso con dos
 * tiempos ("sofríe 2 minutos y luego cocina 4 minutos") se parte en dos.</p>
 */
public final class PasosReceta {

    /** Separador de pasos dentro del texto guardado. */
    public static final String SEPARADOR = "\n";

    /**
     * Las mismas formas de decir un tiempo que reconoce el temporizador del Modo cocinar
     * ({@code minutosDelPaso} en {@code web-app/src/features/cocinar/guion.ts}).
     */
    private static final Pattern TIEMPO = Pattern.compile(
        "(?:\\d+\\s*(?:a|-|o)\\s*)?"
            + "(?:\\d+|un|uno|dos|tres|cuatro|cinco|seis|siete|ocho|nueve|diez|quince|veinte|treinta|cuarenta)"
            + "\\s*(?:minutos?|min\\b)"
            + "|(?:\\d+(?:[.,]\\d+)?|un|una|media|dos|tres)\\s+horas?",
        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
    );

    private PasosReceta() {
    }

    /**
     * Los tiempos que menciona un paso, en el orden en que aparecen.
     *
     * @param paso texto de un paso
     * @return por ejemplo {@code ["2 minutos", "4 minutos"]}; vacía si no dice ninguno
     */
    public static List<String> tiemposDelPaso(String paso) {
        if (paso == null) {
            return List.of();
        }

        return TIEMPO.matcher(paso).results()
            .map(MatchResult::group)
            .toList();
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
