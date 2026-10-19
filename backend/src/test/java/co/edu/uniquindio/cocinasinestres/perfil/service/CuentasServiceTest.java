package co.edu.uniquindio.cocinasinestres.perfil.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.uniquindio.cocinasinestres.comun.error.ConflictoException;
import co.edu.uniquindio.cocinasinestres.comun.error.DatosInvalidosException;
import co.edu.uniquindio.cocinasinestres.comun.error.RecursoNoEncontradoException;
import co.edu.uniquindio.cocinasinestres.perfil.domain.CuentaResumen;
import co.edu.uniquindio.cocinasinestres.perfil.domain.Rol;
import co.edu.uniquindio.cocinasinestres.perfil.model.Usuario;
import co.edu.uniquindio.cocinasinestres.perfil.model.UsuarioDePrueba;
import co.edu.uniquindio.cocinasinestres.perfil.repository.UsuarioRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

/** Gestión de cuentas y roles del administrador (ADR-16), con un doble del repositorio. */
@DisplayName("CuentasService")
class CuentasServiceTest {

    private static final long ADMIN = 1L;
    private static final long LUISA = 7L;

    private UsuarioRepository usuarioRepository;
    private CuentasService servicio;

    @BeforeEach
    void preparar() {
        usuarioRepository = mock(UsuarioRepository.class);
        servicio = new CuentasService(usuarioRepository);
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    @Test
    void listaTodasLasCuentasConSuRolYEstado() {
        when(usuarioRepository.findAll(any(Sort.class))).thenReturn(List.of(
                UsuarioDePrueba.conId(ADMIN, "uid-admin", "admin@ejemplo.com", Rol.ADMIN),
                UsuarioDePrueba.conId(LUISA, "uid-luisa", "luisa@ejemplo.com", Rol.USUARIA)));

        List<CuentaResumen> cuentas = servicio.listar();

        assertThat(cuentas).extracting(CuentaResumen::id).containsExactly(ADMIN, LUISA);
        assertThat(cuentas.get(1).rol()).isEqualTo(Rol.USUARIA);
        assertThat(cuentas.get(1).activo()).isTrue();
    }

    @Test
    void cambiaElRolDeOtraCuenta() {
        when(usuarioRepository.findById(LUISA))
                .thenReturn(Optional.of(UsuarioDePrueba.conId(LUISA, "uid-luisa", "luisa@ejemplo.com", Rol.USUARIA)));

        CuentaResumen resultado = servicio.cambiar(LUISA, "curador", null, ADMIN);

        assertThat(resultado.rol()).isEqualTo(Rol.CURADOR);
        assertThat(resultado.activo()).isTrue();
    }

    @Test
    void desactivaUnaCuentaSinCambiarSuRol() {
        when(usuarioRepository.findById(LUISA))
                .thenReturn(Optional.of(UsuarioDePrueba.conId(LUISA, "uid-luisa", "luisa@ejemplo.com", Rol.USUARIA)));

        CuentaResumen resultado = servicio.cambiar(LUISA, null, false, ADMIN);

        assertThat(resultado.activo()).isFalse();
        assertThat(resultado.rol()).isEqualTo(Rol.USUARIA);
    }

    @Test
    void rechazaUnRolQueNoExiste() {
        assertThatThrownBy(() -> servicio.cambiar(LUISA, "JEFE", null, ADMIN))
                .isInstanceOf(DatosInvalidosException.class)
                .extracting("codigo").isEqualTo(CuentasService.CODIGO_ROL_INVALIDO);
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void rechazaUnCambioVacio() {
        assertThatThrownBy(() -> servicio.cambiar(LUISA, null, null, ADMIN))
                .isInstanceOf(DatosInvalidosException.class)
                .extracting("codigo").isEqualTo(CuentasService.CODIGO_SIN_CAMBIOS);
    }

    @Test
    void elAdministradorNoPuedeCambiarseASiMismo() {
        assertThatThrownBy(() -> servicio.cambiar(ADMIN, "USUARIA", null, ADMIN))
                .isInstanceOf(ConflictoException.class)
                .extracting("codigo").isEqualTo(CuentasService.CODIGO_CUENTA_PROPIA);
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void unaCuentaQueNoExisteResponde404() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.cambiar(99L, "CURADOR", null, ADMIN))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }
}
