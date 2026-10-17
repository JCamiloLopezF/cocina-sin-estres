package co.edu.uniquindio.cocinasinestres.despensa;

import co.edu.uniquindio.cocinasinestres.comun.error.ExcepcionNegocio;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;

/**
 * Ayuda para las pruebas de despensa: obtener el {@code codigo} estable del error de
 * negocio que lanza una acción, que es lo que el frontend usa para decidir el mensaje.
 */
public final class ErroresDePrueba {

    private ErroresDePrueba() {
    }

    /**
     * Ejecuta la acción y devuelve el código del {@link ExcepcionNegocio} que lanza.
     * Falla si no lanza nada o si lanza otra clase de excepción.
     */
    public static String codigoDelError(ThrowingCallable accion) {
        try {
            accion.call();
        } catch (ExcepcionNegocio e) {
            return e.getCodigo();
        } catch (Throwable e) {
            throw new AssertionError("Se esperaba un error de negocio y llegó: " + e, e);
        }

        throw new AssertionError("Se esperaba un error de negocio y no se lanzó ninguno.");
    }
}
