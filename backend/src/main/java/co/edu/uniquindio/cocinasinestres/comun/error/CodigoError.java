package co.edu.uniquindio.cocinasinestres.comun.error;

/**
 * Códigos de error estables que viajan en el campo {@code codigo} de la respuesta.
 *
 * <p>El frontend decide el mensaje que ve la usuaria a partir del código, no del
 * texto de {@code detail}. Por eso un código nunca se renombra: si cambia el
 * significado, se agrega uno nuevo. Formato {@code MODULO_MOTIVO} en mayúsculas
 * (CONTRIBUTING.md §7).</p>
 *
 * <p>Cada módulo declara sus propios códigos; aquí viven solo los transversales.</p>
 */
public final class CodigoError {

    /** No llegó token, o el token no es válido o está vencido. */
    public static final String AUTH_TOKEN_INVALIDO = "AUTH_TOKEN_INVALIDO";

    /** El token es válido pero el rol no alcanza para esa ruta. */
    public static final String AUTH_SIN_PERMISO = "AUTH_SIN_PERMISO";

    /** La cuenta existe pero está desactivada. */
    public static final String AUTH_CUENTA_INACTIVA = "AUTH_CUENTA_INACTIVA";

    /** Falló la validación de los datos que llegaron en la petición. */
    public static final String VALIDACION_DATOS = "VALIDACION_DATOS";

    /** El cuerpo de la petición no se pudo leer (JSON mal formado, tipo equivocado). */
    public static final String PETICION_MAL_FORMADA = "PETICION_MAL_FORMADA";

    /** El método HTTP o el tipo de contenido no aplican a esa ruta. */
    public static final String PETICION_NO_SOPORTADA = "PETICION_NO_SOPORTADA";

    /** No existe, o es de otro hogar (RNF-22: no se confirma que exista). */
    public static final String RECURSO_NO_ENCONTRADO = "RECURSO_NO_ENCONTRADO";

    /** Error no previsto. El detalle útil queda en el log, no en la respuesta. */
    public static final String ERROR_INTERNO = "ERROR_INTERNO";

    private CodigoError() {
    }
}
