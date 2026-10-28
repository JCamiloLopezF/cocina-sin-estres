package co.edu.uniquindio.cocinasinestres.curaduria.domain;

import co.edu.uniquindio.cocinasinestres.comun.error.DatosInvalidosException;
import co.edu.uniquindio.cocinasinestres.recetario.domain.PasosReceta;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Reglas de una receta que el curador crea o edita (RNF-28). Reflejan lo que exige la base
 * (V1, V10, V17) para responder 400 con un mensaje claro y no un 500, y la regla del Modo
 * cocinar: cada paso dice como máximo un tiempo (V12, {@link PasosReceta}).
 *
 * <p>Sin Spring ni base de datos, para probarla de forma unitaria.</p>
 */
public final class ValidadorReceta {

    public static final int LARGO_NOMBRE = 120;
    public static final int LARGO_DESCRIPCION = 500;
    public static final int LARGO_PASO = 500;
    public static final int LARGO_PREPARACION = 300;
    public static final int MAXIMO_PASOS = 30;
    public static final int MAXIMO_INGREDIENTES = 30;
    public static final int MAXIMO_MINUTOS = 600;
    public static final int MAXIMO_PORCIONES = 20;
    private static final BigDecimal MAXIMA_CANTIDAD = new BigDecimal("100000");

    public static final List<String> MOMENTOS = List.of("DESAYUNO", "ALMUERZO", "CENA");

    /** Las mismas formas ambiguas que vigila PasosRecetasIntegracionTest. */
    private static final Pattern RANGO = Pattern.compile("\\d+ ?(-|a) ?\\d+ minutos", Pattern.CASE_INSENSITIVE);

    private ValidadorReceta() {
    }

    /** Nombre sin espacios de más; entre 3 y 120 caracteres. */
    public static String nombre(String nombre) {
        String limpio = CategoriaProducto.limpiarNombre(nombre);
        if (limpio == null || limpio.isEmpty()) {
            throw new DatosInvalidosException("RECETA_NOMBRE_REQUERIDO", "Escribe el nombre de la receta.");
        }
        if (limpio.length() < 3 || limpio.length() > LARGO_NOMBRE) {
            throw new DatosInvalidosException("RECETA_NOMBRE_LARGO",
                "El nombre debe tener entre 3 y " + LARGO_NOMBRE + " caracteres.");
        }
        return limpio;
    }

    /** Texto opcional: vacío es {@code null}; si viene, con un largo máximo. */
    public static String opcional(String texto, int maximo, String codigo, String campo) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        String limpio = texto.strip();
        if (limpio.length() > maximo) {
            throw new DatosInvalidosException(codigo, campo + " no puede superar " + maximo + " caracteres.");
        }
        return limpio;
    }

    public static int tiempo(Integer minutos) {
        if (minutos == null || minutos < 1 || minutos > MAXIMO_MINUTOS) {
            throw new DatosInvalidosException("RECETA_TIEMPO_INVALIDO",
                "El tiempo debe estar entre 1 y " + MAXIMO_MINUTOS + " minutos.");
        }
        return minutos;
    }

    public static int porciones(Integer porciones) {
        if (porciones == null || porciones < 1 || porciones > MAXIMO_PORCIONES) {
            throw new DatosInvalidosException("RECETA_PORCIONES_INVALIDAS",
                "Las porciones deben estar entre 1 y " + MAXIMO_PORCIONES + ".");
        }
        return porciones;
    }

    /**
     * Los pasos sin líneas vacías. Cada uno dice como máximo un tiempo, con un solo número,
     * y no remite a otra parte ("según el empaque").
     */
    public static List<String> pasos(List<String> pasos) {
        List<String> limpios = new ArrayList<>();
        if (pasos != null) {
            for (String paso : pasos) {
                // Un paso con saltos de línea se guardaría como varios: se juntan en uno.
                String limpio = paso == null ? "" : paso.strip().replaceAll("\\s*\\R\\s*", " ");
                if (!limpio.isEmpty()) {
                    limpios.add(limpio);
                }
            }
        }
        if (limpios.isEmpty()) {
            throw new DatosInvalidosException("RECETA_PASOS_REQUERIDOS", "Escribe al menos un paso.");
        }
        if (limpios.size() > MAXIMO_PASOS) {
            throw new DatosInvalidosException("RECETA_PASOS_MUCHOS", "Una receta puede tener hasta " + MAXIMO_PASOS + " pasos.");
        }
        for (int i = 0; i < limpios.size(); i++) {
            String paso = limpios.get(i);
            int numero = i + 1;
            if (paso.length() > LARGO_PASO) {
                throw new DatosInvalidosException("RECETA_PASO_LARGO",
                    "El paso " + numero + " es muy largo (máximo " + LARGO_PASO + " caracteres). Pártelo en dos.");
            }
            List<String> tiempos = PasosReceta.tiemposDelPaso(paso);
            if (tiempos.size() > 1) {
                throw new DatosInvalidosException("RECETA_PASO_DOS_TIEMPOS",
                    "El paso " + numero + " dice dos tiempos (" + String.join(" y ", tiempos)
                        + "). Pártelo en dos para que el temporizador del Modo cocinar sepa qué contar.");
            }
            String minusculas = paso.toLowerCase(Locale.ROOT);
            if (RANGO.matcher(paso).find() || minusculas.contains("según") || minusculas.contains("instrucciones")) {
                throw new DatosInvalidosException("RECETA_PASO_AMBIGUO",
                    "El paso " + numero + " no es concreto: di un solo tiempo (\"cocina 10 minutos\") "
                        + "y no remitas al empaque o a las instrucciones.");
            }
        }
        return limpios;
    }

    /** Las comidas en que va la receta, en el orden del día. Al menos una. */
    public static Set<String> momentos(List<String> momentos) {
        Set<String> validos = new LinkedHashSet<>();
        if (momentos != null) {
            for (String momento : momentos) {
                String codigo = momento == null ? "" : momento.trim().toUpperCase(Locale.ROOT);
                if (!MOMENTOS.contains(codigo)) {
                    throw new DatosInvalidosException("RECETA_MOMENTO_INVALIDO",
                        "Las comidas posibles son desayuno, almuerzo y cena.");
                }
                validos.add(codigo);
            }
        }
        if (validos.isEmpty()) {
            throw new DatosInvalidosException("RECETA_MOMENTO_REQUERIDO",
                "Marca en qué comidas va la receta: desayuno, almuerzo o cena.");
        }
        Set<String> ordenados = new LinkedHashSet<>();
        MOMENTOS.stream().filter(validos::contains).forEach(ordenados::add);
        return ordenados;
    }

    /** Cantidad de un ingrediente en la unidad base: mayor que cero, hasta 3 decimales. */
    public static BigDecimal cantidad(BigDecimal cantidad, String producto) {
        if (cantidad == null || cantidad.signum() <= 0 || cantidad.compareTo(MAXIMA_CANTIDAD) > 0
            || cantidad.stripTrailingZeros().scale() > 3) {
            throw new DatosInvalidosException("RECETA_CANTIDAD_INVALIDA",
                "La cantidad de " + producto + " debe ser mayor que cero, hasta 100.000 y con máximo 3 decimales.");
        }
        return cantidad;
    }

    /**
     * Un video ya cargado sigue durando como máximo la mitad del tiempo de la receta (V10).
     * Si el curador baja el tiempo, primero tiene que recortar el video.
     */
    public static void videoCabe(Integer inicioSeg, Integer finSeg, int minutos) {
        if (inicioSeg != null && finSeg != null && (finSeg - inicioSeg) * 2 > minutos * 60) {
            throw new DatosInvalidosException("RECETA_VIDEO_LARGO",
                "El video dura más de la mitad del tiempo nuevo. Recórtalo en la pantalla de videos o deja el tiempo como estaba.");
        }
    }
}
