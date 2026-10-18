package co.edu.uniquindio.cocinasinestres.planificador.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import co.edu.uniquindio.cocinasinestres.comun.error.RecursoNoEncontradoException;
import co.edu.uniquindio.cocinasinestres.perfil.domain.Rol;
import co.edu.uniquindio.cocinasinestres.perfil.domain.UsuarioAutenticado;
import co.edu.uniquindio.cocinasinestres.perfil.model.Hogar;
import co.edu.uniquindio.cocinasinestres.perfil.repository.HogarRepository;
import co.edu.uniquindio.cocinasinestres.perfil.service.UsuarioActualService;
import co.edu.uniquindio.cocinasinestres.planificador.dto.PlanCostoResponse;
import co.edu.uniquindio.cocinasinestres.planificador.model.PlanSemanal;
import co.edu.uniquindio.cocinasinestres.planificador.repository.PlanSemanalRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("Costo acumulado del plan frente al presupuesto del hogar")
class PlanificadorCostoServiceTest {

    @Mock
    private PlanSemanalRepository planSemanalRepository;

    @Mock
    private HogarRepository hogarRepository;

    @Mock
    private UsuarioActualService usuarioActualService;

    @Mock
    private JdbcTemplate jdbcTemplate;

    private PlanificadorCostoService service;

    /** Hogar de la usuaria autenticada: id 10, presupuesto semanal de $5000. */
    private Hogar hogar;

    @BeforeEach
    void setUp() {
        service = new PlanificadorCostoService(
            planSemanalRepository, hogarRepository, usuarioActualService, jdbcTemplate);

        hogar = mock(Hogar.class);
        lenient().when(hogar.getId()).thenReturn(10L);
        lenient().when(hogar.getPresupuestoMonto()).thenReturn(new BigDecimal("5000.00"));
        lenient().when(hogar.getPeriodicidadIngreso()).thenReturn("SEMANAL");
        lenient().when(usuarioActualService.obtener()).thenReturn(
            new UsuarioAutenticado(20L, "uid-mariana", "mariana@dev.local", Rol.USUARIA, true));
        lenient().when(hogarRepository.findByUsuario_Id(20L)).thenReturn(Optional.of(hogar));
    }

    /** Plan persistido (id 500) del hogar de la usuaria autenticada. */
    private PlanSemanal planDePrueba() {
        PlanSemanal plan = new PlanSemanal(hogar, LocalDate.of(2026, 10, 12));
        ReflectionTestUtils.setField(plan, "id", 500L);
        return plan;
    }

    @Test
    @DisplayName("acumula el costo del lunes al domingo y no excede el presupuesto")
    void acumulaElCostoDiaADia() {
        when(planSemanalRepository.findById(500L)).thenReturn(Optional.of(planDePrueba()));
        when(jdbcTemplate.queryForList(anyString(), eq(500L))).thenReturn(List.of(
            Map.of("dia", 1, "costo", new BigDecimal("1000.50")),
            Map.of("dia", 3, "costo", new BigDecimal("2000.00"))));

        PlanCostoResponse respuesta = service.calcularCosto(500L);

        assertEquals(List.of(
            new BigDecimal("1000.50"), new BigDecimal("1000.50"), new BigDecimal("3000.50"),
            new BigDecimal("3000.50"), new BigDecimal("3000.50"), new BigDecimal("3000.50"),
            new BigDecimal("3000.50")), respuesta.costoAcumuladoPorDia());
        assertEquals(0, respuesta.presupuestoSemanal().compareTo(new BigDecimal("5000.00")));
        assertFalse(respuesta.excedePresupuesto());
    }

    @Test
    @DisplayName("marca excedePresupuesto cuando el total de la semana lo supera")
    void marcaCuandoExcedeElPresupuesto() {
        when(planSemanalRepository.findById(500L)).thenReturn(Optional.of(planDePrueba()));
        when(jdbcTemplate.queryForList(anyString(), eq(500L))).thenReturn(List.of(
            Map.of("dia", 2, "costo", new BigDecimal("6000.00"))));

        PlanCostoResponse respuesta = service.calcularCosto(500L);

        assertEquals(0, respuesta.costoAcumuladoPorDia().get(6)
            .compareTo(new BigDecimal("6000.00")));
        assertTrue(respuesta.excedePresupuesto());
    }

    @Test
    @DisplayName("un plan sin comidas tiene costo cero y no excede")
    void planSinComidasNoExcede() {
        when(planSemanalRepository.findById(500L)).thenReturn(Optional.of(planDePrueba()));
        when(jdbcTemplate.queryForList(anyString(), eq(500L))).thenReturn(List.of());

        PlanCostoResponse respuesta = service.calcularCosto(500L);

        assertEquals(0, respuesta.costoAcumuladoPorDia().get(6).compareTo(BigDecimal.ZERO));
        assertFalse(respuesta.excedePresupuesto());
    }

    @Test
    @DisplayName("el plan de otro hogar responde 404, igual que un plan inexistente")
    void planDeOtroHogarRespondeNoEncontrado() {
        Hogar otroHogar = mock(Hogar.class);
        when(otroHogar.getId()).thenReturn(99L);
        when(planSemanalRepository.findById(500L)).thenReturn(Optional.of(planDePrueba()));
        when(hogarRepository.findByUsuario_Id(20L)).thenReturn(Optional.of(otroHogar));

        assertThrows(RecursoNoEncontradoException.class, () -> service.calcularCosto(500L));
    }

    @Test
    @DisplayName("el plan inexistente responde 404")
    void planInexistenteRespondeNoEncontrado() {
        when(planSemanalRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.calcularCosto(404L));
    }
}
