package co.edu.uniquindio.cocinasinestres.curaduria.domain;

/**
 * Acciones que se registran en la auditoría del catálogo.
 *
 * <p>Deben coincidir con el CHECK {@code ck_auditoria_catalogo_accion} de V1.</p>
 */
public enum AccionAuditoria {
    CREAR,
    EDITAR,
    DESACTIVAR
}
