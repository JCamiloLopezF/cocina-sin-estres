package co.edu.uniquindio.cocinasinestres.planificador.service;

import co.edu.uniquindio.cocinasinestres.planificador.dto.ListaComprasResponse;
import co.edu.uniquindio.cocinasinestres.planificador.model.ItemListaCompras;
import co.edu.uniquindio.cocinasinestres.planificador.model.ListaCompras;
import co.edu.uniquindio.cocinasinestres.planificador.model.PlanSemanal;
import co.edu.uniquindio.cocinasinestres.planificador.repository.ComidaPlanRepository;
import co.edu.uniquindio.cocinasinestres.planificador.repository.ItemListaComprasRepository;
import co.edu.uniquindio.cocinasinestres.planificador.repository.ListaComprasRepository;
import co.edu.uniquindio.cocinasinestres.planificador.repository.PlanSemanalRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListaComprasServiceImplTest {

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

    private ListaComprasServiceImpl service;

    private ListaCompras lista;

    @BeforeEach
    void setUp() {
        service = new ListaComprasServiceImpl(
            planSemanalRepository, comidaPlanRepository, listaComprasRepository,
            itemListaComprasRepository, jdbcTemplate);

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

    private ItemListaCompras item(Long productoId, String costo, int prioridad) {
        return new ItemListaCompras(lista, productoId, BigDecimal.ONE,
            new BigDecimal(costo), prioridad);
    }
}
