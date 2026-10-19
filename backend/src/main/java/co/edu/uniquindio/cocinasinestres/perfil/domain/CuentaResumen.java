package co.edu.uniquindio.cocinasinestres.perfil.domain;

import java.time.Instant;

/**
 * Lo que el administrador ve de una cuenta: sin datos del hogar ni de salud (RNF-22).
 *
 * @param id             identificador de la cuenta
 * @param correo         correo con el que entra
 * @param rol            rol actual
 * @param activo         {@code false} si la cuenta está desactivada y no puede entrar
 * @param perfilCompleto si ya completó el perfil del hogar
 * @param creadoEn       cuándo entró por primera vez
 */
public record CuentaResumen(Long id, String correo, Rol rol, boolean activo, boolean perfilCompleto,
        Instant creadoEn) {
}
