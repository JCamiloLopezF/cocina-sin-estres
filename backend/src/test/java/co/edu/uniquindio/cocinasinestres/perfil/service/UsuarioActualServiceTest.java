package co.edu.uniquindio.cocinasinestres.perfil.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.uniquindio.cocinasinestres.comun.config.PropiedadesCocina;
import co.edu.uniquindio.cocinasinestres.comun.error.CodigoError;
import co.edu.uniquindio.cocinasinestres.comun.error.NoAutenticadoException;
import co.edu.uniquindio.cocinasinestres.perfil.domain.Rol;
import co.edu.uniquindio.cocinasinestres.perfil.domain.UsuarioAutenticado;
import co.edu.uniquindio.cocinasinestres.perfil.model.Usuario;
import co.edu.uniquindio.cocinasinestres.perfil.model.UsuarioDePrueba;
import co.edu.uniquindio.cocinasinestres.perfil.repository.UsuarioRepository;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Prueba del alta perezosa (ADR-05) con un doble del repositorio: no levanta el
 * contexto de Spring ni toca MySQL.
 */
@DisplayName("UsuarioActualService")
class UsuarioActualServiceTest {

    private static final String UID = "uid-firebase-123";
    private static final String CORREO = "mariana@ejemplo.com";
    private static final String CORREO_ADMIN = "admin@ejemplo.com";

    private UsuarioRepository usuarioRepository;

    @BeforeEach
    void prepararDobles() {
        usuarioRepository = mock(UsuarioRepository.class);
    }

    @AfterEach
    void limpiarContextoDeSeguridad() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void creaLaCuentaLaPrimeraVezQueEntraUnUidDesconocido() {
        when(usuarioRepository.findByFirebaseUid(UID)).thenReturn(Optional.empty());
        when(usuarioRepository.save(any(Usuario.class)))
                .thenReturn(UsuarioDePrueba.conId(7L, UID, CORREO, Rol.USUARIA));

        UsuarioAutenticado resultado = servicioConAdmin(CORREO_ADMIN).registrarSiNoExiste(UID, CORREO);

        ArgumentCaptor<Usuario> guardado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(guardado.capture());
        assertThat(guardado.getValue().getFirebaseUid()).isEqualTo(UID);
        assertThat(guardado.getValue().getCorreo()).isEqualTo(CORREO);
        assertThat(guardado.getValue().getRol()).isEqualTo(Rol.USUARIA);
        assertThat(guardado.getValue().isPerfilCompleto()).isFalse();
        assertThat(guardado.getValue().isActivo()).isTrue();

        assertThat(resultado.id()).isEqualTo(7L);
        assertThat(resultado.rol()).isEqualTo(Rol.USUARIA);
    }

    @Test
    void noVuelveACrearLaCuentaSiElUidYaExiste() {
        when(usuarioRepository.findByFirebaseUid(UID))
                .thenReturn(Optional.of(UsuarioDePrueba.conId(7L, UID, CORREO, Rol.CURADOR)));

        UsuarioAutenticado resultado = servicioConAdmin(CORREO_ADMIN).registrarSiNoExiste(UID, CORREO);

        verify(usuarioRepository, never()).save(any(Usuario.class));
        assertThat(resultado.rol()).isEqualTo(Rol.CURADOR);
    }

    @Test
    void daRolAdminAlCorreoConfiguradoComoAdministradorInicial() {
        when(usuarioRepository.findByFirebaseUid(UID)).thenReturn(Optional.empty());
        when(usuarioRepository.save(any(Usuario.class)))
                .thenReturn(UsuarioDePrueba.conId(1L, UID, CORREO_ADMIN, Rol.ADMIN));

        // En mayúsculas a propósito: los correos no distinguen may./min.
        servicioConAdmin(CORREO_ADMIN).registrarSiNoExiste(UID, "ADMIN@EJEMPLO.COM");

        ArgumentCaptor<Usuario> guardado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(guardado.capture());
        assertThat(guardado.getValue().getRol()).isEqualTo(Rol.ADMIN);
    }

    @Test
    void noDaRolAdminSiNoHayCorreoInicialConfigurado() {
        when(usuarioRepository.findByFirebaseUid(UID)).thenReturn(Optional.empty());
        when(usuarioRepository.save(any(Usuario.class)))
                .thenReturn(UsuarioDePrueba.conId(1L, UID, CORREO, Rol.USUARIA));

        servicioConAdmin("").registrarSiNoExiste(UID, CORREO);

        ArgumentCaptor<Usuario> guardado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(guardado.capture());
        assertThat(guardado.getValue().getRol()).isEqualTo(Rol.USUARIA);
    }

    @Test
    void reutilizaLaCuentaQueCreoOtraPeticionSimultaneaDelMismoUid() {
        Usuario creadaPorLaOtra = UsuarioDePrueba.conId(9L, UID, CORREO, Rol.USUARIA);
        when(usuarioRepository.findByFirebaseUid(UID))
                .thenReturn(Optional.empty(), Optional.of(creadaPorLaOtra));
        when(usuarioRepository.save(any(Usuario.class)))
                .thenThrow(new DataIntegrityViolationException("uk_usuario_firebase_uid"));

        UsuarioAutenticado resultado = servicioConAdmin(CORREO_ADMIN).registrarSiNoExiste(UID, CORREO);

        assertThat(resultado.id()).isEqualTo(9L);
    }

    @Test
    void rechazaLaCuentaDesactivada() {
        when(usuarioRepository.findByFirebaseUid(UID))
                .thenReturn(Optional.of(UsuarioDePrueba.inactiva(7L, UID, CORREO)));

        assertThatThrownBy(() -> servicioConAdmin(CORREO_ADMIN).registrarSiNoExiste(UID, CORREO))
                .isInstanceOf(NoAutenticadoException.class)
                .extracting(error -> ((NoAutenticadoException) error).getCodigo())
                .isEqualTo(CodigoError.AUTH_CUENTA_INACTIVA);
    }

    @Test
    void rechazaUnTokenSinUidOSinCorreo() {
        UsuarioActualService servicio = servicioConAdmin(CORREO_ADMIN);

        assertThatThrownBy(() -> servicio.registrarSiNoExiste("  ", CORREO))
                .isInstanceOf(NoAutenticadoException.class);
        assertThatThrownBy(() -> servicio.registrarSiNoExiste(UID, null))
                .isInstanceOf(NoAutenticadoException.class);
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void obtenerDevuelveLaUsuariaQuePusoLaCapaDeSeguridad() {
        UsuarioAutenticado esperada = new UsuarioAutenticado(7L, UID, CORREO, Rol.USUARIA, false);
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(esperada, null, java.util.List.of()));

        assertThat(servicioConAdmin(CORREO_ADMIN).obtener()).isEqualTo(esperada);
    }

    @Test
    void obtenerFallaSiLaPeticionNoEstaAutenticada() {
        assertThatThrownBy(() -> servicioConAdmin(CORREO_ADMIN).obtener())
                .isInstanceOf(NoAutenticadoException.class)
                .extracting(error -> ((NoAutenticadoException) error).getCodigo())
                .isEqualTo(CodigoError.AUTH_TOKEN_INVALIDO);
    }

    private UsuarioActualService servicioConAdmin(String correoAdminInicial) {
        PropiedadesCocina propiedades = new PropiedadesCocina(
                null, null, new PropiedadesCocina.AdminInicial(correoAdminInicial), null);
        return new UsuarioActualService(usuarioRepository, propiedades);
    }
}
