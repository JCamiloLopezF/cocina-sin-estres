package co.edu.uniquindio.cocinasinestres.planificador.dto;

import java.util.List;

/**
 * Cuerpo opcional de "Otra opción".
 *
 * @param excluir recetas que la usuaria ya descartó para ese hueco, para no volver a
 *                proponerlas
 */
public record OtraOpcionRequest(List<Long> excluir) {
}
