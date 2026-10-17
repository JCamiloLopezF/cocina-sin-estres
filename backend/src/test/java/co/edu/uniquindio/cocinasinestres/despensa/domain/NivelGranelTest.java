package co.edu.uniquindio.cocinasinestres.despensa.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("NivelGranel")
class NivelGranelTest {

    private static final BigDecimal MIL = new BigDecimal("1000");

    @Test
    void cadaNivelEquivaleAUnaProporcionDeLaCapacidad() {
        assertThat(NivelGranel.LLENO.aCantidad(MIL)).isEqualByComparingTo("1000");
        assertThat(NivelGranel.MEDIO.aCantidad(MIL)).isEqualByComparingTo("500");
        assertThat(NivelGranel.POCO.aCantidad(MIL)).isEqualByComparingTo("250");
        assertThat(NivelGranel.AGOTADO.aCantidad(MIL)).isEqualByComparingTo("0");
    }

    @Test
    void laCantidadEstimadaSeEntregaConTresDecimales() {
        assertThat(NivelGranel.POCO.aCantidad(new BigDecimal("500")).scale()).isEqualTo(3);
        assertThat(NivelGranel.POCO.aCantidad(new BigDecimal("500"))).isEqualByComparingTo("125");
    }

    @Test
    void cantidadCeroEsAgotado() {
        assertThat(NivelGranel.desde(BigDecimal.ZERO, MIL)).isEqualTo(NivelGranel.AGOTADO);
    }

    @Test
    void cualquierCantidadPositivaEsAlMenosPoco() {
        assertThat(NivelGranel.desde(new BigDecimal("0.001"), MIL)).isEqualTo(NivelGranel.POCO);
    }

    @Test
    void elNivelEsElMasCercanoEntreLosCuatro() {
        assertThat(NivelGranel.desde(new BigDecimal("250"), MIL)).isEqualTo(NivelGranel.POCO);
        assertThat(NivelGranel.desde(new BigDecimal("375"), MIL)).isEqualTo(NivelGranel.POCO);
        assertThat(NivelGranel.desde(new BigDecimal("376"), MIL)).isEqualTo(NivelGranel.MEDIO);
        assertThat(NivelGranel.desde(new BigDecimal("500"), MIL)).isEqualTo(NivelGranel.MEDIO);
        assertThat(NivelGranel.desde(new BigDecimal("750"), MIL)).isEqualTo(NivelGranel.MEDIO);
        assertThat(NivelGranel.desde(new BigDecimal("751"), MIL)).isEqualTo(NivelGranel.LLENO);
        assertThat(NivelGranel.desde(MIL, MIL)).isEqualTo(NivelGranel.LLENO);
    }

    @Test
    void unaCantidadMayorQueLaCapacidadSigueSiendoLleno() {
        assertThat(NivelGranel.desde(new BigDecimal("1200"), MIL)).isEqualTo(NivelGranel.LLENO);
    }

    @Test
    void elNivelGuardadoVuelveASerElMismoNivelAlCalcularlo() {
        BigDecimal[] capacidades = {
            new BigDecimal("1"), new BigDecimal("333.333"), new BigDecimal("500"),
            new BigDecimal("750"), MIL, new BigDecimal("2500")
        };

        for (BigDecimal capacidad : capacidades) {
            for (NivelGranel nivel : NivelGranel.values()) {
                BigDecimal cantidad = nivel.aCantidad(capacidad);

                assertThat(NivelGranel.desde(cantidad, capacidad)).isEqualTo(nivel);
            }
        }
    }

    @Test
    void rechazaUnaCapacidadInvalida() {
        assertThatThrownBy(() -> NivelGranel.LLENO.aCantidad(null))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> NivelGranel.LLENO.aCantidad(BigDecimal.ZERO))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> NivelGranel.desde(BigDecimal.ONE, new BigDecimal("-5")))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
