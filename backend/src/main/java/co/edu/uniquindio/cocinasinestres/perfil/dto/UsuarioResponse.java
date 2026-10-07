package co.edu.uniquindio.cocinasinestres.perfil.dto;

/**
 * La cuenta de quien está usando la aplicación, tal como la ve el frontend.
 *
 * <p>Lleva solo lo que el navegador necesita para armar el menú y saber si falta
 * terminar el onboarding. Nunca el token ni datos de salud (RNF-21, RNF-22).</p>
 *
 * @param id             identificador de la cuenta
 * @param correo         correo con el que entró
 * @param rol            {@code USUARIA}, {@code CURADOR} o {@code ADMIN}
 * @param perfilCompleto {@code false} mientras falten datos obligatorios del hogar (RNF-01)
 */
public record UsuarioResponse(Long id, String correo, String rol, boolean perfilCompleto) {
}
