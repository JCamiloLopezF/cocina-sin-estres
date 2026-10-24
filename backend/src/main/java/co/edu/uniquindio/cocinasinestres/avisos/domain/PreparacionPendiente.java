package co.edu.uniquindio.cocinasinestres.avisos.domain;

/**
 * Una comida del plan de mañana que pide alistar algo desde hoy (V1:
 * {@code receta.preparacion_previa}).
 *
 * @param dia        día de la semana de mañana (1 = lunes)
 * @param tipoComida DESAYUNO, ALMUERZO o CENA
 * @param receta     nombre de la receta
 * @param indicacion lo que hay que hacer ("Remoja los fríjoles la noche anterior")
 */
public record PreparacionPendiente(int dia, String tipoComida, String receta, String indicacion) {
}
