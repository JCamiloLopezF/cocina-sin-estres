package co.edu.uniquindio.cocinasinestres.avisos.domain;

import java.util.Objects;

/**
 * Un aviso listo para guardar y mostrar.
 *
 * @param tipo    uno de los cinco de ADR-17
 * @param clave   clave de idempotencia: el mismo aviso no se repite en el día (o en la semana)
 * @param titulo  lo primero que se lee, corto y en tono de apoyo (RNF-14)
 * @param mensaje el detalle, máximo 255 caracteres
 * @param enlace  ruta de la aplicación a la que lleva el botón
 * @param accion  texto del botón
 */
public record Aviso(TipoAviso tipo, String clave, String titulo, String mensaje, String enlace, String accion) {

    /** Lo que cabe en la columna {@code aviso.mensaje}. */
    public static final int MAX_MENSAJE = 255;

    public Aviso {
        Objects.requireNonNull(tipo, "tipo");
        Objects.requireNonNull(clave, "clave");
        mensaje = recortar(Objects.requireNonNull(mensaje, "mensaje"), MAX_MENSAJE);
        titulo = recortar(Objects.requireNonNull(titulo, "titulo"), 120);
    }

    private static String recortar(String texto, int maximo) {
        return texto.length() <= maximo ? texto : texto.substring(0, maximo - 1).stripTrailing() + "…";
    }
}
