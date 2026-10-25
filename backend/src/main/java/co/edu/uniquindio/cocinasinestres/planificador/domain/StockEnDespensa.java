package co.edu.uniquindio.cocinasinestres.planificador.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * Cuenta cuánto producto tiene el hogar en su despensa, para que la lista de
 * compras pida solo lo que falta.
 *
 * <p>Es un puerto provisional, igual que los de despensa: el planificador no puede
 * usar las entidades de otro módulo (RNF-26), así que lee la tabla
 * {@code item_despensa} por su cuenta. Cuando desp publique un servicio de
 * consulta, solo se cambia la implementación.</p>
 */
public interface StockEnDespensa {

    /**
     * @param hogarId hogar de la usuaria autenticada
     * @param hoy     día de referencia en la zona de Colombia; lo vencido desde
     *                ayer no cuenta como disponible
     * @return cantidad utilizable por producto, sin los productos vencidos
     */
    Map<Long, BigDecimal> disponibles(Long hogarId, LocalDate hoy);

    /**
     * Productos con cantidad que vencen entre hoy y {@code hasta}, inclusive: el
     * generador del plan los usa primero (ADR-24).
     *
     * @return nombre por producto
     */
    Map<Long, String> porVencer(Long hogarId, LocalDate hoy, LocalDate hasta);

    /**
     * Fecha de vencimiento de cada producto con cantidad que vence entre hoy y
     * {@code hasta}, inclusive. El modo rescate ordena con esto (KAN-25).
     */
    default Map<Long, LocalDate> vencimientos(Long hogarId, LocalDate hoy, LocalDate hasta) {
        return Map.of();
    }

    /**
     * Productos que quedaron por debajo de su cantidad mínima: van solos a la lista de
     * compras para reponer (ADR-24).
     *
     * @return cuánto falta para llegar al mínimo, por producto
     */
    default Map<Long, BigDecimal> bajoMinimo(Long hogarId) {
        return Map.of();
    }
}
