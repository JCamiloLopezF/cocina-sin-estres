package co.edu.uniquindio.cocinasinestres.planificador.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Lista de compras de un plan semanal: los productos que hay que comprar para
 * cocinar todas las comidas asignadas y su costo total estimado.
 *
 * <p>Si el plan todavía no tiene comidas, la lista llega vacía con costo cero en
 * lugar de un error: el plan existe y aún no hay nada que comprar.</p>
 *
 * @param id             identificador de la lista, {@code null} si aún no se generó
 * @param planSemanalId  plan al que pertenece
 * @param generadaEn     cuándo se generó por última vez, {@code null} si no existe
 * @param items          productos pendientes de comprar
 * @param costoTotal     suma del costo de los ítems: lo que la usuaria pagó en los que lo
 *                       corrigió y el estimado en los demás (KAN-26)
 */
public record ListaComprasResponse(
    Long id,
    Long planSemanalId,
    LocalDateTime generadaEn,
    List<ItemListaComprasResponse> items,
    BigDecimal costoTotal
) {
}
