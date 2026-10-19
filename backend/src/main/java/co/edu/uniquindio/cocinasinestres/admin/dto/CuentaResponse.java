package co.edu.uniquindio.cocinasinestres.admin.dto;

import co.edu.uniquindio.cocinasinestres.perfil.domain.CuentaResumen;
import java.time.Instant;

/**
 * Una cuenta en la pantalla de administración. Sin datos del hogar ni de salud (RNF-22).
 *
 * @param id             identificador de la cuenta
 * @param correo         correo con el que entra
 * @param rol            {@code USUARIA}, {@code CURADOR} o {@code ADMIN}
 * @param activo         si puede entrar
 * @param perfilCompleto si ya completó el perfil del hogar
 * @param creadoEn       primer ingreso
 */
public record CuentaResponse(Long id, String correo, String rol, boolean activo, boolean perfilCompleto,
        Instant creadoEn) {

    public static CuentaResponse de(CuentaResumen cuenta) {
        return new CuentaResponse(cuenta.id(), cuenta.correo(), cuenta.rol().name(), cuenta.activo(),
                cuenta.perfilCompleto(), cuenta.creadoEn());
    }
}
