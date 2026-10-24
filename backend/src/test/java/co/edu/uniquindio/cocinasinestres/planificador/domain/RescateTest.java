package co.edu.uniquindio.cocinasinestres.planificador.domain;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.uniquindio.cocinasinestres.planificador.domain.AdaptacionReceta.Estado;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Modo rescate: un imprevisto, no hay tiempo de ir a la tienda. ¿Qué se puede hacer hoy
 * con lo que hay, primero lo que está por dañarse?
 */
@DisplayName("Modo rescate: cocinar hoy sin comprar nada (KAN-25)")
class RescateTest {

    private static final long ARROZ = 1, LECHE = 2, POLLO = 3, HUEVOS = 10, ATUN = 13, PAPA = 14;
    private static final long ARROZ_CON_LECHE = 100, ARROZ_CON_POLLO = 101, PERICOS = 102, ATUN_CON_ARROZ = 103,
        PAPAS_FRITAS = 104, BATIDO = 105;
    private static final LocalDate HOY = LocalDate.of(2026, 10, 24);

    private final Rescate rescate = new Rescate();

    private static RecetaCandidata receta(long id, int minutos, Map<Long, Integer> ingredientes, Set<String> alergias,
        Set<Long> principales, boolean platoFuerte) {
        Map<Long, BigDecimal> cantidades = new HashMap<>();
        ingredientes.forEach((p, c) -> cantidades.put(p, BigDecimal.valueOf(c)));
        return new RecetaCandidata(id, minutos, BigDecimal.valueOf(5000), cantidades, alergias, Set.of(),
            true, platoFuerte, false, principales);
    }

    private static Map<Long, RecetaCandidata> recetas() {
        return Map.of(
            ARROZ_CON_LECHE, receta(ARROZ_CON_LECHE, 40, Map.of(ARROZ, 200, LECHE, 1000), Set.of("lactosa"), Set.of(ARROZ, LECHE), false),
            ARROZ_CON_POLLO, receta(ARROZ_CON_POLLO, 45, Map.of(ARROZ, 500, POLLO, 800), Set.of(), Set.of(POLLO), true),
            PERICOS, receta(PERICOS, 15, Map.of(HUEVOS, 6), Set.of("huevo"), Set.of(HUEVOS), true),
            ATUN_CON_ARROZ, receta(ATUN_CON_ARROZ, 20, Map.of(ATUN, 2, ARROZ, 300), Set.of("pescado"), Set.of(ATUN), true),
            PAPAS_FRITAS, receta(PAPAS_FRITAS, 25, Map.of(PAPA, 1000), Set.of(), Set.of(PAPA), true),
            BATIDO, receta(BATIDO, 5, Map.of(LECHE, 300), Set.of("lactosa"), Set.of(LECHE), false));
    }

    /** Hay arroz, leche, pollo, 6 huevos y atún; las papas no alcanzan. */
    private static Map<Long, BigDecimal> despensa() {
        return Map.of(
            ARROZ, BigDecimal.valueOf(1000), LECHE, BigDecimal.valueOf(1000), POLLO, BigDecimal.valueOf(800),
            HUEVOS, BigDecimal.valueOf(6), ATUN, BigDecimal.valueOf(2), PAPA, BigDecimal.valueOf(500));
    }

    private static ContextoPlan contexto(ReglasAlergia reglas) {
        return new ContextoPlan(recetas(), Set.of(), Set.of(), BigDecimal.ZERO, despensa(), Set.of(), Map.of(), false, reglas);
    }

    @Test
    @DisplayName("solo propone lo que alcanza con la despensa")
    void soloLoQueAlcanza() {
        List<Rescate.Opcion> opciones = rescate.opciones(contexto(ReglasAlergia.SIN_DETALLE), Map.of(), null);

        assertThat(opciones).extracting(Rescate.Opcion::recetaId)
            .doesNotContain(PAPAS_FRITAS)
            .contains(ARROZ_CON_POLLO, PERICOS, ATUN_CON_ARROZ, ARROZ_CON_LECHE, BATIDO);
    }

    @Test
    @DisplayName("primero lo que salva el producto que vence antes; sin vencimientos, platos fuertes y rápidos")
    void ordenPorVencimiento() {
        Map<Long, LocalDate> vencen = Map.of(POLLO, HOY.plusDays(2), LECHE, HOY.plusDays(1));

        List<Rescate.Opcion> opciones = rescate.opciones(contexto(ReglasAlergia.SIN_DETALLE), vencen, null);

        // La leche vence primero: arroz con leche y batido; el arroz con leche usa también el arroz,
        // pero solo cuentan los productos por vencer, así que el desempate es plato fuerte (ninguno)
        // y luego el tiempo: batido (5 min) antes que arroz con leche (40 min).
        assertThat(opciones).extracting(Rescate.Opcion::recetaId)
            .startsWith(BATIDO, ARROZ_CON_LECHE, ARROZ_CON_POLLO);
        assertThat(opciones.get(0).aprovecha()).containsExactly(LECHE);
        assertThat(opciones.get(0).primerVencimiento()).isEqualTo(HOY.plusDays(1));
        assertThat(opciones.get(2).aprovecha()).containsExactly(POLLO);
        // Las que no usan nada por vencer van al final: plato fuerte y más rápido primero.
        assertThat(opciones.subList(3, opciones.size())).extracting(Rescate.Opcion::recetaId)
            .containsExactly(PERICOS, ATUN_CON_ARROZ);
        assertThat(opciones.get(3).primerVencimiento()).isNull();
    }

    @Test
    @DisplayName("con poco tiempo, solo las recetas rápidas")
    void conPocoTiempo() {
        List<Rescate.Opcion> opciones = rescate.opciones(contexto(ReglasAlergia.SIN_DETALLE), Map.of(), 20);

        assertThat(opciones).extracting(Rescate.Opcion::recetaId).containsExactlyInAnyOrder(PERICOS, ATUN_CON_ARROZ, BATIDO);
    }

    @Test
    @DisplayName("una alergia grave descarta la receta; una leve se adapta solo si el reemplazo está en la despensa")
    void alergias() {
        ReglasAlergia reglas = new ReglasAlergia(
            List.of(
                new Comensal("Luisa", Set.of(), Set.of()),
                new Comensal("Samuel", Set.of("huevo", "lactosa"), Set.of("lactosa"))),
            Map.of(HUEVOS, Set.of("huevo"), LECHE, Set.of("lactosa")),
            Map.of(HUEVOS, Set.of(POLLO)));

        List<Rescate.Opcion> opciones = rescate.opciones(contexto(reglas), Map.of(LECHE, HOY), null);

        // Samuel es alérgico grave a la lactosa: nada con leche, aunque la leche vence hoy.
        assertThat(opciones).extracting(Rescate.Opcion::recetaId).doesNotContain(ARROZ_CON_LECHE, BATIDO);
        // Los pericos tienen el huevo como principal: a Samuel le toca plato aparte, y el
        // plato aparte (arroz con pollo o atún con arroz) también sale de la despensa.
        Rescate.Opcion pericos = opciones.stream().filter(o -> o.recetaId() == PERICOS).findFirst().orElseThrow();
        assertThat(pericos.adaptacion().estado()).isEqualTo(Estado.PLATO_APARTE);
        // Entre iguales, primero las que todos comen igual.
        assertThat(opciones).extracting(Rescate.Opcion::recetaId).startsWith(ATUN_CON_ARROZ);
    }

    @Test
    @DisplayName("si el reemplazo no está en la despensa, no es sin comprar")
    void reemplazoQueNoHay() {
        RecetaCandidata arrozConHuevo = receta(200, 20, Map.of(ARROZ, 200, HUEVOS, 2), Set.of("huevo"), Set.of(ARROZ), true);
        ReglasAlergia reglas = new ReglasAlergia(
            List.of(new Comensal("Samuel", Set.of("huevo"), Set.of())),
            Map.of(HUEVOS, Set.of("huevo")),
            Map.of(HUEVOS, Set.of(99L)));
        ContextoPlan contexto = new ContextoPlan(Map.of(200L, arrozConHuevo), Set.of("huevo"), Set.of(), BigDecimal.ZERO,
            despensa(), Set.of(), Map.of(), false, reglas);

        assertThat(rescate.opciones(contexto, Map.of(), null)).isEmpty();

        ContextoPlan conReemplazo = new ContextoPlan(Map.of(200L, arrozConHuevo), Set.of("huevo"), Set.of(), BigDecimal.ZERO,
            despensa(), Set.of(), Map.of(), false, new ReglasAlergia(reglas.comensales(), reglas.alergenos(),
                Map.of(HUEVOS, Set.of(POLLO))));
        assertThat(rescate.opciones(conReemplazo, Map.of(), null)).singleElement()
            .satisfies(o -> assertThat(o.adaptacion().estado()).isEqualTo(Estado.ADAPTADA));
    }

    @Test
    @DisplayName("una receta inactiva no se propone y la lista tiene tope")
    void inactivasYTope() {
        RecetaCandidata inactiva = new RecetaCandidata(300L, 10, BigDecimal.ONE, Map.of(), Set.of(), Set.of(),
            false, true, false);
        ContextoPlan contexto = new ContextoPlan(Map.of(300L, inactiva), Set.of(), Set.of(), BigDecimal.ZERO,
            Map.of(), Set.of(), Map.of());
        assertThat(rescate.opciones(contexto, null, null)).isEmpty();
        assertThat(Rescate.MAX_OPCIONES).isPositive();
        assertThat(Rescate.alcanza(Map.of(ARROZ, BigDecimal.TEN), Map.of())).isFalse();
    }
}
