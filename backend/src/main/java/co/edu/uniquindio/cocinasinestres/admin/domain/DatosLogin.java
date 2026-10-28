package co.edu.uniquindio.cocinasinestres.admin.domain;

/**
 * Inicios de sesión del periodo, como los anota la web (V16).
 *
 * @param exitosos     inicios de sesión que terminaron bien
 * @param fallidos     intentos fallidos que hubo antes de esos inicios, en el mismo navegador
 * @param msPromedio   milisegundos promedio desde «Entrar» hasta que la API reconoce la cuenta;
 *                     {@code null} sin inicios
 */
public record DatosLogin(long exitosos, long fallidos, Double msPromedio) {

    /** Sin inicios de sesión medidos. */
    public static final DatosLogin SIN_DATOS = new DatosLogin(0, 0, null);
}
