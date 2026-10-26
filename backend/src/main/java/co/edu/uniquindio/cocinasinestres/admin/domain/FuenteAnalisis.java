package co.edu.uniquindio.cocinasinestres.admin.domain;

import java.time.LocalDate;

/**
 * Puerto de lectura del análisis del administrador (KAN-54). Solo consultas agregadas:
 * cuenta y promedia, nunca devuelve datos de una usuaria en particular.
 */
public interface FuenteAnalisis {

    /**
     * @param dias        tamaño del periodo, hacia atrás desde hoy (eventos: aperturas,
     *                    preparaciones, avisos)
     * @param lunes       lunes de la semana actual
     * @param desdeLunes  lunes de la primera semana que cuenta para los planes
     */
    DatosAnalisis leer(int dias, LocalDate lunes, LocalDate desdeLunes);
}
