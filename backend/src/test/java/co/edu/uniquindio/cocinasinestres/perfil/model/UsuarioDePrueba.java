package co.edu.uniquindio.cocinasinestres.perfil.model;

import co.edu.uniquindio.cocinasinestres.perfil.domain.Rol;
import java.lang.reflect.Field;

/**
 * Construye cuentas con un {@code id} ya puesto, para las pruebas que no tocan la base.
 *
 * <p>En producción el {@code id} lo asigna MySQL y lo lee Hibernate, así que la entidad
 * no tiene cómo recibirlo desde el código. Aquí se pone por reflexión para no abrir un
 * setter que nadie debería usar.</p>
 */
public final class UsuarioDePrueba {

    private UsuarioDePrueba() {
    }

    /** Cuenta activa, con el perfil sin terminar, como queda recién creada. */
    public static Usuario conId(long id, String firebaseUid, String correo, Rol rol) {
        Usuario usuario = Usuario.nueva(firebaseUid, correo, rol);
        asignarId(usuario, id);
        return usuario;
    }

    /** Cuenta desactivada por un administrador. */
    public static Usuario inactiva(long id, String firebaseUid, String correo) {
        Usuario usuario = conId(id, firebaseUid, correo, Rol.USUARIA);
        usuario.setActivo(false);
        return usuario;
    }

    private static void asignarId(Usuario usuario, long id) {
        try {
            Field campo = Usuario.class.getDeclaredField("id");
            campo.setAccessible(true);
            campo.set(usuario, id);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("No se pudo asignar el id de prueba", error);
        }
    }
}
