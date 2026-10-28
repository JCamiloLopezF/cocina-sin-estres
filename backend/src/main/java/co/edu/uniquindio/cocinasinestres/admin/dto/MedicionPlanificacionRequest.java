package co.edu.uniquindio.cocinasinestres.admin.dto;

import java.time.LocalDate;

/**
 * Cuánto tardó la usuaria en dejar la semana completa (KAN-54).
 *
 * @param semanaInicio lunes de la semana planeada
 * @param segundos     desde que abrió la semana sin completar hasta que quedó completa
 */
public record MedicionPlanificacionRequest(LocalDate semanaInicio, Integer segundos) {
}
