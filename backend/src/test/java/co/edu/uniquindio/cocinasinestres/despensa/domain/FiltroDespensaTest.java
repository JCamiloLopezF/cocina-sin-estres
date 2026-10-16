package co.edu.uniquindio.cocinasinestres.despensa.domain;

import static co.edu.uniquindio.cocinasinestres.despensa.ErroresDePrueba.codigoDelError;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("FiltroDespensa")
class FiltroDespensaTest {

    @Test
    void sinFiltroSeMuestraTodo() {
        assertThat(FiltroDespensa.desde(null)).isEqualTo(FiltroDespensa.TODO);
        assertThat(FiltroDespensa.desde("")).isEqualTo(FiltroDespensa.TODO);
        assertThat(FiltroDespensa.desde("   ")).isEqualTo(FiltroDespensa.TODO);
    }

    @Test
    void noDistingueMayusculasNiEspaciosAlrededor() {
        assertThat(FiltroDespensa.desde("por_vencer")).isEqualTo(FiltroDespensa.POR_VENCER);
        assertThat(FiltroDespensa.desde(" BAJO_MINIMO ")).isEqualTo(FiltroDespensa.BAJO_MINIMO);
    }

    @Test
    void unFiltroDesconocidoResponde400ConCodigoEstable() {
        assertThat(codigoDelError(() -> FiltroDespensa.desde("proximos")))
            .isEqualTo(CodigoDespensa.FILTRO_INVALIDO);
    }
}
