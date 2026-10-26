package co.edu.uniquindio.cocinasinestres.planificador.service;

import co.edu.uniquindio.cocinasinestres.planificador.dto.CompraResponse;
import co.edu.uniquindio.cocinasinestres.planificador.model.PlanSemanal;

import co.edu.uniquindio.cocinasinestres.planificador.dto.ListaComprasResponse;
import java.math.BigDecimal;

/**
 * Servicio para generar y consultar la lista de compras de un plan semanal.
 */
public interface ListaComprasService {

    /**
     * Genera o reemplaza la lista de compras para el plan indicado. Las cantidades
     * agregan los ingredientes de todas las recetas y descuentan lo que el hogar ya
     * tiene en su despensa sin estar vencido: solo se pide lo que falta.
     *
     * @param planSemanalId identificador del plan semanal
     */
    void generarListaCompras(Long planSemanalId);

    /**
     * Lee la lista de compras ya generada para el plan, con su costo total.
     * Si el plan todavía no tiene comidas, devuelve una lista vacía con costo cero.
     *
     * @param planSemanalId identificador del plan semanal
     */
    ListaComprasResponse consultar(Long planSemanalId);

    /**
     * Marca un producto de la lista como comprado: entra a la despensa en presentaciones
     * completas y con su vencimiento estimado. Con {@code comprado = false} lo deshace.
     * Es idempotente. El plan ya debe estar verificado como del hogar de la usuaria.
     *
     * @throws co.edu.uniquindio.cocinasinestres.comun.error.RecursoNoEncontradoException
     *         si el ítem no es de la lista de ese plan
     */
    CompraResponse marcarComprado(PlanSemanal plan, Long itemId, boolean comprado);

    /**
     * Guarda lo que la usuaria pagó de verdad por un producto de la lista (KAN-26) y devuelve
     * la lista con el total ajustado. Con {@code costoReal = null} vuelve al estimado. El plan
     * ya debe estar verificado como del hogar de la usuaria.
     *
     * @throws co.edu.uniquindio.cocinasinestres.comun.error.DatosInvalidosException
     *         si el valor es negativo o demasiado grande
     * @throws co.edu.uniquindio.cocinasinestres.comun.error.RecursoNoEncontradoException
     *         si el ítem no es de la lista de ese plan
     */
    ListaComprasResponse cambiarCosto(PlanSemanal plan, Long itemId, BigDecimal costoReal);
}
