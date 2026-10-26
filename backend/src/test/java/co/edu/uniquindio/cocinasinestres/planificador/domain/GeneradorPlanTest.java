package co.edu.uniquindio.cocinasinestres.planificador.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Generador del plan semanal (ADR-24)")
class GeneradorPlanTest {

    private static final long ARROZ = 1L;
    private static final long LECHE = 2L;
    private static final long POLLO = 3L;
    private static final long HUEVO = 4L;

    private final GeneradorPlan generador = new GeneradorPlan();

    /** Doce platos fuertes de 40 minutos y $5.000, sin restricciones, numerados del 101 al 112. */
    private static Map<Long, RecetaCandidata> catalogoBase() {
        Map<Long, RecetaCandidata> recetas = new HashMap<>();
        for (long id = 101; id <= 112; id++) {
            recetas.put(id, receta(id, 40, 5000, Map.of(ARROZ, 200), Set.of(), Set.of()));
        }
        return recetas;
    }

    private static RecetaCandidata receta(
        long id, int minutos, long costo, Map<Long, Integer> ingredientes, Set<String> alergias, Set<String> condiciones) {
        Map<Long, BigDecimal> cantidades = new HashMap<>();
        ingredientes.forEach((p, c) -> cantidades.put(p, BigDecimal.valueOf(c)));
        return new RecetaCandidata(id, minutos, BigDecimal.valueOf(costo), cantidades, alergias, condiciones, true, true, false);
    }

    private static ContextoPlan contexto(Map<Long, RecetaCandidata> recetas) {
        return new ContextoPlan(recetas, Set.of(), Set.of(), BigDecimal.valueOf(200_000), Map.of(), Set.of(), Map.of());
    }

    private static List<Hueco> semana() {
        List<Hueco> huecos = new ArrayList<>();
        for (int dia = 1; dia <= 7; dia++) {
            huecos.add(new Hueco(dia, "ALMUERZO"));
            huecos.add(new Hueco(dia, "CENA"));
        }
        return huecos;
    }

    @Test
    @DisplayName("llena el almuerzo y la cena de los siete días")
    void llenaLaSemana() {
        Map<Hueco, Long> plan = generador.completar(contexto(catalogoBase()), semana());

        assertThat(plan).hasSize(14);
        assertThat(plan.keySet()).containsExactlyElementsOf(semana());
    }

    @Test
    @DisplayName("nunca propone una receta con un ingrediente al que alguien es alérgico")
    void respetaAlergias() {
        Map<Long, RecetaCandidata> recetas = catalogoBase();
        // La más barata y con todo en la despensa, pero lleva leche.
        recetas.put(200L, receta(200, 10, 500, Map.of(LECHE, 100), Set.of("lactosa"), Set.of()));
        ContextoPlan contexto = new ContextoPlan(
            recetas, Set.of("lactosa"), Set.of(), BigDecimal.valueOf(200_000),
            Map.of(LECHE, BigDecimal.valueOf(5000)), Set.of(LECHE), Map.of());

        Map<Hueco, Long> plan = generador.completar(contexto, semana());

        assertThat(plan.values()).doesNotContain(200L);
    }

    @Test
    @DisplayName("una condición de salud no excluye la receta, solo la deja más abajo")
    void condicionSoloPenaliza() {
        Map<Long, RecetaCandidata> recetas = new HashMap<>();
        recetas.put(1L, receta(1, 30, 5000, Map.of(ARROZ, 100), Set.of(), Set.of("diabetes")));
        recetas.put(2L, receta(2, 30, 5000, Map.of(ARROZ, 100), Set.of(), Set.of()));
        ContextoPlan contexto = new ContextoPlan(
            recetas, Set.of("diabetes"), Set.of(), BigDecimal.valueOf(200_000), Map.of(), Set.of(), Map.of());

        Map<Hueco, Long> plan = generador.completar(contexto, List.of(new Hueco(1, "ALMUERZO"), new Hueco(1, "CENA")));

        assertThat(plan).containsEntry(new Hueco(1, "ALMUERZO"), 2L).containsEntry(new Hueco(1, "CENA"), 1L);
    }

    @Test
    @DisplayName("los días sin tiempo solo llevan recetas de 20 minutos o menos")
    void diasSinTiempo() {
        Map<Long, RecetaCandidata> recetas = catalogoBase();
        recetas.put(301L, receta(301, 15, 6000, Map.of(HUEVO, 3), Set.of(), Set.of()));
        recetas.put(302L, receta(302, 20, 6000, Map.of(HUEVO, 2), Set.of(), Set.of()));
        ContextoPlan contexto = new ContextoPlan(
            recetas, Set.of(), Set.of(3), BigDecimal.valueOf(200_000), Map.of(), Set.of(), Map.of());

        Map<Hueco, Long> plan = generador.completar(contexto, semana());

        assertThat(Set.of(plan.get(new Hueco(3, "ALMUERZO")), plan.get(new Hueco(3, "CENA"))))
            .containsExactlyInAnyOrder(301L, 302L);
    }

    @Test
    @DisplayName("usa primero lo que está por vencer en la despensa")
    void aprovechaLoPorVencer() {
        Map<Long, RecetaCandidata> recetas = catalogoBase();
        recetas.put(400L, receta(400, 40, 7000, Map.of(POLLO, 500), Set.of(), Set.of()));
        ContextoPlan contexto = new ContextoPlan(
            recetas, Set.of(), Set.of(), BigDecimal.valueOf(200_000),
            Map.of(POLLO, BigDecimal.valueOf(500)), Set.of(POLLO), Map.of());

        Map<Hueco, Long> plan = generador.completar(contexto, semana());

        assertThat(plan.get(new Hueco(1, "ALMUERZO"))).isEqualTo(400L);
        assertThat(generador.aprovechados(contexto, plan.values())).containsExactly(POLLO);
        // El pollo solo alcanza una vez: no se vuelve a proponer por "aprovechar".
        assertThat(plan.values().stream().filter(id -> id == 400L).count()).isEqualTo(1);
    }

    @Test
    @DisplayName("si hay niños en casa prefiere las recetas que les gustan; si no, no cambia nada")
    void ninos() {
        Map<Long, RecetaCandidata> recetas = new HashMap<>();
        recetas.put(1L, receta(1, 40, 5000, Map.of(ARROZ, 200), Set.of(), Set.of()));
        RecetaCandidata pasta = receta(2, 40, 6000, Map.of(ARROZ, 200), Set.of(), Set.of());
        recetas.put(2L, new RecetaCandidata(2L, 40, pasta.costo(), pasta.ingredientes(), Set.of(), Set.of(), true, true, true));
        List<Hueco> lunes = List.of(new Hueco(1, "ALMUERZO"));

        ContextoPlan sinNinos = contexto(recetas);
        assertThat(generador.completar(sinNinos, lunes)).containsEntry(lunes.get(0), 1L);

        ContextoPlan conNinos = sinNinos.conNinos(true);
        Map<Hueco, Long> plan = generador.completar(conNinos, lunes);
        assertThat(plan).containsEntry(lunes.get(0), 2L);
        assertThat(generador.paraNinos(conNinos, plan.values())).isEqualTo(1);
    }

    @Test
    @DisplayName("la preferencia por los niños nunca pasa por encima de una alergia")
    void ninosSinSaltarseAlergias() {
        Map<Long, RecetaCandidata> recetas = new HashMap<>();
        recetas.put(1L, receta(1, 40, 5000, Map.of(ARROZ, 200), Set.of(), Set.of()));
        recetas.put(2L, new RecetaCandidata(
            2L, 40, BigDecimal.valueOf(3000), Map.of(), Set.of("lactosa"), Set.of(), true, true, true));
        ContextoPlan contexto = new ContextoPlan(
            recetas, Set.of("lactosa"), Set.of(), BigDecimal.valueOf(200_000), Map.of(), Set.of(), Map.of(), true);

        assertThat(generador.completar(contexto, List.of(new Hueco(1, "ALMUERZO")))).containsValue(1L);
    }

    @Test
    @DisplayName("no repite receta en la semana si hay de dónde escoger")
    void variedad() {
        Map<Long, RecetaCandidata> recetas = catalogoBase();
        for (long id = 113; id <= 116; id++) {
            recetas.put(id, receta(id, 40, 5000, Map.of(ARROZ, 200), Set.of(), Set.of()));
        }

        Map<Hueco, Long> plan = generador.completar(contexto(recetas), semana());

        assertThat(new HashSet<>(plan.values())).hasSize(14);
    }

    @Test
    @DisplayName("con pocas recetas repite, pero nunca dos veces el mismo día")
    void sinRepetirEnElDia() {
        Map<Long, RecetaCandidata> recetas = new HashMap<>();
        for (long id = 1; id <= 3; id++) {
            recetas.put(id, receta(id, 30, 4000, Map.of(ARROZ, 100), Set.of(), Set.of()));
        }

        Map<Hueco, Long> plan = generador.completar(contexto(recetas), semana());

        assertThat(plan).hasSize(14);
        for (int dia = 1; dia <= 7; dia++) {
            assertThat(plan.get(new Hueco(dia, "ALMUERZO"))).isNotEqualTo(plan.get(new Hueco(dia, "CENA")));
        }
    }

    @Test
    @DisplayName("no cambia lo que la usuaria ya puso y no propone platos que no son fuertes")
    void respetaLoFijo() {
        Map<Long, RecetaCandidata> recetas = catalogoBase();
        recetas.put(500L, new RecetaCandidata(500L, 5, BigDecimal.ONE, Map.of(), Set.of(), Set.of(), true, false, false));
        recetas.put(501L, new RecetaCandidata(501L, 5, BigDecimal.ONE, Map.of(), Set.of(), Set.of(), false, true, false));
        Map<Hueco, Long> fijas = Map.of(new Hueco(2, "ALMUERZO"), 105L);
        ContextoPlan contexto = contexto(recetas).conFijas(fijas);

        Map<Hueco, Long> plan = generador.completar(contexto, semana());

        assertThat(plan).hasSize(13).doesNotContainKey(new Hueco(2, "ALMUERZO"));
        assertThat(plan.values()).doesNotContain(500L, 501L);
        assertThat(plan.get(new Hueco(2, "CENA"))).isNotEqualTo(105L);
    }

    @Test
    @DisplayName("con presupuesto justo escoge las recetas económicas")
    void cabeEnElPresupuesto() {
        Map<Long, RecetaCandidata> recetas = new HashMap<>();
        for (long id = 1; id <= 14; id++) {
            recetas.put(id, receta(id, 40, 20_000, Map.of(POLLO, 500), Set.of(), Set.of()));
        }
        for (long id = 21; id <= 34; id++) {
            recetas.put(id, receta(id, 40, 4_000, Map.of(ARROZ, 200), Set.of(), Set.of()));
        }
        ContextoPlan contexto = new ContextoPlan(
            recetas, Set.of(), Set.of(), BigDecimal.valueOf(70_000), Map.of(), Set.of(), Map.of());

        Map<Hueco, Long> plan = generador.completar(contexto, semana());

        assertThat(generador.costo(contexto, plan)).isLessThanOrEqualTo(BigDecimal.valueOf(70_000));
    }

    @Test
    @DisplayName("\"Ajustar al presupuesto\" cambia primero la comida más cara")
    void ajustaAlPresupuesto() {
        Map<Long, RecetaCandidata> recetas = new HashMap<>();
        recetas.put(1L, receta(1, 40, 30_000, Map.of(), Set.of(), Set.of()));
        recetas.put(2L, receta(2, 40, 12_000, Map.of(), Set.of(), Set.of()));
        recetas.put(3L, receta(3, 40, 3_000, Map.of(), Set.of(), Set.of()));
        Map<Hueco, Long> fijas = Map.of(new Hueco(1, "ALMUERZO"), 1L, new Hueco(2, "ALMUERZO"), 2L);
        ContextoPlan contexto = new ContextoPlan(
            recetas, Set.of(), Set.of(), BigDecimal.valueOf(20_000), Map.of(), Set.of(), fijas);

        Map<Hueco, Long> cambios = generador.ajustarAlPresupuesto(contexto, fijas.keySet());

        assertThat(cambios).containsExactly(Map.entry(new Hueco(1, "ALMUERZO"), 3L));
    }

    @Test
    @DisplayName("\"Ajustar al presupuesto\" no toca las comidas que no puede cambiar")
    void ajusteRespetaPreparadas() {
        Map<Long, RecetaCandidata> recetas = new HashMap<>();
        recetas.put(1L, receta(1, 40, 30_000, Map.of(), Set.of(), Set.of()));
        recetas.put(3L, receta(3, 40, 3_000, Map.of(), Set.of(), Set.of()));
        Map<Hueco, Long> fijas = Map.of(new Hueco(1, "ALMUERZO"), 1L);
        ContextoPlan contexto = new ContextoPlan(
            recetas, Set.of(), Set.of(), BigDecimal.valueOf(10_000), Map.of(), Set.of(), fijas);

        assertThat(generador.ajustarAlPresupuesto(contexto, Set.of())).isEmpty();
    }

    @Test
    @DisplayName("\"Otra opción\" no repite la actual ni las descartadas, y se acaba")
    void otraOpcion() {
        Map<Long, RecetaCandidata> recetas = new HashMap<>();
        for (long id = 1; id <= 3; id++) {
            recetas.put(id, receta(id, 30, 4000, Map.of(), Set.of(), Set.of()));
        }
        Hueco hueco = new Hueco(1, "ALMUERZO");
        ContextoPlan contexto = contexto(recetas).conFijas(Map.of(hueco, 1L));

        assertThat(generador.otraOpcion(contexto, hueco, Set.of())).contains(2L);
        assertThat(generador.otraOpcion(contexto, hueco, Set.of(2L))).contains(3L);
        assertThat(generador.otraOpcion(contexto, hueco, Set.of(2L, 3L))).isEmpty();
    }

    // --- Comidas costosas después del pago (Definición, proceso 3) ----------------------

    /** Siete recetas de $3.000 a $9.000; cada día de la semana lleva solo el almuerzo. */
    private static Map<Long, RecetaCandidata> recetasDeVariosPrecios() {
        Map<Long, RecetaCandidata> recetas = new HashMap<>();
        for (long id = 1; id <= 7; id++) {
            recetas.put(id, receta(id, 30, 2000 + 1000 * id, Map.of(ARROZ, 100), Set.of(), Set.of()));
        }
        return recetas;
    }

    private static List<Hueco> almuerzos() {
        List<Hueco> huecos = new ArrayList<>();
        for (int dia = 1; dia <= 7; dia++) {
            huecos.add(new Hueco(dia, "ALMUERZO"));
        }
        return huecos;
    }

    /** Pago el viernes: viernes 0, sábado 1, domingo 2, lunes 3, … jueves 6. */
    private static Map<Integer, Integer> pagoElViernes() {
        return new CicloIngreso("SEMANAL", 5, null).diasDesdeElPagoEnLaSemana(java.time.LocalDate.of(2026, 10, 26));
    }

    private static long costo(Map<Long, RecetaCandidata> recetas, Long id) {
        return recetas.get(id).costo().longValue();
    }

    @Test
    @DisplayName("deja las comidas más costosas para el día del pago y los siguientes")
    void costosasDespuesDelPago() {
        Map<Long, RecetaCandidata> recetas = recetasDeVariosPrecios();
        ContextoPlan contexto = contexto(recetas).conIngreso(pagoElViernes());

        Map<Hueco, Long> plan = generador.completar(contexto, almuerzos());

        assertThat(plan).hasSize(7);
        // Del viernes (pago) al jueves siguiente, cada día cuesta lo mismo o menos que el anterior.
        List<Integer> desdeElPago = List.of(5, 6, 7, 1, 2, 3, 4);
        for (int i = 1; i < desdeElPago.size(); i++) {
            long anterior = costo(recetas, plan.get(new Hueco(desdeElPago.get(i - 1), "ALMUERZO")));
            long este = costo(recetas, plan.get(new Hueco(desdeElPago.get(i), "ALMUERZO")));
            assertThat(este).isLessThanOrEqualTo(anterior);
        }
        assertThat(generador.diasDespuesDelPago(contexto)).containsExactly(5, 6);
    }

    @Test
    @DisplayName("ordenar por el pago no cambia las recetas ni el costo de la semana")
    void mismoCostoConOSinPago() {
        Map<Long, RecetaCandidata> recetas = recetasDeVariosPrecios();
        Map<Hueco, Long> sinPago = generador.completar(contexto(recetas), almuerzos());
        Map<Hueco, Long> conPago = generador.completar(contexto(recetas).conIngreso(pagoElViernes()), almuerzos());

        assertThat(conPago.values()).containsExactlyInAnyOrderElementsOf(sinPago.values());
        assertThat(generador.costo(contexto(recetas), conPago)).isEqualByComparingTo(generador.costo(contexto(recetas), sinPago));
        assertThat(generador.diasDespuesDelPago(contexto(recetas))).isEmpty();
    }

    @Test
    @DisplayName("no pasa una receta demorada a un día sin tiempo ni mueve lo que salva algo por vencer")
    void respetaDiasSinTiempoYPorVencer() {
        Map<Long, RecetaCandidata> recetas = new HashMap<>();
        // La más cara es lenta: no puede ir al viernes, que es un día sin tiempo.
        recetas.put(1L, receta(1, 60, 20000, Map.of(ARROZ, 100), Set.of(), Set.of()));
        recetas.put(2L, receta(2, 15, 3000, Map.of(ARROZ, 100), Set.of(), Set.of()));
        // Gasta el huevo que está por vencer: el generador la pone primero y ahí se queda.
        recetas.put(3L, receta(3, 15, 15000, Map.of(HUEVO, 2), Set.of(), Set.of()));
        ContextoPlan contexto = new ContextoPlan(
            recetas, Set.of(), Set.of(5), BigDecimal.valueOf(200_000),
            Map.of(HUEVO, BigDecimal.valueOf(2)), Set.of(HUEVO), Map.of())
            .conIngreso(pagoElViernes());

        Map<Hueco, Long> plan = generador.completar(
            contexto, List.of(new Hueco(1, "ALMUERZO"), new Hueco(4, "ALMUERZO"), new Hueco(5, "ALMUERZO")));

        assertThat(plan).containsEntry(new Hueco(1, "ALMUERZO"), 3L);
        assertThat(recetas.get(plan.get(new Hueco(5, "ALMUERZO"))).tiempoMinutos())
            .isLessThanOrEqualTo(GeneradorPlan.MINUTOS_DIA_SIN_TIEMPO);
    }
}
