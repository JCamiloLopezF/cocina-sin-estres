package co.edu.uniquindio.cocinasinestres.perfil.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.uniquindio.cocinasinestres.comun.config.PropiedadesCocina;
import co.edu.uniquindio.cocinasinestres.comun.error.DatosInvalidosException;
import co.edu.uniquindio.cocinasinestres.comun.seguridad.CifradorDatosSensibles;
import co.edu.uniquindio.cocinasinestres.perfil.domain.PeriodicidadIngreso;
import co.edu.uniquindio.cocinasinestres.perfil.domain.Rol;
import co.edu.uniquindio.cocinasinestres.perfil.domain.UsuarioAutenticado;
import co.edu.uniquindio.cocinasinestres.perfil.dto.PerfilHogarRequest;
import co.edu.uniquindio.cocinasinestres.perfil.dto.PerfilHogarResponse;
import co.edu.uniquindio.cocinasinestres.perfil.dto.PersonaRequest;
import co.edu.uniquindio.cocinasinestres.perfil.model.Hogar;
import co.edu.uniquindio.cocinasinestres.perfil.model.Persona;
import co.edu.uniquindio.cocinasinestres.perfil.model.Usuario;
import co.edu.uniquindio.cocinasinestres.perfil.repository.HogarRepository;
import co.edu.uniquindio.cocinasinestres.perfil.repository.PersonaRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.json.JsonMapper;

/** Alergias y condiciones por persona (ajuste del prototipo 2), con dobles de los repositorios. */
@DisplayName("HogarService: restricciones por persona")
class HogarServiceTest {

    private static final long USUARIA = 1L;

    private HogarRepository hogarRepository;
    private PersonaRepository personaRepository;
    private UsuarioActualService usuarioActualService;
    private HogarService servicio;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void preparar() {
        hogarRepository = mock(HogarRepository.class);
        personaRepository = mock(PersonaRepository.class);
        usuarioActualService = mock(UsuarioActualService.class);
        EntityManager entityManager = mock(EntityManager.class);
        CifradorDatosSensibles cifrador = new CifradorDatosSensibles(new PropiedadesCocina(
            null, new PropiedadesCocina.Cifrado(Base64.getEncoder().encodeToString(new byte[32])), null, null));

        when(usuarioActualService.obtener())
            .thenReturn(new UsuarioAutenticado(USUARIA, "uid-luisa", "luisa@ejemplo.com", Rol.USUARIA, false));
        when(entityManager.getReference(Usuario.class, USUARIA)).thenReturn(mock(Usuario.class));
        when(hogarRepository.findByUsuario_Id(USUARIA)).thenReturn(Optional.empty());
        when(hogarRepository.save(any(Hogar.class))).thenAnswer(invocacion -> {
            Hogar hogar = invocacion.getArgument(0);
            ReflectionTestUtils.setField(hogar, "id", 5L);
            return hogar;
        });
        when(personaRepository.findByHogar_IdOrderByIdAsc(anyLong())).thenReturn(List.of());
        when(personaRepository.saveAll(anyList())).thenAnswer(invocacion -> invocacion.getArgument(0));

        servicio = new HogarService(hogarRepository, personaRepository, usuarioActualService, cifrador,
            JsonMapper.builder().build(), entityManager);
    }

    private static PerfilHogarRequest perfil(boolean sinRestricciones, boolean consentimiento, PersonaRequest... personas) {
        return new PerfilHogarRequest(new BigDecimal("780000"), PeriodicidadIngreso.QUINCENAL, 15, 30, List.of(3),
            null, sinRestricciones, null, consentimiento, List.of(personas));
    }

    @Test
    void guardaLasAlergiasDeCadaPersonaCifradasYElHogarQuedaConLaUnion() {
        PerfilHogarResponse respuesta = servicio.guardar(perfil(false, true,
            new PersonaRequest("Luisa", 34, List.of("diabetes")),
            new PersonaRequest("Samuel", 9, List.of("lactosa", "Mani "))));

        assertThat(respuesta.personas().get(0).restricciones()).containsExactly("diabetes");
        assertThat(respuesta.personas().get(1).restricciones()).containsExactly("lactosa", "mani");
        assertThat(respuesta.restricciones()).containsExactlyInAnyOrder("diabetes", "lactosa", "mani");
        verify(usuarioActualService).actualizarPerfilCompleto(USUARIA, true);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Persona>> guardadas = ArgumentCaptor.forClass(List.class);
        verify(personaRepository).saveAll(guardadas.capture());
        // En la base va cifrado: el código no aparece en claro (ADR-20).
        assertThat(guardadas.getValue().get(1).getRestricciones()).isNotBlank().doesNotContain("lactosa");
    }

    @Test
    void unaPersonaSinAlergiasQuedaSinDatosDeSalud() {
        PerfilHogarResponse respuesta = servicio.guardar(perfil(false, true,
            new PersonaRequest("Luisa", 34, List.of()),
            new PersonaRequest("Samuel", 9, List.of("huevo"))));

        assertThat(respuesta.personas().get(0).restricciones()).isEmpty();
        assertThat(respuesta.restricciones()).containsExactly("huevo");
    }

    @Test
    void sinConsentimientoNoSeGuardanAlergiasPorPersona() {
        assertThatThrownBy(() -> servicio.guardar(perfil(false, false,
            new PersonaRequest("Samuel", 9, List.of("lactosa")))))
            .isInstanceOf(DatosInvalidosException.class)
            .hasMessageContaining("consentir");
    }

    @Test
    void noSePuedeDecirQueNadieTieneRestriccionesYMandarAlergiasPorPersona() {
        assertThatThrownBy(() -> servicio.guardar(perfil(true, true,
            new PersonaRequest("Samuel", 9, List.of("lactosa")))))
            .isInstanceOf(DatosInvalidosException.class);
    }

    @Test
    void sinRestriccionesElPerfilQuedaCompletoYSinDatosDeSalud() {
        PerfilHogarResponse respuesta = servicio.guardar(perfil(true, false,
            new PersonaRequest("Luisa", 34, null)));

        assertThat(respuesta.restricciones()).isEmpty();
        assertThat(respuesta.personas().get(0).restricciones()).isEmpty();
        verify(usuarioActualService).actualizarPerfilCompleto(USUARIA, true);
    }
}
