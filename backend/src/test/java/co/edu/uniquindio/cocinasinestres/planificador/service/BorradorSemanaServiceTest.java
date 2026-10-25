package co.edu.uniquindio.cocinasinestres.planificador.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.uniquindio.cocinasinestres.perfil.domain.UsuarioAutenticado;
import co.edu.uniquindio.cocinasinestres.perfil.model.Hogar;
import co.edu.uniquindio.cocinasinestres.perfil.repository.HogarRepository;
import co.edu.uniquindio.cocinasinestres.perfil.service.UsuarioActualService;
import co.edu.uniquindio.cocinasinestres.planificador.dto.CambioPlanResponse;
import co.edu.uniquindio.cocinasinestres.planificador.dto.PlanGeneradoResponse;
import co.edu.uniquindio.cocinasinestres.planificador.model.ComidaPlan;
import co.edu.uniquindio.cocinasinestres.planificador.model.PlanSemanal;
import co.edu.uniquindio.cocinasinestres.planificador.repository.ComidaPlanRepository;
import co.edu.uniquindio.cocinasinestres.planificador.repository.PlanSemanalRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("Borrador automático de la semana siguiente (ADR-24)")
class BorradorSemanaServiceTest {

    private static final LocalDate LUNES = LocalDate.of(2026, 10, 26);

    @Mock private PlanSemanalRepository planSemanalRepository;
    @Mock private ComidaPlanRepository comidaPlanRepository;
    @Mock private HogarRepository hogarRepository;
    @Mock private UsuarioActualService usuarioActualService;
    @Mock private GeneradorPlanService generadorPlanService;

    private BorradorSemanaService servicio;
    private Hogar hogar;

    @BeforeEach
    void preparar() {
        servicio = new BorradorSemanaService(
            planSemanalRepository, comidaPlanRepository, hogarRepository, usuarioActualService, generadorPlanService);
        UsuarioAutenticado usuario = mock(UsuarioAutenticado.class);
        lenient().when(usuario.id()).thenReturn(1L);
        lenient().when(usuarioActualService.obtener()).thenReturn(usuario);
        hogar = mock(Hogar.class);
        lenient().when(hogar.getId()).thenReturn(10L);
        lenient().when(hogarRepository.findByUsuario_Id(1L)).thenReturn(Optional.of(hogar));
    }

    private PlanSemanal plan() {
        PlanSemanal plan = new PlanSemanal(hogar, LUNES);
        ReflectionTestUtils.setField(plan, "id", 30L);
        return plan;
    }

    private static PlanGeneradoResponse generado(int comidas) {
        List<CambioPlanResponse> cambios = java.util.stream.IntStream.range(0, comidas)
            .mapToObj(i -> new CambioPlanResponse(i % 7 + 1, i < 7 ? "ALMUERZO" : "CENA", 5L, null))
            .toList();
        return new PlanGeneradoResponse(null, cambios, BigDecimal.valueOf(150_000), BigDecimal.valueOf(195_000),
            false, List.of(), 0, 0);
    }

    @Test
    @DisplayName("una semana sin plan queda armada y marcada como borrador")
    void semanaSinPlan() {
        PlanSemanal nuevo = plan();
        when(planSemanalRepository.findByHogar_IdAndSemanaInicio(10L, LUNES)).thenReturn(Optional.empty());
        when(planSemanalRepository.save(any(PlanSemanal.class))).thenReturn(nuevo);
        when(comidaPlanRepository.findByPlanSemanal_IdOrderByDiaAscTipoComidaAsc(30L)).thenReturn(List.of());
        when(generadorPlanService.generar(30L, null)).thenReturn(generado(14));

        var borrador = servicio.armar(LUNES);

        assertThat(borrador).hasValueSatisfying(b -> {
            assertThat(b.planId()).isEqualTo(30L);
            assertThat(b.comidas()).isEqualTo(14);
        });
        assertThat(nuevo.isBorrador()).isTrue();
    }

    @Test
    @DisplayName("si la usuaria ya empezó la semana, no se toca")
    void semanaEmpezada() {
        PlanSemanal existente = plan();
        when(planSemanalRepository.findByHogar_IdAndSemanaInicio(10L, LUNES)).thenReturn(Optional.of(existente));
        when(comidaPlanRepository.findByPlanSemanal_IdOrderByDiaAscTipoComidaAsc(30L))
            .thenReturn(List.of(new ComidaPlan(existente, 1, "ALMUERZO", 5L, null)));

        assertThat(servicio.armar(LUNES)).isEmpty();
        assertThat(existente.isBorrador()).isFalse();
        verify(generadorPlanService, never()).generar(any(), any());
    }

    @Test
    @DisplayName("si el generador no encuentra recetas, no hay borrador; y solo se arma desde un lunes")
    void sinRecetas() {
        PlanSemanal existente = plan();
        when(planSemanalRepository.findByHogar_IdAndSemanaInicio(10L, LUNES)).thenReturn(Optional.of(existente));
        when(comidaPlanRepository.findByPlanSemanal_IdOrderByDiaAscTipoComidaAsc(30L)).thenReturn(List.of());
        when(generadorPlanService.generar(30L, null)).thenReturn(generado(0));

        assertThat(servicio.armar(LUNES)).isEmpty();
        assertThat(servicio.armar(LUNES.plusDays(1))).isEmpty();
        assertThat(servicio.armar(null)).isEmpty();
    }
}
