package co.edu.uniquindio.cocinasinestres.despensa.domain;

import static co.edu.uniquindio.cocinasinestres.despensa.ErroresDePrueba.codigoDelError;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.edu.uniquindio.cocinasinestres.curaduria.domain.TipoCantidad;
import co.edu.uniquindio.cocinasinestres.curaduria.domain.UnidadBase;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ValidadorItemDespensa")
class ValidadorItemDespensaTest {

    private static final LocalDate VENCE = LocalDate.of(2026, 10, 20);

    private static final ProductoCatalogo HUEVOS = new ProductoCatalogo(
        1L, "Huevos", "LACTEOS", TipoCantidad.CONTABLE, UnidadBase.unidad,
        null, "Cubeta", new BigDecimal("30"), new BigDecimal("19500"));

    private static final ProductoCatalogo ARROZ = new ProductoCatalogo(
        2L, "Arroz", "GRANOS", TipoCantidad.GRANEL, UnidadBase.g,
        new BigDecimal("1000"), "Bolsa", new BigDecimal("1000"), new BigDecimal("4500"));

    // ---------- Producto contable: cantidad numérica ----------

    @Test
    void contableGuardaLaCantidadExactaYNoLaMarcaComoEstimada() {
        DatosItemDespensa datos = ValidadorItemDespensa.validar(
            HUEVOS, new BigDecimal("12"), null, new BigDecimal("4"), null, VENCE, "Nevera");

        assertThat(datos.cantidad()).isEqualByComparingTo("12");
        assertThat(datos.esEstimada()).isFalse();
        assertThat(datos.cantidadMinima()).isEqualByComparingTo("4");
        assertThat(datos.fechaVencimiento()).isEqualTo(VENCE);
        assertThat(datos.ubicacion()).isEqualTo("Nevera");
    }

    @Test
    void contableNormalizaLasCantidadesATresDecimales() {
        DatosItemDespensa datos = ValidadorItemDespensa.validar(
            HUEVOS, new BigDecimal("1.50000"), null, new BigDecimal("2"), null, null, null);

        assertThat(datos.cantidad().scale()).isEqualTo(3);
        assertThat(datos.cantidadMinima().scale()).isEqualTo(3);
    }

    @Test
    void elMinimoYElVencimientoSonOpcionales() {
        DatosItemDespensa datos = ValidadorItemDespensa.validar(
            HUEVOS, new BigDecimal("6"), null, null, null, null, null);

        assertThat(datos.cantidadMinima()).isNull();
        assertThat(datos.fechaVencimiento()).isNull();
        assertThat(datos.ubicacion()).isNull();
    }

    @Test
    void contableAceptaCantidadCero() {
        DatosItemDespensa datos = ValidadorItemDespensa.validar(
            HUEVOS, BigDecimal.ZERO, null, null, null, null, null);

        assertThat(datos.cantidad()).isEqualByComparingTo("0");
    }

    @Test
    void contableExigeCantidad() {
        assertThat(codigoDelError(() -> ValidadorItemDespensa.validar(
            HUEVOS, null, null, null, null, null, null)))
            .isEqualTo(CodigoDespensa.CANTIDAD_REQUERIDA);
    }

    @Test
    void contableRechazaCantidadNegativa() {
        assertThat(codigoDelError(() -> ValidadorItemDespensa.validar(
            HUEVOS, new BigDecimal("-1"), null, null, null, null, null)))
            .isEqualTo(CodigoDespensa.CANTIDAD_INVALIDA);
    }

    @Test
    void contableRechazaMasDeTresDecimalesOMasDeNueveEnteros() {
        assertThat(codigoDelError(() -> ValidadorItemDespensa.validar(
            HUEVOS, new BigDecimal("1.0001"), null, null, null, null, null)))
            .isEqualTo(CodigoDespensa.CANTIDAD_INVALIDA);

        assertThat(codigoDelError(() -> ValidadorItemDespensa.validar(
            HUEVOS, new BigDecimal("1000000000"), null, null, null, null, null)))
            .isEqualTo(CodigoDespensa.CANTIDAD_INVALIDA);
    }

    @Test
    void contableRechazaUnMinimoNegativo() {
        assertThat(codigoDelError(() -> ValidadorItemDespensa.validar(
            HUEVOS, new BigDecimal("6"), null, new BigDecimal("-1"), null, null, null)))
            .isEqualTo(CodigoDespensa.MINIMO_INVALIDO);
    }

    @Test
    void contableNoAceptaNivelNiNivelMinimo() {
        assertThat(codigoDelError(() -> ValidadorItemDespensa.validar(
            HUEVOS, new BigDecimal("6"), NivelGranel.MEDIO, null, null, null, null)))
            .isEqualTo(CodigoDespensa.NIVEL_NO_PERMITIDO);

        assertThat(codigoDelError(() -> ValidadorItemDespensa.validar(
            HUEVOS, new BigDecimal("6"), null, null, NivelGranel.POCO, null, null)))
            .isEqualTo(CodigoDespensa.NIVEL_MINIMO_NO_PERMITIDO);
    }

    // ---------- Producto a granel: nivel ----------

    @Test
    void granelConvierteElNivelEnUnaCantidadEstimada() {
        DatosItemDespensa datos = ValidadorItemDespensa.validar(
            ARROZ, null, NivelGranel.MEDIO, null, NivelGranel.POCO, VENCE, "Alacena");

        assertThat(datos.cantidad()).isEqualByComparingTo("500");
        assertThat(datos.esEstimada()).isTrue();
        assertThat(datos.cantidadMinima()).isEqualByComparingTo("250");
        assertThat(datos.fechaVencimiento()).isEqualTo(VENCE);
        assertThat(datos.ubicacion()).isEqualTo("Alacena");
    }

    @Test
    void granelUsaLaCapacidadDeReferenciaDelProducto() {
        ProductoCatalogo aceite = new ProductoCatalogo(
            3L, "Aceite vegetal", "ACEITES", TipoCantidad.GRANEL, UnidadBase.ml,
            new BigDecimal("2000"), "Botella", new BigDecimal("2000"), new BigDecimal("16000"));

        DatosItemDespensa datos = ValidadorItemDespensa.validar(
            aceite, null, NivelGranel.POCO, null, null, null, null);

        assertThat(datos.cantidad()).isEqualByComparingTo("500");
    }

    @Test
    void granelPermiteElNivelAgotadoYUnMinimoAgotado() {
        DatosItemDespensa datos = ValidadorItemDespensa.validar(
            ARROZ, null, NivelGranel.AGOTADO, null, NivelGranel.AGOTADO, null, null);

        assertThat(datos.cantidad()).isEqualByComparingTo("0");
        assertThat(datos.cantidadMinima()).isEqualByComparingTo("0");
    }

    @Test
    void granelSinMinimoDejaElMinimoVacio() {
        DatosItemDespensa datos = ValidadorItemDespensa.validar(
            ARROZ, null, NivelGranel.LLENO, null, null, null, null);

        assertThat(datos.cantidadMinima()).isNull();
    }

    @Test
    void granelExigeNivel() {
        assertThat(codigoDelError(() -> ValidadorItemDespensa.validar(
            ARROZ, null, null, null, null, null, null)))
            .isEqualTo(CodigoDespensa.NIVEL_REQUERIDO);
    }

    @Test
    void granelNoAceptaCantidadNiMinimoNumerico() {
        assertThat(codigoDelError(() -> ValidadorItemDespensa.validar(
            ARROZ, new BigDecimal("500"), NivelGranel.MEDIO, null, null, null, null)))
            .isEqualTo(CodigoDespensa.CANTIDAD_NO_PERMITIDA);

        assertThat(codigoDelError(() -> ValidadorItemDespensa.validar(
            ARROZ, null, NivelGranel.MEDIO, new BigDecimal("200"), null, null, null)))
            .isEqualTo(CodigoDespensa.MINIMO_NO_PERMITIDO);
    }

    @Test
    void granelRechazaLlenoComoNivelMinimo() {
        assertThat(codigoDelError(() -> ValidadorItemDespensa.validar(
            ARROZ, null, NivelGranel.MEDIO, null, NivelGranel.LLENO, null, null)))
            .isEqualTo(CodigoDespensa.NIVEL_MINIMO_INVALIDO);
    }

    @Test
    void granelSinCapacidadEsUnDatoCorruptoDelCatalogoYNoUnErrorDeLaUsuaria() {
        ProductoCatalogo corrupto = new ProductoCatalogo(
            4L, "Lenteja", "GRANOS", TipoCantidad.GRANEL, UnidadBase.g,
            null, "Bolsa", new BigDecimal("500"), new BigDecimal("4000"));

        assertThatThrownBy(() -> ValidadorItemDespensa.validar(
            corrupto, null, NivelGranel.MEDIO, null, null, null, null))
            .isInstanceOf(IllegalStateException.class);
    }

    // ---------- Ubicación ----------

    @Test
    void laUbicacionSeLimpiaYUnaVaciaQuedaSinValor() {
        assertThat(ValidadorItemDespensa.validar(
            HUEVOS, BigDecimal.ONE, null, null, null, null, "  Nevera  ").ubicacion())
            .isEqualTo("Nevera");

        assertThat(ValidadorItemDespensa.validar(
            HUEVOS, BigDecimal.ONE, null, null, null, null, "   ").ubicacion())
            .isNull();
    }

    @Test
    void laUbicacionPuedeTenerHastaCuarentaCaracteres() {
        String cuarenta = "a".repeat(ValidadorItemDespensa.LONGITUD_MAXIMA_UBICACION);
        String cuarentaYUno = cuarenta + "a";

        assertThat(ValidadorItemDespensa.validar(
            HUEVOS, BigDecimal.ONE, null, null, null, null, cuarenta).ubicacion())
            .isEqualTo(cuarenta);

        assertThat(codigoDelError(() -> ValidadorItemDespensa.validar(
            HUEVOS, BigDecimal.ONE, null, null, null, null, cuarentaYUno)))
            .isEqualTo(CodigoDespensa.UBICACION_LARGA);
    }
}
