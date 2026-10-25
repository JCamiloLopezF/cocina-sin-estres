package co.edu.uniquindio.cocinasinestres.avisos.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.uniquindio.cocinasinestres.avisos.domain.AlmacenAvisos;
import co.edu.uniquindio.cocinasinestres.avisos.domain.AlmacenAvisos.AvisoGuardado;
import co.edu.uniquindio.cocinasinestres.avisos.domain.Aviso;
import co.edu.uniquindio.cocinasinestres.avisos.domain.EstadoSemana;
import co.edu.uniquindio.cocinasinestres.avisos.domain.FuenteAvisos;
import co.edu.uniquindio.cocinasinestres.avisos.domain.ProductoAviso;
import co.edu.uniquindio.cocinasinestres.avisos.domain.TipoAviso;
import co.edu.uniquindio.cocinasinestres.avisos.dto.AvisoResponse;
import co.edu.uniquindio.cocinasinestres.comun.error.ConflictoException;
import co.edu.uniquindio.cocinasinestres.comun.error.DatosInvalidosException;
import co.edu.uniquindio.cocinasinestres.comun.error.RecursoNoEncontradoException;
import co.edu.uniquindio.cocinasinestres.perfil.domain.UsuarioAutenticado;
import co.edu.uniquindio.cocinasinestres.perfil.service.UsuarioActualService;
import co.edu.uniquindio.cocinasinestres.planificador.dto.RescateResponse;
import co.edu.uniquindio.cocinasinestres.planificador.service.BorradorSemanaService;
import co.edu.uniquindio.cocinasinestres.planificador.service.BorradorSemanaService.BorradorSemana;
import co.edu.uniquindio.cocinasinestres.planificador.service.RescateService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Motor de avisos (KAN-27)")
class AvisosServiceTest {

    private static final Long HOGAR = 10L;
    /** Viernes 23 de octubre de 2026, 3 p. m. en Bogotá. */
    private static final Clock VIERNES = Clock.fixed(Instant.parse("2026-10-23T20:00:00Z"), ZoneId.of("America/Bogota"));
    private static final Clock MARTES = Clock.fixed(Instant.parse("2026-10-20T20:00:00Z"), ZoneId.of("America/Bogota"));
    private static final LocalDate LUNES_SIGUIENTE = LocalDate.of(2026, 10, 26);

    @Mock private UsuarioActualService usuarioActualService;
    @Mock private FuenteAvisos fuente;
    @Mock private AlmacenAvisos almacen;
    @Mock private BorradorSemanaService borradores;
    @Mock private RescateService rescateService;

    /** Lo que se guardó, para devolverlo como "sin leer". */
    private final List<Aviso> guardados = new ArrayList<>();

    @BeforeEach
    void preparar() {
        UsuarioAutenticado usuario = mock(UsuarioAutenticado.class);
        lenient().when(usuario.id()).thenReturn(1L);
        lenient().when(usuarioActualService.obtener()).thenReturn(usuario);
        lenient().when(fuente.hogarDe(1L)).thenReturn(Optional.of(HOGAR));
        lenient().when(fuente.apagados(HOGAR)).thenReturn(Set.of());
        lenient().when(fuente.semana(eq(HOGAR), any())).thenReturn(EstadoSemana.SIN_PLAN);
        lenient().when(rescateService.opciones(null)).thenReturn(new RescateResponse(List.of()));
        lenient().doAnswer(inv -> {
            guardados.add(inv.getArgument(1));
            return null;
        }).when(almacen).guardar(eq(HOGAR), any(Aviso.class));
        lenient().when(almacen.sinLeer(eq(HOGAR), anyCollection())).thenAnswer(inv -> {
            Collection<String> claves = inv.getArgument(1);
            List<AvisoGuardado> filas = new ArrayList<>();
            long id = 1;
            for (Aviso a : guardados) {
                if (claves.contains(a.clave())) {
                    filas.add(new AvisoGuardado(id++, a.tipo(), a.titulo(), a.mensaje(), a.enlace(), a.accion(), false, null));
                }
            }
            return filas;
        });
    }

    private AvisosService servicio(Clock reloj) {
        return new AvisosService(usuarioActualService, fuente, almacen, borradores, rescateService, reloj);
    }

    @Test
    @DisplayName("sin perfil del hogar no hay avisos")
    void sinHogar() {
        when(fuente.hogarDe(1L)).thenReturn(Optional.empty());

        assertThat(servicio(VIERNES).evaluar()).isEmpty();
        verify(almacen, never()).guardar(anyLong(), any());
    }

    @Test
    @DisplayName("el viernes, si la semana siguiente no tiene plan, deja un borrador y avisa para revisarlo")
    void borradorElViernes() {
        when(borradores.armar(LUNES_SIGUIENTE)).thenReturn(
            Optional.of(new BorradorSemana(30L, LUNES_SIGUIENTE, 14, BigDecimal.valueOf(150_000))));
        when(fuente.semana(HOGAR, LUNES_SIGUIENTE)).thenReturn(
            EstadoSemana.SIN_PLAN, new EstadoSemana(EstadoSemana.Situacion.BORRADOR, 14));

        List<AvisoResponse> avisos = servicio(VIERNES).evaluar();

        assertThat(avisos).singleElement().satisfies(a -> {
            assertThat(a.tipo()).isEqualTo("SEMANA_SIN_PLANIFICAR");
            assertThat(a.titulo()).isEqualTo("Te armamos la próxima semana");
            assertThat(a.enlace()).isEqualTo("/plan?semana=proxima");
        });
    }

    @Test
    @DisplayName("si el borrador falla, el aviso igual sale invitando a armar la semana")
    void borradorQueFalla() {
        when(borradores.armar(LUNES_SIGUIENTE)).thenThrow(new IllegalStateException("sin recetas"));

        assertThat(servicio(VIERNES).evaluar()).singleElement()
            .satisfies(a -> assertThat(a.titulo()).isEqualTo("La próxima semana todavía no tiene plan"));
    }

    @Test
    @DisplayName("entre semana no se revisa la semana siguiente ni se arma borrador")
    void martes() {
        assertThat(servicio(MARTES).evaluar()).isEmpty();
        verify(borradores, never()).armar(any());
        verify(fuente, never()).semana(anyLong(), any());
    }

    @Test
    @DisplayName("los tipos apagados no se evalúan")
    void apagados() {
        when(fuente.apagados(HOGAR)).thenReturn(Set.of(TipoAviso.SEMANA_SIN_PLANIFICAR, TipoAviso.APROVECHAR_PRODUCTO));
        when(fuente.bajoMinimo(HOGAR)).thenReturn(List.of(new ProductoAviso(1L, "Arroz", null)));

        List<AvisoResponse> avisos = servicio(VIERNES).evaluar();

        assertThat(avisos).extracting(AvisoResponse::tipo).containsExactly("BAJO_MINIMO");
        verify(borradores, never()).armar(any());
        verify(rescateService, never()).opciones(any());
    }

    @Test
    @DisplayName("corre las cinco reglas, guarda cada aviso una vez y muestra hasta cinco en orden")
    void cincoReglas() {
        LocalDate hoy = LocalDate.of(2026, 10, 23);
        when(fuente.bajoMinimo(HOGAR)).thenReturn(List.of(new ProductoAviso(1L, "Arroz", null)));
        when(fuente.porVencer(HOGAR, hoy, hoy.plusDays(3))).thenReturn(List.of(new ProductoAviso(2L, "Leche", hoy.plusDays(1))));
        when(fuente.preparacionesPara(HOGAR, hoy.plusDays(1))).thenReturn(List.of(
            new co.edu.uniquindio.cocinasinestres.avisos.domain.PreparacionPendiente(6, "ALMUERZO", "Fríjoles", "Remoja los fríjoles.")));
        when(fuente.semana(HOGAR, LUNES_SIGUIENTE)).thenReturn(new EstadoSemana(EstadoSemana.Situacion.BORRADOR, 14));
        when(rescateService.opciones(null)).thenReturn(new RescateResponse(List.of(
            new RescateResponse.Opcion(5L, "Pericos", 15, "APTA", List.of()),
            new RescateResponse.Opcion(7L, "Arroz con leche", 40, "APTA",
                List.of(new RescateResponse.PorVencer(2L, "Leche", hoy.plusDays(1), 1))))));

        List<AvisoResponse> avisos = servicio(VIERNES).evaluar();

        assertThat(avisos).extracting(AvisoResponse::tipo).containsExactly(
            "BAJO_MINIMO", "POR_VENCER", "PREPARACION_PREVIA", "SEMANA_SIN_PLANIFICAR", "APROVECHAR_PRODUCTO");
        assertThat(avisos.get(4).titulo()).isEqualTo("Hoy puedes hacer Arroz con leche");
        verify(almacen, times(5)).guardar(eq(HOGAR), any(Aviso.class));
        // La semana ya tenía borrador: no se vuelve a armar.
        verify(borradores, never()).armar(any());
    }

    @Test
    @DisplayName("si el modo rescate falla, los demás avisos igual llegan")
    void rescateQueFalla() {
        when(rescateService.opciones(null)).thenThrow(new IllegalStateException("caído"));
        when(fuente.bajoMinimo(HOGAR)).thenReturn(List.of(new ProductoAviso(1L, "Arroz", null)));
        when(fuente.apagados(HOGAR)).thenReturn(Set.of(TipoAviso.SEMANA_SIN_PLANIFICAR));

        assertThat(servicio(VIERNES).evaluar()).extracting(AvisoResponse::tipo).containsExactly("BAJO_MINIMO");
    }

    @Test
    @DisplayName("marcar como leído un aviso de otro hogar responde 404")
    void marcarLeido() {
        when(almacen.marcarLeido(HOGAR, 99L)).thenReturn(false);
        when(almacen.marcarLeido(HOGAR, 5L)).thenReturn(true);

        servicio(VIERNES).marcarLeido(5L);
        assertThatThrownBy(() -> servicio(VIERNES).marcarLeido(99L)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    @DisplayName("las preferencias se encienden y apagan por tipo")
    void preferencias() {
        when(fuente.apagados(HOGAR)).thenReturn(Set.of(TipoAviso.POR_VENCER));

        var preferencias = servicio(VIERNES).cambiarPreferencia("por_vencer", false);

        verify(almacen).cambiarPreferencia(HOGAR, TipoAviso.POR_VENCER, false);
        assertThat(preferencias).hasSize(5);
        assertThat(preferencias).filteredOn(p -> p.tipo().equals("POR_VENCER")).singleElement()
            .satisfies(p -> assertThat(p.activo()).isFalse());
        assertThatThrownBy(() -> servicio(VIERNES).cambiarPreferencia("OTRO", true))
            .isInstanceOf(DatosInvalidosException.class);
        assertThatThrownBy(() -> servicio(VIERNES).cambiarPreferencia("POR_VENCER", null))
            .isInstanceOf(DatosInvalidosException.class);
    }

    @Test
    @DisplayName("el centro de avisos pide perfil del hogar")
    void centroSinHogar() {
        when(fuente.hogarDe(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio(VIERNES).recientes()).isInstanceOf(ConflictoException.class);
    }

    @Test
    @DisplayName("el centro de avisos trae los de los últimos 30 días")
    void recientes() {
        ArgumentCaptor<java.time.LocalDateTime> desde = ArgumentCaptor.forClass(java.time.LocalDateTime.class);
        when(almacen.desde(eq(HOGAR), desde.capture(), anyInt())).thenReturn(List.of(
            new AvisoGuardado(1L, TipoAviso.POR_VENCER, "Leche vence hoy", "Úsala.", "/despensa", "Ver", true, null)));

        assertThat(servicio(VIERNES).recientes()).singleElement().satisfies(a -> assertThat(a.leido()).isTrue());
        assertThat(desde.getValue().toLocalDate()).isEqualTo(LocalDate.of(2026, 9, 23));
    }
}
