package co.edu.uniquindio.cocinasinestres.planificador.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.uniquindio.cocinasinestres.comun.error.ConflictoException;
import co.edu.uniquindio.cocinasinestres.comun.error.RecursoNoEncontradoException;
import co.edu.uniquindio.cocinasinestres.perfil.domain.PersonaResumen;
import co.edu.uniquindio.cocinasinestres.perfil.domain.ResumenHogar;
import co.edu.uniquindio.cocinasinestres.perfil.domain.UsuarioAutenticado;
import co.edu.uniquindio.cocinasinestres.perfil.model.Hogar;
import co.edu.uniquindio.cocinasinestres.perfil.repository.HogarRepository;
import co.edu.uniquindio.cocinasinestres.perfil.service.ResumenHogarService;
import co.edu.uniquindio.cocinasinestres.perfil.service.UsuarioActualService;
import co.edu.uniquindio.cocinasinestres.planificador.domain.CatalogoParaPlan;
import co.edu.uniquindio.cocinasinestres.planificador.domain.RecetaCandidata;
import co.edu.uniquindio.cocinasinestres.planificador.domain.StockEnDespensa;
import co.edu.uniquindio.cocinasinestres.planificador.dto.AdaptacionResponse;
import co.edu.uniquindio.cocinasinestres.planificador.dto.OtraOpcionRequest;
import co.edu.uniquindio.cocinasinestres.planificador.dto.PlanGeneradoResponse;
import co.edu.uniquindio.cocinasinestres.planificador.model.ComidaPlan;
import co.edu.uniquindio.cocinasinestres.planificador.model.PlanSemanal;
import co.edu.uniquindio.cocinasinestres.planificador.repository.ComidaPlanRepository;
import co.edu.uniquindio.cocinasinestres.planificador.repository.PlanSemanalRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("Servicio del generador del plan (ADR-24)")
class GeneradorPlanServiceTest {

    @Mock private PlanSemanalRepository planSemanalRepository;
    @Mock private ComidaPlanRepository comidaPlanRepository;
    @Mock private HogarRepository hogarRepository;
    @Mock private UsuarioActualService usuarioActualService;
    @Mock private ResumenHogarService resumenHogarService;
    @Mock private CatalogoParaPlan catalogo;
    @Mock private StockEnDespensa stockEnDespensa;
    @Mock private ListaComprasService listaComprasService;

    private GeneradorPlanService servicio;
    private Hogar hogar;
    private PlanSemanal plan;
    private final List<ComidaPlan> guardadas = new ArrayList<>();

    @BeforeEach
    void preparar() {
        Clock reloj = Clock.fixed(Instant.parse("2026-10-19T15:00:00Z"), ZoneId.of("America/Bogota"));
        servicio = new GeneradorPlanService(
            planSemanalRepository, comidaPlanRepository, hogarRepository, usuarioActualService,
            new ContextoPlanFactory(resumenHogarService, catalogo, stockEnDespensa, reloj),
            catalogo, listaComprasService);

        UsuarioAutenticado usuario = mock(UsuarioAutenticado.class);
        lenient().when(usuario.id()).thenReturn(1L);
        lenient().when(usuarioActualService.obtener()).thenReturn(usuario);

        hogar = mock(Hogar.class);
        lenient().when(hogar.getId()).thenReturn(10L);
        lenient().when(hogarRepository.findByUsuario_Id(1L)).thenReturn(Optional.of(hogar));

        plan = new PlanSemanal(hogar, LocalDate.of(2026, 10, 19));
        ReflectionTestUtils.setField(plan, "id", 7L);
        lenient().when(planSemanalRepository.findById(7L)).thenReturn(Optional.of(plan));

        lenient().when(resumenHogarService.resumir(hogar)).thenReturn(
            new ResumenHogar(10L, Set.of(), Set.of(), BigDecimal.valueOf(195_000), false));
        lenient().when(stockEnDespensa.disponibles(any(), any())).thenReturn(Map.of());
        lenient().when(stockEnDespensa.porVencer(any(), any(), any())).thenReturn(Map.of());

        Map<Long, RecetaCandidata> recetas = new HashMap<>();
        for (long id = 1; id <= 3; id++) {
            recetas.put(id, new RecetaCandidata(
                id, 30, BigDecimal.valueOf(4000), Map.of(), Set.of(), Set.of(), true, true, false));
        }
        lenient().when(catalogo.recetas()).thenReturn(recetas);

        lenient().when(comidaPlanRepository.save(any(ComidaPlan.class))).thenAnswer(inv -> {
            ComidaPlan comida = inv.getArgument(0);
            if (!guardadas.contains(comida)) {
                guardadas.add(comida);
            }
            return comida;
        });
        lenient().when(comidaPlanRepository.findByPlanSemanal_IdOrderByDiaAscTipoComidaAsc(7L))
            .thenAnswer(inv -> new ArrayList<>(guardadas));
    }

    @Test
    @DisplayName("\"Armar mi semana\" guarda almuerzo y cena de los siete días y regenera la lista una vez")
    void generaLaSemana() {
        PlanGeneradoResponse respuesta = servicio.generar(7L, null);

        verify(comidaPlanRepository, times(14)).save(any(ComidaPlan.class));
        verify(listaComprasService, times(1)).generarListaCompras(7L);
        assertThat(respuesta.cambios()).hasSize(14);
        assertThat(respuesta.plan().comidas()).hasSize(14);
        assertThat(respuesta.costoEstimado()).isEqualByComparingTo("56000");
        assertThat(respuesta.excedePresupuesto()).isFalse();
    }

    @Test
    @DisplayName("no toca un plan de otro hogar: responde como si no existiera")
    void planDeOtroHogar() {
        Hogar otro = mock(Hogar.class);
        when(otro.getId()).thenReturn(99L);
        PlanSemanal ajeno = new PlanSemanal(otro, LocalDate.of(2026, 10, 19));
        ReflectionTestUtils.setField(ajeno, "id", 8L);
        when(planSemanalRepository.findById(8L)).thenReturn(Optional.of(ajeno));

        assertThatThrownBy(() -> servicio.generar(8L, null)).isInstanceOf(RecursoNoEncontradoException.class);
        verify(comidaPlanRepository, never()).save(any());
    }

    @Test
    @DisplayName("\"Otra opción\" no cambia una comida ya preparada")
    void otraOpcionDePreparada() {
        ComidaPlan comida = new ComidaPlan(plan, 1, "ALMUERZO", 1L, null);
        ReflectionTestUtils.setField(comida, "id", 70L);
        comida.setPreparada(true);
        guardadas.add(comida);

        assertThatThrownBy(() -> servicio.otraOpcion(7L, 70L, null))
            .isInstanceOf(ConflictoException.class)
            .extracting("codigo").isEqualTo("PLAN_COMIDA_PREPARADA");
    }

    @Test
    @DisplayName("\"Otra opción\" cambia la receta y avisa cuando ya no quedan")
    void otraOpcion() {
        ComidaPlan comida = new ComidaPlan(plan, 1, "ALMUERZO", 1L, null);
        ReflectionTestUtils.setField(comida, "id", 70L);
        guardadas.add(comida);

        assertThat(servicio.otraOpcion(7L, 70L, null).recetaId()).isEqualTo(2L);
        verify(listaComprasService).generarListaCompras(7L);

        assertThatThrownBy(() -> servicio.otraOpcion(7L, 70L, new OtraOpcionRequest(List.of(1L, 3L))))
            .isInstanceOf(ConflictoException.class)
            .extracting("codigo").isEqualTo("PLAN_SIN_ALTERNATIVAS");
    }

    @Test
    @DisplayName("\"Ajustar al presupuesto\" sin nada que cambiar no regenera la lista")
    void ajusteSinCambios() {
        PlanGeneradoResponse respuesta = servicio.ajustarAlPresupuesto(7L);

        assertThat(respuesta.cambios()).isEmpty();
        verify(listaComprasService, never()).generarListaCompras(anyLong());
    }

    @Test
    @DisplayName("las adaptaciones nombran a la persona, el ingrediente y su reemplazo")
    void adaptaciones() {
        when(resumenHogarService.resumir(hogar)).thenReturn(new ResumenHogar(
            10L, Set.of("huevo"), Set.of(), BigDecimal.valueOf(195_000), true,
            List.of(new PersonaResumen(2L, "Samuel", 9, Set.of("huevo"), Set.of()))));
        when(catalogo.recetas()).thenReturn(Map.of(5L, new RecetaCandidata(
            5L, 25, BigDecimal.valueOf(4000), Map.of(10L, BigDecimal.ONE, 1L, BigDecimal.TEN),
            Set.of("huevo"), Set.of(), true, true, false, Set.of(1L))));
        when(catalogo.alergenos()).thenReturn(Map.of(10L, Set.of("huevo")));
        when(catalogo.reemplazos()).thenReturn(Map.of(10L, Set.of(11L)));
        when(catalogo.nombresProducto()).thenReturn(Map.of(10L, "Huevos", 11L, "Pollo", 1L, "Arroz"));

        List<AdaptacionResponse> respuesta = servicio.adaptaciones(List.of(5L, 999L));

        assertThat(respuesta).singleElement().satisfies(a -> {
            assertThat(a.estado()).isEqualTo("ADAPTADA");
            assertThat(a.cambios()).singleElement().satisfies(c -> {
                assertThat(c.tipo()).isEqualTo("REEMPLAZO");
                assertThat(c.persona()).isEqualTo("Samuel");
                assertThat(c.producto()).isEqualTo("Huevos");
                assertThat(c.reemplazo()).isEqualTo("Pollo");
            });
        });
    }
}
