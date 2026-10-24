package co.edu.uniquindio.cocinasinestres.planificador.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Cambios que el planificador le hace a la despensa por su cuenta (ADR-24, pieza 2):
 * lo comprado entra, lo preparado sale. Puerto propio: no usa las entidades de despensa
 * (RNF-26).
 */
public interface MovimientosDespensa {

    /**
     * Cómo se compra un producto.
     *
     * @param presentacion cuánto trae la unidad de compra, en unidad base (una bolsa de
     *                     arroz = 1000 g); {@code null} si no se conoce
     * @param vidaUtilDias cuántos días dura desde que se compra; {@code null} si no se conoce
     */
    record DatosCompra(BigDecimal presentacion, Integer vidaUtilDias) {
    }

    DatosCompra datosCompra(Long productoId);

    /**
     * Suma lo comprado a la despensa (crea el ítem si no existía). El vencimiento que
     * queda es el más próximo entre el que tenía (si sigue vigente y hay cantidad) y el
     * de lo nuevo: se gasta primero lo más viejo.
     *
     * @return el vencimiento con que quedó el ítem, o {@code null}
     */
    LocalDate agregar(Long hogarId, Long productoId, BigDecimal cantidad, LocalDate venceEn, LocalDate hoy);

    /** Resta una cantidad (sin bajar de cero). Sirve para deshacer una compra. */
    void quitar(Long hogarId, Long productoId, BigDecimal cantidad);

    /**
     * Descuenta de la despensa los ingredientes obligatorios de una receta preparada
     * (KAN-16), sin bajar de cero.
     *
     * @return cuántos productos se descontaron
     */
    int descontarReceta(Long hogarId, Long recetaId);
}
