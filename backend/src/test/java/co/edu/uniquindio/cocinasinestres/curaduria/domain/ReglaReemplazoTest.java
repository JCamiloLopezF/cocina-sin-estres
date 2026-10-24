package co.edu.uniquindio.cocinasinestres.curaduria.domain;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.uniquindio.cocinasinestres.curaduria.domain.ReglaReemplazo.Par;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Regla de reemplazos automáticos (ADR-24)")
class ReglaReemplazoTest {

    private static final long HUEVOS = 10, POLLO = 11, ATUN = 13, LECHE = 8, QUESO = 9, ARROZ = 1;

    private final Map<Long, String> categorias = Map.of(
        HUEVOS, "PROTEINAS", POLLO, "PROTEINAS", ATUN, "PROTEINAS",
        LECHE, "LACTEOS", QUESO, "LACTEOS", ARROZ, "GRANOS");
    private final Map<Long, Set<String>> alergenos = Map.of(
        HUEVOS, Set.of("huevo"), ATUN, Set.of("pescado"), LECHE, Set.of("lactosa"), QUESO, Set.of("lactosa"));

    @Test
    @DisplayName("propone productos de la misma categoría que no tengan el alérgeno")
    void mismaCategoriaSinAlergeno() {
        Set<Par> pares = ReglaReemplazo.proponer(ReglaReemplazo.catalogo(categorias, alergenos));

        assertThat(pares).contains(new Par(HUEVOS, POLLO), new Par(HUEVOS, ATUN), new Par(ATUN, HUEVOS), new Par(ATUN, POLLO));
        // Leche y queso tienen el mismo alérgeno: no se reemplazan entre ellos.
        assertThat(pares).doesNotContain(new Par(LECHE, QUESO), new Par(QUESO, LECHE));
        // De otra categoría, nunca.
        assertThat(pares).noneMatch(p -> p.reemplazoId() == ARROZ);
        // Un producto sin alérgenos no necesita reemplazo.
        assertThat(pares).noneMatch(p -> p.productoId() == POLLO || p.productoId() == ARROZ);
    }
}
