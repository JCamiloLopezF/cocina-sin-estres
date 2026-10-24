package co.edu.uniquindio.cocinasinestres.avisos.domain;

import java.time.LocalDate;

/**
 * Un producto de la despensa que motiva un aviso.
 *
 * @param venceEn fecha de vencimiento, o {@code null} si no aplica (bajo el mínimo)
 */
public record ProductoAviso(Long productoId, String nombre, LocalDate venceEn) {
}
