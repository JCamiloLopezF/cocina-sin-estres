package co.edu.uniquindio.cocinasinestres.planificador.domain;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.uniquindio.cocinasinestres.planificador.domain.AdaptacionReceta.Cambio;
import co.edu.uniquindio.cocinasinestres.planificador.domain.AdaptacionReceta.Estado;
import co.edu.uniquindio.cocinasinestres.planificador.domain.AdaptacionReceta.TipoCambio;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * El caso del profesor: Samuel es alérgico al huevo. La mamá no deja de hacer el arroz
 * con huevo para todos; a Samuel le cambia el huevo por el pollo que tiene en la nevera.
 */
@DisplayName("Adaptar recetas a las alergias de cada persona (ADR-24)")
class AdaptadorAlergiasTest {

    private static final long ARROZ = 1, HUEVOS = 10, POLLO = 11, ATUN = 13, QUESO = 9;
    private static final long ARROZ_CON_HUEVO = 100, PERICOS = 101, ATUN_CON_ARROZ = 102, REVUELTOS_CON_QUESO = 103;

    private final AdaptadorAlergias adaptador = new AdaptadorAlergias();

    private static RecetaCandidata receta(long id, int minutos, long costo, Set<Long> productos, Set<String> alergias, Set<Long> principales) {
        Map<Long, BigDecimal> ingredientes = new HashMap<>();
        productos.forEach(p -> ingredientes.put(p, BigDecimal.ONE));
        return new RecetaCandidata(id, minutos, BigDecimal.valueOf(costo), ingredientes, alergias, Set.of(),
            true, true, false, principales);
    }

    private static ContextoPlan contexto(Comensal... personas) {
        Map<Long, RecetaCandidata> recetas = Map.of(
            ARROZ_CON_HUEVO, receta(ARROZ_CON_HUEVO, 25, 4000, Set.of(ARROZ, HUEVOS), Set.of("huevo"), Set.of(ARROZ)),
            PERICOS, receta(PERICOS, 15, 4100, Set.of(HUEVOS), Set.of("huevo"), Set.of(HUEVOS)),
            ATUN_CON_ARROZ, receta(ATUN_CON_ARROZ, 20, 6200, Set.of(ATUN, ARROZ), Set.of("pescado"), Set.of(ATUN, ARROZ)),
            REVUELTOS_CON_QUESO, receta(REVUELTOS_CON_QUESO, 10, 5000, Set.of(HUEVOS, QUESO), Set.of("huevo", "lactosa"), Set.of(HUEVOS)));
        ReglasAlergia reglas = new ReglasAlergia(
            List.of(personas),
            Map.of(HUEVOS, Set.of("huevo"), ATUN, Set.of("pescado"), QUESO, Set.of("lactosa")),
            Map.of(HUEVOS, Set.of(POLLO, ATUN), ATUN, Set.of(HUEVOS, POLLO)));
        return new ContextoPlan(recetas, Set.of("huevo"), Set.of(), BigDecimal.valueOf(200_000),
            Map.of(POLLO, BigDecimal.valueOf(500)), Set.of(), Map.of(), false, reglas);
    }

    private static final Comensal LUISA = new Comensal("Luisa", Set.of(), Set.of());
    private static final Comensal SAMUEL = new Comensal("Samuel", Set.of("huevo"), Set.of());

    @Test
    @DisplayName("si el ingrediente no es el principal, se cambia en su porción, primero por lo que hay en la despensa")
    void reemplazaConLoQueHay() {
        ContextoPlan contexto = contexto(LUISA, SAMUEL);

        AdaptacionReceta adaptacion = adaptador.adaptar(contexto, contexto.recetas().get(ARROZ_CON_HUEVO));

        assertThat(adaptacion.estado()).isEqualTo(Estado.ADAPTADA);
        assertThat(adaptacion.cambios()).containsExactly(
            new Cambio(TipoCambio.REEMPLAZO, "Samuel", Set.of("huevo"), HUEVOS, POLLO, true, null));
    }

    @Test
    @DisplayName("si es el principal, le propone otro plato que sí pueda comer")
    void platoAparte() {
        ContextoPlan contexto = contexto(LUISA, SAMUEL);

        AdaptacionReceta adaptacion = adaptador.adaptar(contexto, contexto.recetas().get(PERICOS));

        assertThat(adaptacion.estado()).isEqualTo(Estado.PLATO_APARTE);
        assertThat(adaptacion.cambios()).singleElement().satisfies(cambio -> {
            assertThat(cambio.tipo()).isEqualTo(TipoCambio.PLATO_APARTE);
            assertThat(cambio.persona()).isEqualTo("Samuel");
            assertThat(cambio.recetaAparteId()).isEqualTo(ATUN_CON_ARROZ);
        });
    }

    @Test
    @DisplayName("si no hay reemplazo para un ingrediente secundario, su porción va sin él")
    void sinElIngrediente() {
        Comensal ana = new Comensal("Ana", Set.of("lactosa"), Set.of());
        ContextoPlan contexto = contexto(ana);

        AdaptacionReceta adaptacion = adaptador.adaptar(contexto, contexto.recetas().get(REVUELTOS_CON_QUESO));

        assertThat(adaptacion.cambios()).containsExactly(
            new Cambio(TipoCambio.SIN, "Ana", Set.of("lactosa"), QUESO, null, false, null));
    }

    @Test
    @DisplayName("una alergia grave descarta la receta para toda la casa")
    void alergiaGrave() {
        Comensal samuelGrave = new Comensal("Samuel", Set.of("huevo"), Set.of("huevo"));
        ContextoPlan contexto = contexto(LUISA, samuelGrave);

        assertThat(adaptador.adaptar(contexto, contexto.recetas().get(ARROZ_CON_HUEVO)).estado()).isEqualTo(Estado.NO_APTA);
        assertThat(adaptador.adaptar(contexto, contexto.recetas().get(ATUN_CON_ARROZ)).estado()).isEqualTo(Estado.APTA);
    }

    @Test
    @DisplayName("el reemplazo tampoco puede tener las alergias de esa persona")
    void reemplazoSeguro() {
        Comensal pedro = new Comensal("Pedro", Set.of("huevo", "pescado"), Set.of());
        ContextoPlan contexto = contexto(pedro);

        assertThat(adaptador.adaptar(contexto, contexto.recetas().get(ARROZ_CON_HUEVO)).cambios())
            .extracting(Cambio::reemplazoId).containsExactly(POLLO);
    }

    @Test
    @DisplayName("el generador prefiere lo que todos comen, luego lo adaptado y por último el plato aparte")
    void prioridadEnElPlan() {
        ContextoPlan contexto = contexto(LUISA, SAMUEL);
        GeneradorPlan generador = new GeneradorPlan();

        Map<Hueco, Long> plan = generador.completar(contexto, List.of(
            new Hueco(1, "ALMUERZO"), new Hueco(1, "CENA"), new Hueco(2, "ALMUERZO")));

        // Atún con arroz (todos), arroz con huevo (cambio para Samuel) y por último los pericos.
        assertThat(plan.values()).containsExactly(ATUN_CON_ARROZ, ARROZ_CON_HUEVO, PERICOS);
        assertThat(plan.values()).doesNotContain(REVUELTOS_CON_QUESO);
    }
}
