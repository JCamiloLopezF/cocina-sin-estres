package co.edu.uniquindio.cocinasinestres.perfil.model;

import co.edu.uniquindio.cocinasinestres.perfil.domain.Rol;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.generator.EventType;
import org.hibernate.type.SqlTypes;

/**
 * Cuenta de una persona que usa la aplicación.
 *
 * <p>No hay registro propio: la fila se crea la primera vez que llega un token de
 * Firebase válido con un {@code firebase_uid} desconocido (alta perezosa, ADR-05).
 * La contraseña y el token viven en Firebase, nunca aquí.</p>
 *
 * <p>Mapea la tabla {@code usuario} de {@code V1__esquema_inicial.sql}. Con
 * {@code ddl-auto=validate}, cualquier diferencia con esa tabla rompe el arranque.</p>
 */
@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "firebase_uid", nullable = false, length = 128, updatable = false)
    private String firebaseUid;

    @Column(name = "correo", nullable = false, length = 254)
    private String correo;

    // La columna es VARCHAR + CHECK, no un ENUM de MySQL (docs/modelo-datos.md).
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "rol", nullable = false, length = 10)
    private Rol rol;

    @Column(name = "perfil_completo", nullable = false)
    private boolean perfilCompleto;

    @Column(name = "activo", nullable = false)
    private boolean activo;

    // Las dos marcas de tiempo las pone MySQL; Hibernate solo las lee.
    @Generated(event = EventType.INSERT)
    @Column(name = "creado_en")
    private Instant creadoEn;

    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @Column(name = "actualizado_en")
    private Instant actualizadoEn;

    protected Usuario() {
        // Requerido por JPA.
    }

    private Usuario(String firebaseUid, String correo, Rol rol) {
        this.firebaseUid = firebaseUid;
        this.correo = correo;
        this.rol = rol;
        this.perfilCompleto = false;
        this.activo = true;
    }

    /**
     * Crea la cuenta de quien entra por primera vez. Queda activa y con el perfil
     * sin terminar: el onboarding lo completa después el módulo {@code perfil}.
     *
     * @param firebaseUid identificador de la cuenta en Firebase
     * @param correo      correo verificado por Firebase
     * @param rol         rol inicial, normalmente {@link Rol#PREDETERMINADO}
     */
    public static Usuario nueva(String firebaseUid, String correo, Rol rol) {
        return new Usuario(
                Objects.requireNonNull(firebaseUid, "firebaseUid"),
                Objects.requireNonNull(correo, "correo"),
                Objects.requireNonNull(rol, "rol"));
    }

    public Long getId() {
        return id;
    }

    public String getFirebaseUid() {
        return firebaseUid;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public Rol getRol() {
        return rol;
    }

    public void setRol(Rol rol) {
        this.rol = rol;
    }

    public boolean isPerfilCompleto() {
        return perfilCompleto;
    }

    public void setPerfilCompleto(boolean perfilCompleto) {
        this.perfilCompleto = perfilCompleto;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }

    public Instant getActualizadoEn() {
        return actualizadoEn;
    }

    @Override
    public boolean equals(Object otro) {
        if (this == otro) {
            return true;
        }
        if (!(otro instanceof Usuario usuario)) {
            return false;
        }
        return id != null && id.equals(usuario.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    /** Sin el correo: no se escriben datos personales en los logs (RNF-21). */
    @Override
    public String toString() {
        return "Usuario[id=" + id + ", rol=" + rol + ", activo=" + activo + "]";
    }
}
