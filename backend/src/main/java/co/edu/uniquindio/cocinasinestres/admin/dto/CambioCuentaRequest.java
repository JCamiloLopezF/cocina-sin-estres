package co.edu.uniquindio.cocinasinestres.admin.dto;

/**
 * Cambio que pide el administrador. Lo que no venga se deja igual.
 *
 * @param rol    nuevo rol: {@code USUARIA}, {@code CURADOR} o {@code ADMIN}
 * @param activo {@code false} para desactivar la cuenta, {@code true} para reactivarla
 */
public record CambioCuentaRequest(String rol, Boolean activo) {
}
