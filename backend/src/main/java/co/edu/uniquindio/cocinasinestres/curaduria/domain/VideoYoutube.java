package co.edu.uniquindio.cocinasinestres.curaduria.domain;

import co.edu.uniquindio.cocinasinestres.comun.error.DatosInvalidosException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reglas del video de una receta (ajuste de la sustentación): un video de YouTube del que
 * se muestra un fragmento que dura como máximo la mitad del tiempo de la receta.
 *
 * <p>Acepta los enlaces que se copian del celular o del computador: {@code youtube.com/watch?v=…},
 * {@code youtu.be/…}, {@code youtube.com/shorts/…}, {@code /embed/…}, {@code /live/…} o el id
 * solo.</p>
 */
public final class VideoYoutube {

    private static final Pattern ID = Pattern.compile("^[A-Za-z0-9_-]{11}$");
    private static final Pattern ENLACE = Pattern.compile(
        "(?:youtube(?:-nocookie)?\\.com/(?:watch\\?(?:[^#]*&)?v=|shorts/|embed/|live/|v/)|youtu\\.be/)"
            + "([A-Za-z0-9_-]{11})(?![A-Za-z0-9_-])");

    private VideoYoutube() {
    }

    /**
     * @return el id del video
     * @throws DatosInvalidosException si no es un enlace de YouTube
     */
    public static String idDesde(String enlace) {
        String texto = enlace == null ? "" : enlace.trim();
        if (ID.matcher(texto).matches()) {
            return texto;
        }
        Matcher encontrado = ENLACE.matcher(texto);
        if (encontrado.find()) {
            return encontrado.group(1);
        }
        throw new DatosInvalidosException(
            "RECETA_VIDEO_ENLACE_INVALIDO", "Pega el enlace de un video de YouTube.");
    }

    /** Segundos que puede durar el fragmento: la mitad del tiempo de la receta. */
    public static int maximoSegundos(int tiempoMinutos) {
        return tiempoMinutos * 30;
    }

    /**
     * @throws DatosInvalidosException si el fragmento no tiene sentido o dura más de la mitad
     *         del tiempo de la receta
     */
    public static void validarFragmento(Integer inicioSeg, Integer finSeg, int tiempoMinutos) {
        if (inicioSeg == null || finSeg == null || inicioSeg < 0 || finSeg <= inicioSeg) {
            throw new DatosInvalidosException(
                "RECETA_VIDEO_FRAGMENTO_INVALIDO", "Indica desde qué segundo y hasta cuál se ve el video.");
        }
        int maximo = maximoSegundos(tiempoMinutos);
        if (finSeg - inicioSeg > maximo) {
            throw new DatosInvalidosException(
                "RECETA_VIDEO_MUY_LARGO",
                "El video puede durar como máximo la mitad de la receta: " + formato(maximo) + ".");
        }
    }

    /** 750 → "12:30". */
    public static String formato(int segundos) {
        return "%d:%02d".formatted(segundos / 60, segundos % 60);
    }
}
