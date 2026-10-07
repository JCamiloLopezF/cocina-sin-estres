package co.edu.uniquindio.cocinasinestres.perfil.domain;

/**
 * Roles de una cuenta (ADR-16). Se guardan como texto en {@code usuario.rol},
 * limitado por la restricción {@code ck_usuario_rol}.
 *
 * <p>Sin Spring ni JPA (ADR-09): el prefijo {@code ROLE_} que pide Spring Security
 * se construye aquí como una cadena cualquiera, para no atar el dominio al framework.</p>
 */
public enum Rol {

    /** Usuaria del hogar. Rol con el que se da de alta toda cuenta nueva. */
    USUARIA,

    /** Mantiene el catálogo de productos y recetas. */
    CURADOR,

    /** Gestiona cuentas y roles. */
    ADMIN;

    private static final String PREFIJO_AUTORIDAD = "ROLE_";

    /** Rol con el que entra una cuenta nueva (ADR-05). */
    public static final Rol PREDETERMINADO = USUARIA;

    /**
     * Nombre de la autoridad equivalente para la capa de seguridad, por ejemplo
     * {@code ROLE_USUARIA}.
     */
    public String autoridad() {
        return PREFIJO_AUTORIDAD + name();
    }

    /** Indica si el rol puede editar el catálogo y ver la auditoría. */
    public boolean puedeCurar() {
        return this == CURADOR || this == ADMIN;
    }

    /** Indica si el rol puede gestionar cuentas y asignar roles. */
    public boolean puedeAdministrar() {
        return this == ADMIN;
    }
}
