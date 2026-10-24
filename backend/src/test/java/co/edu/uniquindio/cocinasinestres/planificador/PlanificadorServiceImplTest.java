package co.edu.uniquindio.cocinasinestres.planificador.service;

import co.edu.uniquindio.cocinasinestres.comun.error.ConflictoException;
import co.edu.uniquindio.cocinasinestres.comun.error.DatosInvalidosException;
import co.edu.uniquindio.cocinasinestres.comun.error.RecursoNoEncontradoException;
import co.edu.uniquindio.cocinasinestres.perfil.domain.UsuarioAutenticado;
import co.edu.uniquindio.cocinasinestres.perfil.model.Hogar;
import co.edu.uniquindio.cocinasinestres.perfil.repository.HogarRepository;
import co.edu.uniquindio.cocinasinestres.perfil.service.UsuarioActualService;
import co.edu.uniquindio.cocinasinestres.planificador.domain.MovimientosDespensa;
import co.edu.uniquindio.cocinasinestres.planificador.dto.ComidaPlanAssignRequest;
import co.edu.uniquindio.cocinasinestres.planificador.dto.ComidaPlanResponse;
import co.edu.uniquindio.cocinasinestres.planificador.dto.ListaComprasResponse;
import co.edu.uniquindio.cocinasinestres.planificador.dto.PlanSemanalResponse;
import co.edu.uniquindio.cocinasinestres.planificador.model.ComidaPlan;
import co.edu.uniquindio.cocinasinestres.planificador.model.PlanSemanal;
import co.edu.uniquindio.cocinasinestres.planificador.repository.ComidaPlanRepository;
import co.edu.uniquindio.cocinasinestres.planificador.repository.PlanSemanalRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlanificadorServiceImplTest {

    @Mock
    private PlanSemanalRepository planSemanalRepository;

    @Mock
    private ComidaPlanRepository comidaPlanRepository;

    @Mock
    private UsuarioActualService usuarioActualService;

    @Mock
    private HogarRepository hogarRepository;

    @Mock
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Mock
    private ListaComprasService listaComprasService;

    @Mock
    private MovimientosDespensa movimientosDespensa;

    @InjectMocks
    private PlanificadorServiceImpl service;

    @Captor
    private ArgumentCaptor<ComidaPlan> comidaCaptor;

    private UsuarioAutenticado usuarioMock;
    private Hogar hogarMock;

    @BeforeEach
    void setUp() {
        usuarioMock = mock(UsuarioAutenticado.class);
        lenient().when(usuarioMock.id()).thenReturn(1L);
        lenient().when(usuarioActualService.obtener()).thenReturn(usuarioMock);

        hogarMock = mock(Hogar.class);
        lenient().when(hogarMock.getId()).thenReturn(10L);

        // Por defecto asumimos que cualquier receta consultada está activa
        lenient().when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any())).thenReturn(1);
    }

    /** Crea un plan "persistido" (con id) para que el servicio pueda comparar ids sin NPE. */
    private PlanSemanal planConId(Long id) {
        PlanSemanal plan = new PlanSemanal(hogarMock, LocalDate.of(2026, 10, 12));
        ReflectionTestUtils.setField(plan, "id", id);
        return plan;
    }

    @Test
    void crearPlan_semanaNoEmpiezaEnLunes_debeLanzarDatosInvalidos() {
        LocalDate martes = LocalDate.of(2026, 10, 13); // martes
        DatosInvalidosException ex = assertThrows(DatosInvalidosException.class,
            () -> service.crearPlanSiNoExiste(martes));
        assertNotNull(ex);
    }

    @Test
    void asignarComida_diaFueraDeRango_debeLanzarDatosInvalidos() {
        ComidaPlanAssignRequest req = new ComidaPlanAssignRequest(0, "DESAYUNO", 1L, null);
        DatosInvalidosException ex = assertThrows(DatosInvalidosException.class,
            () -> service.asignarComida(1L, req));
        assertNotNull(ex);
    }

    @Test
    void asignarComida_tipoInvalido_debeLanzarDatosInvalidos() {
        ComidaPlanAssignRequest req = new ComidaPlanAssignRequest(1, "MERIENDA", 1L, null);
        DatosInvalidosException ex = assertThrows(DatosInvalidosException.class,
            () -> service.asignarComida(1L, req));
        assertNotNull(ex);
    }

    @Test
    void crearPlan_usuarioSinHogar_debeLanzarConflicto() {
        LocalDate lunes = LocalDate.of(2026, 10, 12);
        when(hogarRepository.findByUsuario_Id(usuarioMock.id())).thenReturn(Optional.empty());

        ConflictoException ex = assertThrows(ConflictoException.class,
            () -> service.crearPlanSiNoExiste(lunes));
        assertNotNull(ex);
    }

    @Test
    void asignarComida_planDeOtroHogar_debeLanzarRecursoNoEncontrado() {
        PlanSemanal planOtro = new PlanSemanal(mock(Hogar.class), LocalDate.of(2026, 10, 12));
        when(planSemanalRepository.findById(100L)).thenReturn(Optional.of(planOtro));
        when(hogarRepository.findByUsuario_Id(usuarioMock.id())).thenReturn(Optional.of(hogarMock));

        ComidaPlanAssignRequest req = new ComidaPlanAssignRequest(1, "DESAYUNO", 5L, null);
        RecursoNoEncontradoException ex = assertThrows(RecursoNoEncontradoException.class,
            () -> service.asignarComida(100L, req));
        assertNotNull(ex);
    }

    @Test
    void asignarComida_mismoIdCambioCliente_dosVeces_actualizaExistente() {
        PlanSemanal plan = planConId(200L);
        ComidaPlan existente = new ComidaPlan(plan, 1, "DESAYUNO", 2L, "cid-123");

        when(planSemanalRepository.findById(200L)).thenReturn(Optional.of(plan));
        when(hogarRepository.findByUsuario_Id(usuarioMock.id())).thenReturn(Optional.of(hogarMock));
        when(comidaPlanRepository.findByIdCambioCliente("cid-123")).thenReturn(Optional.of(existente));
        when(comidaPlanRepository.save(any(ComidaPlan.class))).thenAnswer(i -> i.getArgument(0));

        ComidaPlanAssignRequest req = new ComidaPlanAssignRequest(1, "DESAYUNO", 99L, "cid-123");
        ComidaPlanResponse resp = service.asignarComida(200L, req);

        assertEquals(99L, resp.recetaId());
        verify(listaComprasService, times(1)).generarListaCompras(200L);
    }

    @Test
    void asignarComida_diaYTipoOcupados_reemplazaReceta() {
        PlanSemanal plan = planConId(300L);
        ComidaPlan existing = new ComidaPlan(plan, 2, "ALMUERZO", 10L, null);

        when(planSemanalRepository.findById(300L)).thenReturn(Optional.of(plan));
        when(hogarRepository.findByUsuario_Id(usuarioMock.id())).thenReturn(Optional.of(hogarMock));
        when(comidaPlanRepository.findByPlanSemanal_IdAndDiaAndTipoComida(300L, 2, "ALMUERZO")).thenReturn(Optional.of(existing));
        when(comidaPlanRepository.save(any(ComidaPlan.class))).thenAnswer(i -> i.getArgument(0));

        ComidaPlanAssignRequest req = new ComidaPlanAssignRequest(2, "ALMUERZO", 77L, null);
        ComidaPlanResponse resp = service.asignarComida(300L, req);

        assertEquals(77L, resp.recetaId());
        verify(comidaPlanRepository, times(1)).save(existing);
        verify(listaComprasService, times(1)).generarListaCompras(300L);
    }

    @Test
    void consultarListaCompras_usuarioSinHogar_debeLanzarConflicto() {
        when(hogarRepository.findByUsuario_Id(usuarioMock.id())).thenReturn(Optional.empty());

        ConflictoException ex = assertThrows(ConflictoException.class,
            () -> service.consultarListaCompras(200L));
        assertNotNull(ex);
    }

    @Test
    void consultarListaCompras_planInexistente_debeLanzarRecursoNoEncontrado() {
        when(hogarRepository.findByUsuario_Id(usuarioMock.id())).thenReturn(Optional.of(hogarMock));
        when(planSemanalRepository.findById(999L)).thenReturn(Optional.empty());

        RecursoNoEncontradoException ex = assertThrows(RecursoNoEncontradoException.class,
            () -> service.consultarListaCompras(999L));
        assertNotNull(ex);
    }

    @Test
    void consultarListaCompras_planDeOtroHogar_debeLanzarRecursoNoEncontrado() {
        // Plan creado por un hogar distinto: mismo 404 que "no existe" (RNF-22)
        PlanSemanal planOtro = new PlanSemanal(mock(Hogar.class), LocalDate.of(2026, 10, 12));
        ReflectionTestUtils.setField(planOtro, "id", 400L);
        when(hogarRepository.findByUsuario_Id(usuarioMock.id())).thenReturn(Optional.of(hogarMock));
        when(planSemanalRepository.findById(400L)).thenReturn(Optional.of(planOtro));

        RecursoNoEncontradoException ex = assertThrows(RecursoNoEncontradoException.class,
            () -> service.consultarListaCompras(400L));
        assertNotNull(ex);
    }

    @Test
    void consultarListaCompras_planPropio_delegaEnElServicioDeListaCompras() {
        PlanSemanal plan = planConId(200L);
        ListaComprasResponse esperada = new ListaComprasResponse(
            7L, 200L, null, List.of(), BigDecimal.ZERO);
        when(hogarRepository.findByUsuario_Id(usuarioMock.id())).thenReturn(Optional.of(hogarMock));
        when(planSemanalRepository.findById(200L)).thenReturn(Optional.of(plan));
        when(listaComprasService.consultar(200L)).thenReturn(esperada);

        ListaComprasResponse respuesta = service.consultarListaCompras(200L);

        assertEquals(esperada, respuesta);
        verify(listaComprasService, times(1)).consultar(200L);
    }

    @Test
    void marcarPreparada_descuentaLaDespensaUnaSolaVez() {
        when(hogarRepository.findByUsuario_Id(usuarioMock.id())).thenReturn(Optional.of(hogarMock));
        PlanSemanal plan = planConId(1L);
        ComidaPlan comida = new ComidaPlan(plan, 1, "ALMUERZO", 4L, null);
        ReflectionTestUtils.setField(comida, "id", 70L);
        when(comidaPlanRepository.findById(70L)).thenReturn(Optional.of(comida));
        when(comidaPlanRepository.save(any(ComidaPlan.class))).thenAnswer(i -> i.getArgument(0));

        service.marcarPreparada(1L, 70L);
        service.marcarPreparada(1L, 70L);

        verify(movimientosDespensa, times(1)).descontarReceta(10L, 4L);
        assertTrue(comida.isPreparada());
    }

    @Test
    void confirmarBorrador_dejaDeSerBorradorYEsIdempotente() {
        PlanSemanal plan = planConId(30L);
        plan.setBorrador(true);
        when(hogarRepository.findByUsuario_Id(1L)).thenReturn(Optional.of(hogarMock));
        when(planSemanalRepository.findById(30L)).thenReturn(Optional.of(plan));

        PlanSemanalResponse respuesta = service.confirmarBorrador(30L);

        assertFalse(respuesta.borrador());
        assertFalse(plan.isBorrador());
        verify(planSemanalRepository).save(plan);

        service.confirmarBorrador(30L);
        verify(planSemanalRepository, times(1)).save(plan);
    }

    @Test
    void confirmarBorrador_planDeOtroHogar_respondeNoEncontrado() {
        PlanSemanal ajeno = new PlanSemanal(mock(Hogar.class), LocalDate.of(2026, 10, 26));
        ReflectionTestUtils.setField(ajeno, "id", 31L);
        when(hogarRepository.findByUsuario_Id(1L)).thenReturn(Optional.of(hogarMock));
        when(planSemanalRepository.findById(31L)).thenReturn(Optional.of(ajeno));

        assertThrows(RecursoNoEncontradoException.class, () -> service.confirmarBorrador(31L));
        verify(planSemanalRepository, never()).save(any());
    }
}

