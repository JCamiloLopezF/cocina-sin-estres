package co.edu.uniquindio.cocinasinestres.comun.seguridad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import co.edu.uniquindio.cocinasinestres.comun.config.PropiedadesCocina;
import java.util.Base64;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Prueba del cifrado AES-256-GCM de los datos de salud (ADR-20, RNF-22). No levanta Spring.
 */
@DisplayName("CifradorDatosSensibles")
class CifradorDatosSensiblesTest {

    private static final String CLAVE = Base64.getEncoder().encodeToString(new byte[32]);
    private static final String TEXTO = "[\"lactosa\",\"maní\"]";

    private static CifradorDatosSensibles cifradorCon(String clave) {
        return new CifradorDatosSensibles(
            new PropiedadesCocina(null, new PropiedadesCocina.Cifrado(clave), null, null));
    }

    @Test
    void cifraYDescifraConservandoLasTildes() {
        CifradorDatosSensibles cifrador = cifradorCon(CLAVE);

        String cifrado = cifrador.cifrar(TEXTO);

        assertThat(cifrado).startsWith("v1:").doesNotContain("lactosa");
        assertThat(cifrador.descifrar(cifrado)).isEqualTo(TEXTO);
    }

    @Test
    void usaUnNonceDistintoEnCadaCifrado() {
        CifradorDatosSensibles cifrador = cifradorCon(CLAVE);

        assertThat(cifrador.cifrar(TEXTO)).isNotEqualTo(cifrador.cifrar(TEXTO));
    }

    @Test
    void nuloSigueSiendoNulo() {
        CifradorDatosSensibles cifrador = cifradorCon(CLAVE);

        assertThat(cifrador.cifrar(null)).isNull();
        assertThat(cifrador.descifrar(null)).isNull();
    }

    @Test
    void rechazaUnDatoAlterado() {
        CifradorDatosSensibles cifrador = cifradorCon(CLAVE);
        char[] cifrado = cifrador.cifrar(TEXTO).toCharArray();
        cifrado[10] = cifrado[10] == 'A' ? 'B' : 'A';

        assertThatIllegalArgumentException().isThrownBy(() -> cifrador.descifrar(new String(cifrado)));
    }

    @Test
    void rechazaDescifrarConOtraClave() {
        byte[] otra = new byte[32];
        otra[0] = 1;
        String cifrado = cifradorCon(CLAVE).cifrar(TEXTO);

        assertThatIllegalArgumentException().isThrownBy(
            () -> cifradorCon(Base64.getEncoder().encodeToString(otra)).descifrar(cifrado));
    }

    @Test
    void rechazaUnDatoSinVersion() {
        assertThatIllegalArgumentException().isThrownBy(() -> cifradorCon(CLAVE).descifrar("texto plano"));
    }

    @Test
    void noArrancaSinClaveOConUnaClaveDeOtroTamano() {
        assertThatIllegalStateException().isThrownBy(() -> cifradorCon(""));
        assertThatIllegalStateException().isThrownBy(() -> cifradorCon("no-es-base64!"));
        assertThatIllegalStateException().isThrownBy(
            () -> cifradorCon(Base64.getEncoder().encodeToString(new byte[16])));
    }
}
