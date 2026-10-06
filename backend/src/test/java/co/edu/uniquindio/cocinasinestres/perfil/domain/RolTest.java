package co.edu.uniquindio.cocinasinestres.perfil.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Prueba del enum de roles (ADR-16). Dominio puro: no levanta el contexto de Spring.
 */
@DisplayName("Rol")
class RolTest {

    @Test
    void laAutoridadLlevaElPrefijoQuePideLaCapaDeSeguridad() {
        assertThat(Rol.USUARIA.autoridad()).isEqualTo("ROLE_USUARIA");
        assertThat(Rol.CURADOR.autoridad()).isEqualTo("ROLE_CURADOR");
        assertThat(Rol.ADMIN.autoridad()).isEqualTo("ROLE_ADMIN");
    }

    @Test
    void soloElCuradorYElAdminPuedenEditarElCatalogo() {
        assertThat(Rol.CURADOR.puedeCurar()).isTrue();
        assertThat(Rol.ADMIN.puedeCurar()).isTrue();
        assertThat(Rol.USUARIA.puedeCurar()).isFalse();
    }

    @Test
    void soloElAdminPuedeGestionarCuentas() {
        assertThat(Rol.ADMIN.puedeAdministrar()).isTrue();
        assertThat(Rol.CURADOR.puedeAdministrar()).isFalse();
        assertThat(Rol.USUARIA.puedeAdministrar()).isFalse();
    }

    @Test
    void unaCuentaNuevaEntraComoUsuaria() {
        assertThat(Rol.PREDETERMINADO).isEqualTo(Rol.USUARIA);
    }

    @Test
    void losNombresCoincidenConLaRestriccionDeLaBase() {
        // ck_usuario_rol en V1__esquema_inicial.sql acepta exactamente estos tres textos.
        assertThat(Rol.values()).extracting(Enum::name)
                .containsExactly("USUARIA", "CURADOR", "ADMIN");
    }
}
