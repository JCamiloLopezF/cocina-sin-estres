package co.edu.uniquindio.cocinasinestres.planificador.dto;

import java.util.List;

/**
 * Cuerpo opcional de {@code POST /api/planes-semanales/{id}/generar}.
 *
 * @param comidas comidas a llenar ({@code ALMUERZO}, {@code CENA}, {@code DESAYUNO});
 *                sin valor, almuerzo y cena
 */
public record GenerarPlanRequest(List<String> comidas) {
}
