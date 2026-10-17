/**
 * Autenticación y autorización.
 *
 * <p>En producción se verifica el token de Firebase; en el perfil {@code dev} un filtro
 * alternativo acepta el encabezado {@code X-Dev-User} (ADR-19). La autorización por rol
 * es siempre del backend (ADR-16).</p>
 */
package co.edu.uniquindio.cocinasinestres.comun.seguridad;
