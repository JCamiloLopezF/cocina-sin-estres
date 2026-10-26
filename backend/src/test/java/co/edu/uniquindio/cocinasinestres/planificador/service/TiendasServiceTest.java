package co.edu.uniquindio.cocinasinestres.planificador.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.uniquindio.cocinasinestres.comun.error.ConflictoException;
import co.edu.uniquindio.cocinasinestres.comun.error.DatosInvalidosException;
import co.edu.uniquindio.cocinasinestres.comun.error.RecursoNoEncontradoException;
import co.edu.uniquindio.cocinasinestres.perfil.domain.UsuarioAutenticado;
import co.edu.uniquindio.cocinasinestres.perfil.model.Hogar;
import co.edu.uniquindio.cocinasinestres.perfil.repository.HogarRepository;
import co.edu.uniquindio.cocinasinestres.perfil.service.UsuarioActualService;
import co.edu.uniquindio.cocinasinestres.planificador.dto.TiendaRequest;
import co.edu.uniquindio.cocinasinestres.planificador.dto.TiendaResponse;
import co.edu.uniquindio.cocinasinestres.planificador.model.Tienda;
import co.edu.uniquindio.cocinasinestres.planificador.repository.TiendaRepository;
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
@DisplayName("Tiendas para pedir por WhatsApp (KAN-53)")
class TiendasServiceTest {

    @Mock private TiendaRepository tiendaRepository;
    @Mock private HogarRepository hogarRepository;
    @Mock private UsuarioActualService usuarioActualService;

    private TiendasService servicio;

    @BeforeEach
    void preparar() {
        servicio = new TiendasService(tiendaRepository, hogarRepository, usuarioActualService);
        UsuarioAutenticado usuario = mock(UsuarioAutenticado.class);
        lenient().when(usuario.id()).thenReturn(1L);
        lenient().when(usuarioActualService.obtener()).thenReturn(usuario);
        Hogar hogar = mock(Hogar.class);
        lenient().when(hogar.getId()).thenReturn(10L);
        lenient().when(hogarRepository.findByUsuario_Id(1L)).thenReturn(Optional.of(hogar));
    }

    private static Tienda tienda(long id, String nombre, String whatsapp) {
        Tienda tienda = new Tienda(10L, nombre, whatsapp);
        ReflectionTestUtils.setField(tienda, "id", id);
        return tienda;
    }

    @Test
    @DisplayName("guarda el nombre y el número listo para wa.me")
    void crea() {
        when(tiendaRepository.save(any(Tienda.class))).thenAnswer(i -> {
            Tienda t = i.getArgument(0);
            ReflectionTestUtils.setField(t, "id", 3L);
            return t;
        });

        TiendaResponse respuesta = servicio.crear(new TiendaRequest("  Tienda de don Jorge ", "300 123 4567"));

        assertThat(respuesta).isEqualTo(new TiendaResponse(3L, "Tienda de don Jorge", "573001234567"));
    }

    @Test
    @DisplayName("sin nombre o con un número que no es celular, no la guarda")
    void datosInvalidos() {
        assertThatThrownBy(() -> servicio.crear(new TiendaRequest(" ", "3001234567")))
            .isInstanceOf(DatosInvalidosException.class);
        assertThatThrownBy(() -> servicio.crear(new TiendaRequest("Tienda", "7451234")))
            .isInstanceOf(DatosInvalidosException.class);
        assertThatThrownBy(() -> servicio.crear(null)).isInstanceOf(DatosInvalidosException.class);
        verify(tiendaRepository, never()).save(any());
    }

    @Test
    @DisplayName("no repite número ni pasa de cinco tiendas")
    void limites() {
        when(tiendaRepository.existsByHogarIdAndWhatsapp(10L, "573001234567")).thenReturn(true);
        assertThatThrownBy(() -> servicio.crear(new TiendaRequest("Otra", "3001234567")))
            .isInstanceOf(ConflictoException.class)
            .hasFieldOrPropertyWithValue("codigo", "TIENDA_REPETIDA");

        when(tiendaRepository.countByHogarId(10L)).thenReturn(5L);
        assertThatThrownBy(() -> servicio.crear(new TiendaRequest("Sexta", "3109998877")))
            .isInstanceOf(ConflictoException.class)
            .hasFieldOrPropertyWithValue("codigo", "TIENDAS_MAXIMO");
        verify(tiendaRepository, never()).save(any());
    }

    @Test
    @DisplayName("lista las tiendas del hogar y solo borra las propias")
    void listaYBorra() {
        Tienda jorge = tienda(3L, "Tienda de don Jorge", "573001234567");
        when(tiendaRepository.findByHogarIdOrderByNombreAsc(10L)).thenReturn(List.of(jorge));
        when(tiendaRepository.findByIdAndHogarId(3L, 10L)).thenReturn(Optional.of(jorge));
        when(tiendaRepository.findByIdAndHogarId(99L, 10L)).thenReturn(Optional.empty());

        assertThat(servicio.listar()).containsExactly(new TiendaResponse(3L, "Tienda de don Jorge", "573001234567"));
        servicio.borrar(3L);
        verify(tiendaRepository).delete(jorge);
        assertThatThrownBy(() -> servicio.borrar(99L)).isInstanceOf(RecursoNoEncontradoException.class);
    }
}
