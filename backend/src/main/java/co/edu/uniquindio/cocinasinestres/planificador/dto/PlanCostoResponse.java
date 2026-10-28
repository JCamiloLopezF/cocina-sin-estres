package co.edu.uniquindio.cocinasinestres.planificador.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Costo del plan semanal frente al presupuesto (ADR-14, RNF-02).
 *
 * @param costoAcumuladoPorDia valor de las comidas del plan, acumulado del lunes (índice 0) al
 *                             domingo (índice 6), con todos sus ingredientes aunque ya estén
 *                             en la despensa
 * @param presupuestoSemanal   presupuesto del hogar prorrateado a la semana
 * @param excedePresupuesto    lo que falta comprar supera el presupuesto
 * @param costoAlergias        lo que suma al valor el reemplazo o el plato aparte de quien tiene
 *                             alergia (KAN-24); ya está incluido en el acumulado
 * @param costoPorComprar      el costo del plan según RNF-02: la suma de los precios de
 *                             referencia de los ingredientes que faltan en la despensa para las
 *                             comidas que quedan por preparar. Es lo que se compara con el
 *                             presupuesto y coincide con la lista de compras sin lo que se repone
 */
public record PlanCostoResponse(
    List<BigDecimal> costoAcumuladoPorDia,
    BigDecimal presupuestoSemanal,
    boolean excedePresupuesto,
    BigDecimal costoAlergias,
    BigDecimal costoPorComprar
) {}
