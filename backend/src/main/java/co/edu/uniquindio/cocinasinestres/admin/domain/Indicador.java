package co.edu.uniquindio.cocinasinestres.admin.domain;

/**
 * Un indicador del Plan de Gestión de la Calidad (sección 3.4) con su valor actual.
 *
 * @param proceso  proceso de negocio al que pertenece
 * @param nombre   nombre del indicador, como en el plan
 * @param formula  cómo se calcula
 * @param valor    valor medido; {@code null} si no hay datos o no se mide en la app
 * @param unidad   "%", "min", "ms", "días" o "avisos"
 * @param meta     la interpretación esperada del plan ("> 90 %"); {@code null} si es de seguimiento
 * @param cumple   si el valor alcanza la meta; {@code null} sin datos o sin meta
 * @param muestra  sobre cuántos casos se calculó
 * @param nota     de dónde sale o por qué no hay valor
 */
public record Indicador(
    String proceso,
    String nombre,
    String formula,
    Double valor,
    String unidad,
    String meta,
    Boolean cumple,
    long muestra,
    String nota) {
}
