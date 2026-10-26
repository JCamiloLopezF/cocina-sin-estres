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

    @Test
    void encuentraLosTiemposQueEntiendeElTemporizador() {
        assertThat(PasosReceta.tiemposDelPaso("Cocina tapado 20 minutos.")).containsExactly("20 minutos");
        assertThat(PasosReceta.tiemposDelPaso("Hierve de 8 a 10 minutos.")).containsExactly("8 a 10 minutos");
        assertThat(PasosReceta.tiemposDelPaso("Revuelve un minuto.")).containsExactly("un minuto");
        assertThat(PasosReceta.tiemposDelPaso("Deja reposar media hora.")).containsExactly("media hora");
        assertThat(PasosReceta.tiemposDelPaso("Pica la cebolla.")).isEmpty();
        assertThat(PasosReceta.tiemposDelPaso(null)).isEmpty();
    }

    @Test
    void detectaUnPasoConDosTiempos() {
        String paso = "Sofríe la cebolla 2 minutos, añade el tomate y cocina 4 minutos.";

        assertThat(PasosReceta.tiemposDelPaso(paso)).containsExactly("2 minutos", "4 minutos");
    }
}
