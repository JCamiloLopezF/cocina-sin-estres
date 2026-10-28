package co.edu.uniquindio.cocinasinestres.planificador.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Cuánto se cocina para el hogar: una porción por persona. Las cantidades de una receta
 * están escritas para sus porciones; para el hogar se multiplican por
 * {@code personas / porciones}. Un hogar de 1 persona no paga ni compra 4 porciones.
 *
 * <p>Dominio puro: se prueba sin Spring.</p>
 */
public final class Porciones {

    private Porciones() {
    }

    /**
     * @param personas        quienes comen en casa; 0 si no se sabe
     * @param porcionesReceta porciones para las que está escrita la receta
     * @return el factor; 1 si falta alguno de los dos datos
     */
    public static BigDecimal factor(int personas, Integer porcionesReceta) {
        if (personas <= 0 || porcionesReceta == null || porcionesReceta <= 0) {
            return BigDecimal.ONE;
        }
        return BigDecimal.valueOf(personas).divide(BigDecimal.valueOf(porcionesReceta), 4, RoundingMode.HALF_UP);
    }
}
