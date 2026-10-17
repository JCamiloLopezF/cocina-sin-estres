package co.edu.uniquindio.cocinasinestres.comun.seguridad;

import co.edu.uniquindio.cocinasinestres.comun.error.NoAutenticadoException;

/**
 * Comprueba que un token de Firebase sea auténtico y vigente.
 *
 * <p>Está detrás de una interfaz para que el backend no dependa del SDK de Firebase
 * en todas partes y para poder doblarlo en las pruebas.</p>
 */
public interface VerificadorTokenFirebase {

    /**
     * @param idToken token que mandó el navegador en {@code Authorization: Bearer}
     * @return el {@code firebase_uid} y el correo que afirma el token
     * @throws NoAutenticadoException si el token es inválido, está vencido o fue revocado
     */
    Identidad verificar(String idToken);

    /**
     * @param firebaseUid identificador de la cuenta en Firebase
     * @param correo      correo verificado por Firebase
     */
    record Identidad(String firebaseUid, String correo) {
    }
}
