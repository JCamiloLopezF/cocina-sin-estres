package co.edu.uniquindio.cocinasinestres.curaduria.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Categorías y nombres del catálogo (RNF-24)")
class CategoriaProductoTest {

    @Test
    @DisplayName("solo acepta las categorías de la app, sin importar mayúsculas ni tildes")
    void categorias() {
        assertThat(CategoriaProducto.normalizar(" granos ")).isEqualTo("GRANOS");
        assertThat(CategoriaProducto.normalizar("Tubérculos")).isEqualTo("TUBERCULOS");
        assertThat(CategoriaProducto.normalizar("Hackeo")).isNull();
        assertThat(CategoriaProducto.normalizar("Granos y básicos")).isNull();
        assertThat(CategoriaProducto.normalizar(null)).isNull();
    }

    @Test
    @DisplayName("«  ARROZ » y «Arroz» son el mismo nombre")
    void nombres() {
        assertThat(CategoriaProducto.claveNombre("  ARROZ ")).isEqualTo(CategoriaProducto.claveNombre("Arroz"));
        assertThat(CategoriaProducto.claveNombre("Limón")).isEqualTo(CategoriaProducto.claveNombre("limon"));
        assertThat(CategoriaProducto.limpiarNombre("  Papa   criolla ")).isEqualTo("Papa criolla");
    }

    @Test
    @DisplayName("el validador rechaza una categoría inventada y un contable en gramos")
    void validadorProducto() {
        BigDecimal cien = new BigDecimal("100");
        ValidadorProducto.validar("Yuca", "TUBERCULOS", TipoCantidad.GRANEL, UnidadBase.g, "Libra", new BigDecimal("500"),
            new BigDecimal("2500"), new BigDecimal("500"));

        assertThatThrownBy(() -> ValidadorProducto.validar("Algo", "Hackeo", TipoCantidad.CONTABLE, UnidadBase.unidad,
            "Unidad", BigDecimal.ONE, cien, null))
            .hasFieldOrPropertyWithValue("codigo", "PRODUCTO_CATEGORIA_INVALIDA");
        assertThatThrownBy(() -> ValidadorProducto.validar("Algo", "GRANOS", TipoCantidad.CONTABLE, UnidadBase.g,
            "Unidad", BigDecimal.ONE, cien, null))
            .hasFieldOrPropertyWithValue("codigo", "PRODUCTO_UNIDAD_INVALIDA");
        assertThatThrownBy(() -> ValidadorProducto.validar("Algo", "GRANOS", TipoCantidad.GRANEL, UnidadBase.unidad,
            "Bolsa", BigDecimal.ONE, cien, cien))
            .hasFieldOrPropertyWithValue("codigo", "PRODUCTO_UNIDAD_INVALIDA");
    }
}
