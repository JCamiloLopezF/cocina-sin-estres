package co.edu.uniquindio.cocinasinestres.planificador.service;

import co.edu.uniquindio.cocinasinestres.comun.config.ConfiguracionReloj;
import co.edu.uniquindio.cocinasinestres.perfil.model.Hogar;
import co.edu.uniquindio.cocinasinestres.planificador.domain.StockEnDespensa;
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
            itemListaComprasRepository, jdbcTemplate, stockEnDespensa, relojFijo);

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
        when(hogar.getId()).thenReturn(10L);
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

    private ItemListaCompras item(Long productoId, String costo, int prioridad) {
        return new ItemListaCompras(lista, productoId, BigDecimal.ONE,
            new BigDecimal(costo), prioridad);
    }
}
