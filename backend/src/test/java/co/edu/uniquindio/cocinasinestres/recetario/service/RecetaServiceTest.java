package co.edu.uniquindio.cocinasinestres.recetario.service;

import co.edu.uniquindio.cocinasinestres.comun.error.RecursoNoEncontradoException;
import co.edu.uniquindio.cocinasinestres.recetario.domain.RestriccionesDeProductos;
import co.edu.uniquindio.cocinasinestres.recetario.dto.RecetaDetalleResponse;
import co.edu.uniquindio.cocinasinestres.recetario.dto.RecetaResponse;
import co.edu.uniquindio.cocinasinestres.recetario.model.Receta;
import co.edu.uniquindio.cocinasinestres.recetario.repository.RecetaRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecetaServiceTest {

    @Mock
    private RecetaRepository recetaRepository;

    @Mock
    private RestriccionesDeProductos restriccionesDeProductos;

    @InjectMocks
    private RecetaService service;

    /** Receta persistida (con id) con dos pasos y dos ingredientes. */
    private Receta recetaDePrueba(boolean activa) {
        Receta receta = new Receta(
            "Arroz con pollo",
            "Almuerzo sencillo",
            50,
            4,
            "Secar el pollo\nSofreír la cebolla",
            "Remojar el arroz una hora",
            activa,
            1L);
        ReflectionTestUtils.setField(receta, "id", 1L);
        receta.agregarIngrediente(12L, new BigDecimal("500.000"), false);
        receta.agregarIngrediente(16L, new BigDecimal("250.000"), true);

        return receta;
    }

    @Test
    void listar_mapeaCadaRecetaASuResumen() {
        when(recetaRepository.findByActivaTrueOrderByNombreAsc())
            .thenReturn(List.of(recetaDePrueba(true)));

        List<RecetaResponse> recetas = service.listar();

        assertEquals(1, recetas.size());
        RecetaResponse resumen = recetas.get(0);
        assertEquals(1L, resumen.id());
        assertEquals("Arroz con pollo", resumen.nombre());
        assertEquals("Almuerzo sencillo", resumen.descripcion());
        assertEquals(50, resumen.tiempoMinutos());
        assertEquals(4, resumen.porciones());
    }

    @Test
    void listar_sinRecetas_devuelveListaVacia() {
        when(recetaRepository.findByActivaTrueOrderByNombreAsc()).thenReturn(List.of());

        assertTrue(service.listar().isEmpty());
    }

    @Test
    void obtenerPorId_recetaActiva_incluyePasosSeparadosEIngredientes() {
        when(recetaRepository.findById(1L)).thenReturn(Optional.of(recetaDePrueba(true)));

        RecetaDetalleResponse detalle = service.obtenerPorId(1L);

        assertEquals(1L, detalle.id());
        assertEquals("Arroz con pollo", detalle.nombre());
        assertEquals("Remojar el arroz una hora", detalle.preparacionPrevia());
        assertEquals(List.of("Secar el pollo", "Sofreír la cebolla"), detalle.pasos());
        assertEquals(2, detalle.ingredientes().size());
        assertEquals(12L, detalle.ingredientes().get(0).productoId());
        assertEquals(new BigDecimal("500.000"), detalle.ingredientes().get(0).cantidad());
        assertFalse(detalle.ingredientes().get(0).opcional());
        assertTrue(detalle.ingredientes().get(1).opcional());
    }

    @Test
    void obtenerPorId_lasRestriccionesDeLaRecetaSonLasDeSusIngredientesObligatorios() {
        when(recetaRepository.findById(1L)).thenReturn(Optional.of(recetaDePrueba(true)));
        // El 12 es obligatorio y tiene lactosa; el 16 es opcional y tiene gluten.
        when(restriccionesDeProductos.codigosPorProducto(List.of(12L, 16L)))
            .thenReturn(Map.of(12L, List.of("lactosa"), 16L, List.of("gluten")));

        RecetaDetalleResponse detalle = service.obtenerPorId(1L);

        assertEquals(List.of("lactosa"), detalle.ingredientes().get(0).restricciones());
        assertEquals(List.of("gluten"), detalle.ingredientes().get(1).restricciones());
        assertEquals(List.of("lactosa"), detalle.restricciones());
    }

    @Test
    void obtenerPorId_recetaInexistente_lanzaRecursoNoEncontrado() {
        when(recetaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.obtenerPorId(99L));
    }

    @Test
    void obtenerPorId_recetaDesactivada_lanzaRecursoNoEncontrado() {
        when(recetaRepository.findById(1L)).thenReturn(Optional.of(recetaDePrueba(false)));

        assertThrows(RecursoNoEncontradoException.class, () -> service.obtenerPorId(1L));
    }
}
