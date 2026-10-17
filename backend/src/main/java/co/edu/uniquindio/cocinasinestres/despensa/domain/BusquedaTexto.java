package co.edu.uniquindio.cocinasinestres.despensa.domain;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Coincidencia de texto para el buscador de la despensa: no distingue mayúsculas ni
 * tildes, porque quien escribe "frijol" desde un celular espera encontrar "Fríjol".
 */
public final class BusquedaTexto {

    private BusquedaTexto() {
    }

    /**
     * @param texto    texto donde se busca (ej.: el nombre del producto)
     * @param consulta lo que escribió la usuaria; vacío o ausente coincide con todo
     */
    public static boolean coincide(String texto, String consulta) {
        if (consulta == null || consulta.isBlank()) {
            return true;
        }

        if (texto == null) {
            return false;
        }

        return normalizar(texto).contains(normalizar(consulta));
    }

    static String normalizar(String texto) {
        return Normalizer.normalize(texto.trim(), Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .toLowerCase(Locale.ROOT);
    }
}
