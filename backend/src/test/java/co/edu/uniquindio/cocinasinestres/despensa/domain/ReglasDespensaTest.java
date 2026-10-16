package co.edu.uniquindio.cocinasinestres.despensa.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ReglasDespensa")
class ReglasDespensaTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 14);

    @Test
    void sinFechaNoHayEstadoDeVencimiento() {
        assertThat(ReglasDespensa.estadoVencimiento(null, HOY)).isEqualTo(EstadoVencimiento.SIN_FECHA);
        assertThat(ReglasDespensa.diasParaVencer(null, HOY)).isNull();
    }

    @Test
    void unProductoQueVenceHoyTodaviaSePuedeUsarHoy() {
        assertThat(ReglasDespensa.estadoVencimiento(HOY, HOY)).isEqualTo(EstadoVencimiento.POR_VENCER);
        assertThat(ReglasDespensa.diasParaVencer(HOY, HOY)).isEqualTo(0);
    }

    @Test
    void estaVencidoDesdeElDiaSiguienteALaFecha() {
        LocalDate ayer = HOY.minusDays(1);

        assertThat(ReglasDespensa.estadoVencimiento(ayer, HOY)).isEqualTo(EstadoVencimiento.VENCIDO);
        assertThat(ReglasDespensa.diasParaVencer(ayer, HOY)).isEqualTo(-1);
    }

    @Test
    void esPorVencerHastaSieteDiasInclusive() {
        assertThat(ReglasDespensa.estadoVencimiento(HOY.plusDays(7), HOY))
            .isEqualTo(EstadoVencimiento.POR_VENCER);
        assertThat(ReglasDespensa.estadoVencimiento(HOY.plusDays(8), HOY))
            .isEqualTo(EstadoVencimiento.VIGENTE);
    }

    @Test
    void cuentaLosDiasQueFaltanParaVencer() {
        assertThat(ReglasDespensa.diasParaVencer(HOY.plusDays(2), HOY)).isEqualTo(2);
        assertThat(ReglasDespensa.diasParaVencer(HOY.plusMonths(1), HOY)).isEqualTo(31);
    }

    @Test
    void sinMinimoNuncaEstaBajoElMinimo() {
        assertThat(ReglasDespensa.estaBajoMinimo(BigDecimal.ZERO, null)).isFalse();
    }

    @Test
    void estaBajoElMinimoAlAlcanzarloOAlQuedarPorDebajo() {
        BigDecimal minimo = new BigDecimal("4");

        assertThat(ReglasDespensa.estaBajoMinimo(new BigDecimal("6"), minimo)).isFalse();
        assertThat(ReglasDespensa.estaBajoMinimo(new BigDecimal("4"), minimo)).isTrue();
        assertThat(ReglasDespensa.estaBajoMinimo(new BigDecimal("3"), minimo)).isTrue();
    }

    @Test
    void laComparacionNoDependeDeLaEscalaDeLosDecimales() {
        assertThat(ReglasDespensa.estaBajoMinimo(new BigDecimal("4.000"), new BigDecimal("4"))).isTrue();
        assertThat(ReglasDespensa.estaBajoMinimo(new BigDecimal("4.001"), new BigDecimal("4"))).isFalse();
    }

    @Test
    void unMinimoDeCeroAvisaCuandoSeAgota() {
        assertThat(ReglasDespensa.estaBajoMinimo(BigDecimal.ZERO, BigDecimal.ZERO)).isTrue();
        assertThat(ReglasDespensa.estaBajoMinimo(BigDecimal.ONE, BigDecimal.ZERO)).isFalse();
    }

    @Test
    void todoMuestraTodosLosProductos() {
        for (EstadoVencimiento estado : EstadoVencimiento.values()) {
            assertThat(ReglasDespensa.cumpleFiltro(FiltroDespensa.TODO, estado, false)).isTrue();
        }
    }

    @Test
    void porVencerIncluyeLosYaVencidosPeroNoLosVigentesNiLosSinFecha() {
        assertThat(ReglasDespensa.cumpleFiltro(FiltroDespensa.POR_VENCER, EstadoVencimiento.POR_VENCER, false))
            .isTrue();
        assertThat(ReglasDespensa.cumpleFiltro(FiltroDespensa.POR_VENCER, EstadoVencimiento.VENCIDO, false))
            .isTrue();
        assertThat(ReglasDespensa.cumpleFiltro(FiltroDespensa.POR_VENCER, EstadoVencimiento.VIGENTE, true))
            .isFalse();
        assertThat(ReglasDespensa.cumpleFiltro(FiltroDespensa.POR_VENCER, EstadoVencimiento.SIN_FECHA, true))
            .isFalse();
    }

    @Test
    void bajoMinimoSoloMuestraLosQueLlegaronAlMinimo() {
        assertThat(ReglasDespensa.cumpleFiltro(FiltroDespensa.BAJO_MINIMO, EstadoVencimiento.VIGENTE, true))
            .isTrue();
        assertThat(ReglasDespensa.cumpleFiltro(FiltroDespensa.BAJO_MINIMO, EstadoVencimiento.POR_VENCER, false))
            .isFalse();
    }
}
