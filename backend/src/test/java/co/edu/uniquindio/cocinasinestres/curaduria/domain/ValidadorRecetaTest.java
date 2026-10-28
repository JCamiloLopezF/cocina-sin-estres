package co.edu.uniquindio.cocinasinestres.curaduria.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.edu.uniquindio.cocinasinestres.comun.error.DatosInvalidosException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Reglas de una receta del curador (RNF-28)")
class ValidadorRecetaTest {

    @Test
    @DisplayName("el nombre se limpia de espacios y tiene entre 3 y 120 caracteres")
    void nombre() {
        assertThat(ValidadorReceta.nombre("  Sudado   de pollo ")).isEqualTo("Sudado de pollo");
        assertThatThrownBy(() -> ValidadorReceta.nombre("   "))
            .hasFieldOrPropertyWithValue("codigo", "RECETA_NOMBRE_REQUERIDO");
        assertThatThrownBy(() -> ValidadorReceta.nombre("Té"))
            .hasFieldOrPropertyWithValue("codigo", "RECETA_NOMBRE_LARGO");
        assertThatThrownBy(() -> ValidadorReceta.nombre("a".repeat(121)))
            .hasFieldOrPropertyWithValue("codigo", "RECETA_NOMBRE_LARGO");
    }

    @Test
    @DisplayName("los pasos se limpian; un paso con dos tiempos dice cuál partir")
    void pasos() {
        assertThat(ValidadorReceta.pasos(Arrays.asList(" Pica la cebolla. ", "", null, "Cocina 10 minutos.")))
            .containsExactly("Pica la cebolla.", "Cocina 10 minutos.");
        assertThat(ValidadorReceta.pasos(List.of("Pica la cebolla\n y el tomate."))).containsExactly("Pica la cebolla y el tomate.");

        assertThatThrownBy(() -> ValidadorReceta.pasos(List.of("Pica.", "Sofríe 2 minutos y cocina 4 minutos.")))
            .isInstanceOf(DatosInvalidosException.class)
            .hasFieldOrPropertyWithValue("codigo", "RECETA_PASO_DOS_TIEMPOS")
            .hasMessageContaining("El paso 2");
        assertThatThrownBy(() -> ValidadorReceta.pasos(List.of("Cocina la pasta según el empaque.")))
            .hasFieldOrPropertyWithValue("codigo", "RECETA_PASO_AMBIGUO");
        assertThatThrownBy(() -> ValidadorReceta.pasos(List.of("Hornea 8 a 10 minutos.")))
            .hasFieldOrPropertyWithValue("codigo", "RECETA_PASO_AMBIGUO");
        assertThatThrownBy(() -> ValidadorReceta.pasos(List.of(" ")))
            .hasFieldOrPropertyWithValue("codigo", "RECETA_PASOS_REQUERIDOS");
    }

    @Test
    @DisplayName("las comidas se ordenan y al menos hay una")
    void momentos() {
        assertThat(ValidadorReceta.momentos(List.of("cena", "DESAYUNO"))).containsExactly("DESAYUNO", "CENA");
        assertThatThrownBy(() -> ValidadorReceta.momentos(List.of()))
            .hasFieldOrPropertyWithValue("codigo", "RECETA_MOMENTO_REQUERIDO");
        assertThatThrownBy(() -> ValidadorReceta.momentos(List.of("ONCES")))
            .hasFieldOrPropertyWithValue("codigo", "RECETA_MOMENTO_INVALIDO");
    }

    @Test
    @DisplayName("tiempo, porciones y cantidades dentro de lo que guarda la base")
    void numeros() {
        assertThat(ValidadorReceta.tiempo(45)).isEqualTo(45);
        assertThat(ValidadorReceta.porciones(4)).isEqualTo(4);
        assertThatThrownBy(() -> ValidadorReceta.tiempo(0)).hasFieldOrPropertyWithValue("codigo", "RECETA_TIEMPO_INVALIDO");
        assertThatThrownBy(() -> ValidadorReceta.tiempo(null)).hasFieldOrPropertyWithValue("codigo", "RECETA_TIEMPO_INVALIDO");
        assertThatThrownBy(() -> ValidadorReceta.porciones(21)).hasFieldOrPropertyWithValue("codigo", "RECETA_PORCIONES_INVALIDAS");
        assertThat(ValidadorReceta.cantidad(new BigDecimal("0.5"), "Repollo")).isEqualTo(new BigDecimal("0.5"));
        assertThatThrownBy(() -> ValidadorReceta.cantidad(new BigDecimal("0.0001"), "Sal"))
            .hasFieldOrPropertyWithValue("codigo", "RECETA_CANTIDAD_INVALIDA");
        assertThatThrownBy(() -> ValidadorReceta.cantidad(BigDecimal.ZERO, "Sal"))
            .hasFieldOrPropertyWithValue("codigo", "RECETA_CANTIDAD_INVALIDA");
    }

    @Test
    @DisplayName("si se baja el tiempo, el video no puede quedar más largo que la mitad")
    void video() {
        ValidadorReceta.videoCabe(null, null, 5);
        ValidadorReceta.videoCabe(0, 600, 20);
        assertThatThrownBy(() -> ValidadorReceta.videoCabe(0, 601, 20))
            .hasFieldOrPropertyWithValue("codigo", "RECETA_VIDEO_LARGO");
    }

    @Test
    @DisplayName("un almuerzo es un plato completo: proteína y algo que llene")
    void reglaAlmuerzo() {
        assertThat(ReglaAlmuerzo.esCompleto(List.of("PROTEINAS", "GRANOS", "VERDURAS"))).isTrue();
        assertThat(ReglaAlmuerzo.esCompleto(List.of("LEGUMBRES", "TUBERCULOS"))).isTrue();
        assertThat(ReglaAlmuerzo.esCompleto(List.of("PROTEINAS", "HARINAS"))).isTrue();
        // Huevos pericos: huevo, tomate y cebolla.
        assertThat(ReglaAlmuerzo.esCompleto(List.of("PROTEINAS", "VERDURAS", "VERDURAS"))).isFalse();
        // Pasta con queso.
        assertThat(ReglaAlmuerzo.esCompleto(List.of("GRANOS", "LACTEOS"))).isFalse();
    }
}
