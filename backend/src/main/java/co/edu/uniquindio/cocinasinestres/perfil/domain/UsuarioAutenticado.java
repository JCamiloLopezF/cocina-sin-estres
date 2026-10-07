package co.edu.uniquindio.cocinasinestres.perfil.domain;

import java.util.Objects;

/**
 * Datos de la usuaria que hace la petición, ya resueltos contra la base.
 *
 * <p>Es lo único que la capa de seguridad publica para el resto de la aplicación:
 * no lleva el token ni nada que no deba quedar en memoria más de lo necesario.
 * Sin Spring ni JPA (ADR-09).</p>
 *
 * @param id             identificador de la fila en {@code usuario}
 * @param firebaseUid    identificador de la cuenta en Firebase
 * @param correo         correo de la cuenta
 * @param rol            rol vigente (ADR-16)
 * @param perfilCompleto {@code false} mientras falten datos obligatorios del hogar (RNF-01);
 *                       los módulos lo consultan para no operar sobre un perfil a medias
 */
public record UsuarioAutenticado(Long id, String firebaseUid, String correo, Rol rol,
                                 boolean perfilCompleto) {

    public UsuarioAutenticado {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(firebaseUid, "firebaseUid");
        Objects.requireNonNull(correo, "correo");
        Objects.requireNonNull(rol, "rol");
    }

    /** Sin el correo: no se escriben datos personales en los logs (RNF-21). */
    @Override
    public String toString() {
        return "UsuarioAutenticado[id=" + id + ", rol=" + rol + "]";
    }
}
