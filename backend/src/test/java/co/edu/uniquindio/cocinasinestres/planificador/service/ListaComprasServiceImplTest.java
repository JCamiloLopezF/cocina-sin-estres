package co.edu.uniquindio.cocinasinestres.planificador.service;

import co.edu.uniquindio.cocinasinestres.comun.config.ConfiguracionReloj;
import co.edu.uniquindio.cocinasinestres.perfil.model.Hogar;
import co.edu.uniquindio.cocinasinestres.planificador.domain.MovimientosDespensa;
import co.edu.uniquindio.cocinasinestres.planificador.domain.StockEnDespensa;
import co.edu.uniquindio.cocinasinestres.planificador.dto.CompraResponse;
import co.edu.uniquindio.cocinasinestres.planificador.dto.ListaComprasResponse;
import co.edu.uniquindio.cocinasinestres.planificador.model.ComidaPlan;
import co.edu.uniquindio.cocinasinestres.planificador.model.ItemListaCompras;
import co.edu.uniquindio.cocinasinestres.planificador.model.ListaCompras;
import co.edu.uniquindio.cocinasinestres.planificador.model.PlanSemanal;
import co.edu.uniquindio.cocinasinestres.planificador.repository.ComidaPlanRepository;
import co.edu.uniquindio.cocinasinestres.planificador.repository.ItemListaComprasRepository;
import co.edu.uniquindio.cocinasinestres.planificador.repository.ListaComprasRepository;
import co.edu.uniquindio.cocinasinestres.planificador.repository.PlanSemanalRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListaComprasServiceImplTest {

    /** Día fijo de las pruebas del cruce con la despensa. */
    private static final LocalDate HOY = LocalDate.of(2026, 10, 18);

    @Mock
    private PlanSemanalRepository planSemanalRepository;

    @Mock
    private ComidaPlanRepository comidaPlanRepository;

    @Mock
    private ListaComprasRepository listaComprasRepository;

    @Mock
    private ItemListaComprasRepository itemListaComprasRepository;

    @Mock
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Mock
    private StockEnDespensa stockEnDespensa;

    @Mock
    private MovimientosDespensa movimientosDespensa;

    @Mock
    private ComprasPorAlergias comprasPorAlergias;

    @Captor
    private ArgumentCaptor<List<ItemListaCompras>> itemsCaptor;

    private ListaComprasServiceImpl service;

    private ListaCompras lista;

    @BeforeEach
    void setUp() {
        Clock relojFijo = Clock.fixed(
            HOY.atStartOfDay(ConfiguracionReloj.ZONA_COLOMBIA).toInstant(),
            ConfiguracionReloj.ZONA_COLOMBIA);
        service = new ListaComprasServiceImpl(
            planSemanalRepository, comidaPlanRepository, listaComprasRepository,
            itemListaComprasRepository, jdbcTemplate, stockEnDespensa, movimientosDespensa,
            comprasPorAlergias, relojFijo);

        // El plan solo se usa como dueño de la lista; en este servicio no se consulta.
        lista = new ListaCompras(mock(PlanSemanal.class));
        ReflectionTestUtils.setField(lista, "id", 7L);
    }

    @Test
    void consultar_planSinComidas_devuelveListaVaciaYCostoCero() {
        when(listaComprasRepository.findByPlanSemanal_Id(anyLong())).thenReturn(Optional.empty());

        ListaComprasResponse respuesta = service.consultar(300L);

        assertNull(respuesta.id());
        assertEquals(300L, respuesta.planSemanalId());
        assertNull(respuesta.generadaEn());
        assertTrue(respuesta.items().isEmpty());
        assertEquals(BigDecimal.ZERO, respuesta.costoTotal());
    }

    @Test
    void consultar_conLista_devuelveItemsYSumaElCostoTotal() {
        when(listaComprasRepository.findByPlanSemanal_Id(300L)).thenReturn(Optional.of(lista));
        when(itemListaComprasRepository.findByListaCompras_Id(7L)).thenReturn(List.of(
            item(1L, "4500.00", 1),
            item(2L, "18000.00", 1)
        ));

        ListaComprasResponse respuesta = service.consultar(300L);

        assertEquals(7L, respuesta.id());
        assertEquals(2, respuesta.items().size());
        assertEquals(new BigDecimal("22500.00"), respuesta.costoTotal());
        assertEquals(1L, respuesta.items().get(0).productoId());
    }

    @Test
    void consultar_conLista_ordenaPorPrioridadDespuesPorProducto() {
        when(listaComprasRepository.findByPlanSemanal_Id(300L)).thenReturn(Optional.of(lista));
        when(itemListaComprasRepository.findByListaCompras_Id(7L)).thenReturn(List.of(
            item(3L, "2000.00", 2),
            item(1L, "4500.00", 1),
            item(2L, "5500.00", 1)
        ));

        ListaComprasResponse respuesta = service.consultar(300L);

        assertEquals(List.of(1L, 2L, 3L),
            respuesta.items().stream().map(i -> i.productoId()).toList());
    }

    @Test
    void consultar_listaVaciaPorErrorDeGeneracion_devuelveCostoCero() {
        when(listaComprasRepository.findByPlanSemanal_Id(300L)).thenReturn(Optional.of(lista));
        when(itemListaComprasRepository.findByListaCompras_Id(7L)).thenReturn(List.of());

        ListaComprasResponse respuesta = service.consultar(300L);

        assertEquals(7L, respuesta.id());
        assertTrue(respuesta.items().isEmpty());
        assertEquals(BigDecimal.ZERO, respuesta.costoTotal());
    }

    /** Plan persistido (id 500) cuyo hogar responde id 10. */
    private PlanSemanal planGeneracion() {
        Hogar hogar = mock(Hogar.class);
        // lenient: corregir el costo (KAN-26) no necesita saber de qué hogar es el plan.
        lenient().when(hogar.getId()).thenReturn(10L);
        PlanSemanal plan = new PlanSemanal(hogar, LocalDate.of(2026, 10, 12));
        ReflectionTestUtils.setField(plan, "id", 500L);
        return plan;
    }

    /** Deja el plan con una comida de la receta 4 y el plan pidiendo una cantidad dada. */
    private void planQuePide(Long productoId, String cantidadRequerida) {
        PlanSemanal plan = planGeneracion();
        when(planSemanalRepository.findById(500L)).thenReturn(Optional.of(plan));
        when(comidaPlanRepository.findByPlanSemanal_IdOrderByDiaAscTipoComidaAsc(500L))
            .thenReturn(List.of(new ComidaPlan(plan, 1, "ALMUERZO", 4L, null)));
        when(jdbcTemplate.queryForList(anyString(), eq(500L))).thenReturn(List.of(
            Map.of("producto_id", productoId, "cantidad_total", new BigDecimal(cantidadRequerida))));
    }

    /** Deja el precio de referencia del producto: unidad de compra de 1000 a $4000. */
    private void productoA4000(Long productoId) {
        when(jdbcTemplate.queryForMap(anyString(), eq(productoId))).thenReturn(Map.of(
            "cantidad_unidad_compra", new BigDecimal("1000.000"),
            "precio_unidad_compra", new BigDecimal("4000.00")));
    }

    @Test
    void generar_loQueLaDespensaYaTiene_noSePideEnLaLista() {
        planQuePide(1L, "500.000");
        when(stockEnDespensa.disponibles(10L, HOY))
            .thenReturn(Map.of(1L, new BigDecimal("200.000")));
        when(listaComprasRepository.findByPlanSemanal_Id(500L)).thenReturn(Optional.empty());
        when(listaComprasRepository.save(any(ListaCompras.class))).thenAnswer(i -> i.getArgument(0));
        productoA4000(1L);

        service.generarListaCompras(500L);

        verify(itemListaComprasRepository).saveAll(itemsCaptor.capture());
        ItemListaCompras item = itemsCaptor.getValue().get(0);
        // La receta pide 500 g y la despensa ya tiene 200 g: solo faltan 300 g.
        assertEquals(new BigDecimal("300.000"), item.getCantidad());
        // El costo se calcula sobre lo que falta: 300/1000 de una unidad a $4000.
        assertEquals(new BigDecimal("1200.00"), item.getCostoEstimado());
    }

    @Test
    void generar_siLaDespensaYaTieneTodo_noSeCreaLista() {
        planQuePide(1L, "250.000");
        when(stockEnDespensa.disponibles(10L, HOY))
            .thenReturn(Map.of(1L, new BigDecimal("300.000")));
        when(listaComprasRepository.findByPlanSemanal_Id(500L)).thenReturn(Optional.empty());

        service.generarListaCompras(500L);

        verify(itemListaComprasRepository, never()).saveAll(any());
        verify(listaComprasRepository, never()).delete(any(ListaCompras.class));
    }

    @Test
    void generar_sinNadaEnLaDespensa_pideLaCantidadCompleta() {
        planQuePide(1L, "500.000");
        when(stockEnDespensa.disponibles(10L, HOY)).thenReturn(Map.of());
        when(listaComprasRepository.findByPlanSemanal_Id(500L)).thenReturn(Optional.empty());
        when(listaComprasRepository.save(any(ListaCompras.class))).thenAnswer(i -> i.getArgument(0));
        productoA4000(1L);

        service.generarListaCompras(500L);

        verify(itemListaComprasRepository).saveAll(itemsCaptor.capture());
        ItemListaCompras item = itemsCaptor.getValue().get(0);
        assertEquals(new BigDecimal("500.000"), item.getCantidad());
        assertEquals(new BigDecimal("2000.00"), item.getCostoEstimado());
    }

    @Test
    void costoPorComprar_esLoQueFaltaConLaDespensa_sinGuardarNada() {
        planQuePide(1L, "500.000");
        when(stockEnDespensa.disponibles(10L, HOY))
            .thenReturn(Map.of(1L, new BigDecimal("200.000")));
        productoA4000(1L);

        // RNF-02: la receta pide 500 g, hay 200 g: faltan 300 g a $4000 el kilo.
        assertEquals(new BigDecimal("1200.00"), service.costoPorComprar(500L));
        verify(itemListaComprasRepository, never()).saveAll(any());
        verify(stockEnDespensa, never()).bajoMinimo(any());
    }

    // ---------------------------------------------------------------------
    // Despensa automática (ADR-24)
    // ---------------------------------------------------------------------

    @Test
    void generar_loYaPreparadoNoSeVuelveAComprar() {
        PlanSemanal plan = planGeneracion();
        when(planSemanalRepository.findById(500L)).thenReturn(Optional.of(plan));
        ComidaPlan preparada = new ComidaPlan(plan, 1, "ALMUERZO", 4L, null);
        preparada.setPreparada(true);
        when(comidaPlanRepository.findByPlanSemanal_IdOrderByDiaAscTipoComidaAsc(500L)).thenReturn(List.of(preparada));
        when(stockEnDespensa.disponibles(10L, HOY)).thenReturn(Map.of());
        when(listaComprasRepository.findByPlanSemanal_Id(500L)).thenReturn(Optional.empty());

        service.generarListaCompras(500L);

        verify(jdbcTemplate, never()).queryForList(anyString(), eq(500L));
        verify(itemListaComprasRepository, never()).saveAll(any());
    }

    @Test
    void generar_loQueQuedoBajoElMinimoSeRepone_yLaPorcionDeAlergiaSeMarca() {
        planQuePide(1L, "500.000");
        when(stockEnDespensa.disponibles(10L, HOY)).thenReturn(Map.of());
        when(stockEnDespensa.bajoMinimo(10L)).thenReturn(Map.of(2L, new BigDecimal("300.000"), 1L, new BigDecimal("50.000")));
        when(comprasPorAlergias.extras(any(), any())).thenReturn(Map.of(5L, new BigDecimal("125.000")));
        when(listaComprasRepository.findByPlanSemanal_Id(500L)).thenReturn(Optional.empty());
        when(listaComprasRepository.save(any(ListaCompras.class))).thenAnswer(i -> i.getArgument(0));
        when(jdbcTemplate.queryForMap(anyString(), any(Long.class))).thenReturn(Map.of(
            "cantidad_unidad_compra", new BigDecimal("1000.000"),
            "precio_unidad_compra", new BigDecimal("4000.00")));

        service.generarListaCompras(500L);

        verify(itemListaComprasRepository).saveAll(itemsCaptor.capture());
        Map<Long, ItemListaCompras> porProducto = new java.util.HashMap<>();
        itemsCaptor.getValue().forEach(i -> porProducto.put(i.getProductoId(), i));
        assertEquals("PLAN", porProducto.get(1L).getMotivo());
        assertEquals(1, porProducto.get(1L).getPrioridad());
        // El producto 1 ya lo pide el plan: no se repite como reposición.
        assertEquals(new BigDecimal("500.000"), porProducto.get(1L).getCantidad());
        assertEquals("REPONER", porProducto.get(2L).getMotivo());
        assertEquals(2, porProducto.get(2L).getPrioridad());
        assertEquals("ALERGIA", porProducto.get(5L).getMotivo());
        assertEquals(new BigDecimal("125.000"), porProducto.get(5L).getCantidad());
    }

    @Test
    void generar_loYaCompradoSeConservaEnLaLista() {
        planQuePide(1L, "500.000");
        when(stockEnDespensa.disponibles(10L, HOY)).thenReturn(Map.of());
        when(listaComprasRepository.findByPlanSemanal_Id(500L)).thenReturn(Optional.of(lista));
        ItemListaCompras comprado = item(9L, "4500.00", 1);
        comprado.setComprado(true);
        ItemListaCompras pendiente = item(1L, "2000.00", 1);
        when(itemListaComprasRepository.findByListaCompras_Id(7L)).thenReturn(List.of(comprado, pendiente));
        productoA4000(1L);

        service.generarListaCompras(500L);

        verify(itemListaComprasRepository).deleteAll(List.of(pendiente));
        verify(itemListaComprasRepository).saveAll(itemsCaptor.capture());
        assertEquals(1, itemsCaptor.getValue().size());
    }

    private PlanSemanal planConItem(ItemListaCompras item) {
        PlanSemanal plan = planGeneracion();
        ListaCompras deEstePlan = new ListaCompras(plan);
        ReflectionTestUtils.setField(item, "listaCompras", deEstePlan);
        when(itemListaComprasRepository.findById(70L)).thenReturn(Optional.of(item));
        when(itemListaComprasRepository.save(any(ItemListaCompras.class))).thenAnswer(i -> i.getArgument(0));
        return plan;
    }

    @Test
    void marcarComprado_entraALaDespensaEnPresentacionesCompletasConSuVencimiento() {
        ItemListaCompras arroz = new ItemListaCompras(lista, 1L, new BigDecimal("300.000"), new BigDecimal("1350.00"), 1);
        PlanSemanal plan = planConItem(arroz);
        when(movimientosDespensa.datosCompra(1L)).thenReturn(new MovimientosDespensa.DatosCompra(new BigDecimal("1000.000"), 365));
        when(movimientosDespensa.agregar(10L, 1L, new BigDecimal("1000.000"), HOY.plusDays(365), HOY)).thenReturn(HOY.plusDays(365));

        CompraResponse respuesta = service.marcarComprado(plan, 70L, true);

        assertTrue(respuesta.item().comprado());
        assertEquals(new BigDecimal("1000.000"), respuesta.item().cantidadAgregada());
        assertEquals(HOY.plusDays(365), respuesta.venceEn());

        // Marcarlo otra vez no lo suma dos veces.
        service.marcarComprado(plan, 70L, true);
        verify(movimientosDespensa, org.mockito.Mockito.times(1)).agregar(anyLong(), anyLong(), any(), any(), any());
    }

    @Test
    void marcarComprado_deshacerloLoSacaDeLaDespensa() {
        ItemListaCompras arroz = new ItemListaCompras(lista, 1L, new BigDecimal("300.000"), new BigDecimal("1350.00"), 1);
        arroz.setComprado(true);
        arroz.setCantidadAgregada(new BigDecimal("1000.000"));
        PlanSemanal plan = planConItem(arroz);

        CompraResponse respuesta = service.marcarComprado(plan, 70L, false);

        verify(movimientosDespensa).quitar(10L, 1L, new BigDecimal("1000.000"));
        assertFalse(respuesta.item().comprado());
        assertNull(respuesta.item().cantidadAgregada());
    }

    @Test
    void marcarComprado_unItemDeOtroPlanNoExiste() {
        ItemListaCompras ajeno = new ItemListaCompras(lista, 1L, BigDecimal.ONE, BigDecimal.ONE, 1);
        PlanSemanal otro = mock(PlanSemanal.class);
        when(otro.getId()).thenReturn(999L);
        ReflectionTestUtils.setField(ajeno, "listaCompras", new ListaCompras(otro));
        when(itemListaComprasRepository.findById(70L)).thenReturn(Optional.of(ajeno));

        PlanSemanal miPlan = new PlanSemanal(mock(Hogar.class), LocalDate.of(2026, 10, 12));
        ReflectionTestUtils.setField(miPlan, "id", 500L);

        assertThrows(co.edu.uniquindio.cocinasinestres.comun.error.RecursoNoEncontradoException.class,
            () -> service.marcarComprado(miPlan, 70L, true));
    }

    @Test
    void enPresentaciones_redondeaHaciaArribaYSinPresentacionDejaLaCantidad() {
        assertEquals(new BigDecimal("1000.000"), ListaComprasServiceImpl.enPresentaciones(new BigDecimal("300"), new BigDecimal("1000")));
        assertEquals(new BigDecimal("1000.000"), ListaComprasServiceImpl.enPresentaciones(new BigDecimal("1000"), new BigDecimal("1000")));
        assertEquals(new BigDecimal("2000.000"), ListaComprasServiceImpl.enPresentaciones(new BigDecimal("1001"), new BigDecimal("1000")));
        assertEquals(new BigDecimal("2.000"), ListaComprasServiceImpl.enPresentaciones(new BigDecimal("2"), null));
    }

    // ---------------------------------------------------------------------
    // Costo editable (KAN-26)
    // ---------------------------------------------------------------------

    @Test
    void consultar_conPrecioCorregido_elTotalUsaLoQuePago() {
        when(listaComprasRepository.findByPlanSemanal_Id(300L)).thenReturn(Optional.of(lista));
        ItemListaCompras arroz = item(1L, "4500.00", 1);
        arroz.setCostoReal(new BigDecimal("5200.00"));
        when(itemListaComprasRepository.findByListaCompras_Id(7L)).thenReturn(List.of(arroz, item(2L, "18000.00", 1)));

        ListaComprasResponse respuesta = service.consultar(300L);

        assertEquals(new BigDecimal("23200.00"), respuesta.costoTotal());
        assertEquals(new BigDecimal("4500.00"), respuesta.items().get(0).costoEstimado());
        assertEquals(new BigDecimal("5200.00"), respuesta.items().get(0).costoReal());
        assertNull(respuesta.items().get(1).costoReal());
    }

    @Test
    void cambiarCosto_guardaLoQuePagoYDevuelveElTotalAjustado() {
        ItemListaCompras arroz = new ItemListaCompras(lista, 1L, new BigDecimal("300.000"), new BigDecimal("1350.00"), 1);
        PlanSemanal plan = planConItem(arroz);
        ListaCompras deEstePlan = arroz.getListaCompras();
        ReflectionTestUtils.setField(deEstePlan, "id", 8L);
        when(listaComprasRepository.findByPlanSemanal_Id(500L)).thenReturn(Optional.of(deEstePlan));
        when(itemListaComprasRepository.findByListaCompras_Id(8L)).thenReturn(List.of(arroz));

        ListaComprasResponse respuesta = service.cambiarCosto(plan, 70L, new BigDecimal("1800"));

        assertEquals(new BigDecimal("1800.00"), arroz.getCostoReal());
        assertEquals(new BigDecimal("1800.00"), respuesta.costoTotal());

        // Con null vuelve al estimado.
        respuesta = service.cambiarCosto(plan, 70L, null);
        assertNull(arroz.getCostoReal());
        assertEquals(new BigDecimal("1350.00"), respuesta.costoTotal());
    }

    @Test
    void cambiarCosto_unValorNegativoSeRechaza() {
        PlanSemanal plan = new PlanSemanal(mock(Hogar.class), LocalDate.of(2026, 10, 12));

        assertThrows(co.edu.uniquindio.cocinasinestres.comun.error.DatosInvalidosException.class,
            () -> service.cambiarCosto(plan, 70L, new BigDecimal("-1")));
        verify(itemListaComprasRepository, never()).save(any(ItemListaCompras.class));
    }

    @Test
    void generar_conservaElPrecioCorregidoSiLaCantidadNoCambia() {
        planQuePide(1L, "500.000");
        when(stockEnDespensa.disponibles(10L, HOY)).thenReturn(Map.of());
        when(listaComprasRepository.findByPlanSemanal_Id(500L)).thenReturn(Optional.of(lista));
        ItemListaCompras corregido = new ItemListaCompras(lista, 1L, new BigDecimal("500.000"), new BigDecimal("2000.00"), 1);
        corregido.setCostoReal(new BigDecimal("2600.00"));
        when(itemListaComprasRepository.findByListaCompras_Id(7L)).thenReturn(List.of(corregido));
        productoA4000(1L);

        service.generarListaCompras(500L);

        verify(itemListaComprasRepository).saveAll(itemsCaptor.capture());
        assertEquals(new BigDecimal("2600.00"), itemsCaptor.getValue().get(0).getCostoReal());
    }

    @Test
    void generar_siCambiaLaCantidadVuelveAlEstimado() {
        planQuePide(1L, "800.000");
        when(stockEnDespensa.disponibles(10L, HOY)).thenReturn(Map.of());
        when(listaComprasRepository.findByPlanSemanal_Id(500L)).thenReturn(Optional.of(lista));
        ItemListaCompras corregido = new ItemListaCompras(lista, 1L, new BigDecimal("500.000"), new BigDecimal("2000.00"), 1);
        corregido.setCostoReal(new BigDecimal("2600.00"));
        when(itemListaComprasRepository.findByListaCompras_Id(7L)).thenReturn(List.of(corregido));
        productoA4000(1L);

        service.generarListaCompras(500L);

        verify(itemListaComprasRepository).saveAll(itemsCaptor.capture());
        assertNull(itemsCaptor.getValue().get(0).getCostoReal());
        assertEquals(new BigDecimal("3200.00"), itemsCaptor.getValue().get(0).getCostoEstimado());
    }

    private ItemListaCompras item(Long productoId, String costo, int prioridad) {
        return new ItemListaCompras(lista, productoId, BigDecimal.ONE,
            new BigDecimal(costo), prioridad);
    }
}
