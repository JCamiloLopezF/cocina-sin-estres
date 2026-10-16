package co.edu.uniquindio.cocinasinestres.despensa.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("BusquedaTexto")
class BusquedaTextoTest {

    @Test
    void noDistingueTildesNiMayusculas() {
        assertThat(BusquedaTexto.coincide("Fríjol", "frijol")).isTrue();
        assertThat(BusquedaTexto.coincide("Azúcar", "AZUCAR")).isTrue();
        assertThat(BusquedaTexto.coincide("Jamón cocido", "jamon")).isTrue();
    }

    @Test
    void buscaEnCualquierParteDelNombre() {
        assertThat(BusquedaTexto.coincide("Leche entera", "entera")).isTrue();
        assertThat(BusquedaTexto.coincide("Leche entera", "che en")).isTrue();
    }

    @Test
    void ignoraEspaciosAlrededorDeLaConsulta() {
        assertThat(BusquedaTexto.coincide("Arroz", "  arroz ")).isTrue();
    }

    @Test
    void unaConsultaVaciaCoincideConTodo() {
        assertThat(BusquedaTexto.coincide("Arroz", null)).isTrue();
        assertThat(BusquedaTexto.coincide("Arroz", "")).isTrue();
        assertThat(BusquedaTexto.coincide("Arroz", "  ")).isTrue();
    }

    @Test
    void noCoincideSiElTextoNoLoContiene() {
        assertThat(BusquedaTexto.coincide("Arroz", "frijol")).isFalse();
        assertThat(BusquedaTexto.coincide(null, "arroz")).isFalse();
    }
}
