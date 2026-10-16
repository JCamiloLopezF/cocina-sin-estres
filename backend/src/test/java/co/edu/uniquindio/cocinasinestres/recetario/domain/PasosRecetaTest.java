package co.edu.uniquindio.cocinasinestres.recetario.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Prueba del formato de los pasos de una receta. Dominio puro: no levanta Spring. */
@DisplayName("PasosReceta")
class PasosRecetaTest {

    @Test
    void divideUnPasoPorLineaSinEspaciosNiLineasVacias() {
        String texto = "  Pica la cebolla.  \n\nSofríe 5 minutos.\r\n   \nSirve.";

        assertThat(PasosReceta.dividir(texto))
            .containsExactly("Pica la cebolla.", "Sofríe 5 minutos.", "Sirve.");
    }

    @Test
    void textoNuloOVacioDaListaVacia() {
        assertThat(PasosReceta.dividir(null)).isEmpty();
        assertThat(PasosReceta.dividir("  \n ")).isEmpty();
    }

    @Test
    void unirDejaUnPasoPorLineaEIgnoraVaciosYNulos() {
        assertThat(PasosReceta.unir(Arrays.asList(" Pica. ", "", null, "Sirve.")))
            .isEqualTo("Pica.\nSirve.");
    }

    @Test
    void unirYDividirSonInversas() {
        List<String> pasos = List.of("Pica la cebolla.", "Sofríe 5 minutos.", "Sirve.");

        assertThat(PasosReceta.dividir(PasosReceta.unir(pasos))).isEqualTo(pasos);
    }

    @Test
    void unirRechazaListaNula() {
        assertThatNullPointerException().isThrownBy(() -> PasosReceta.unir(null));
    }
}
