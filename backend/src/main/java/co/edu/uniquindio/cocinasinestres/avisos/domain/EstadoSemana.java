package co.edu.uniquindio.cocinasinestres.avisos.domain;

/**
 * Cómo está planeada una semana.
 *
 * @param situacion si tiene plan, si está vacío, si la app lo armó (borrador) o si ya está planeada
 * @param comidas   cuántas comidas tiene
 */
public record EstadoSemana(Situacion situacion, int comidas) {

    public enum Situacion { SIN_PLAN, VACIA, BORRADOR, PLANEADA }

    public static final EstadoSemana SIN_PLAN = new EstadoSemana(Situacion.SIN_PLAN, 0);

    /** Todavía no tiene comidas: el motor puede dejarle un borrador. */
    public boolean sinComidas() {
        return situacion == Situacion.SIN_PLAN || situacion == Situacion.VACIA;
    }
}
