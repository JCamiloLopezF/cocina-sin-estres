package co.edu.uniquindio.cocinasinestres.planificador.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * Arma la semana por la usuaria (ADR-24, ajuste de la sustentación: "que la app
 * automatice"). No toca la base de datos: recibe el {@link ContextoPlan} y devuelve
 * qué receta va en cada hueco, así que se prueba sin Spring.
 *
 * <p>Reglas, en orden de peso:</p>
 * <ol>
 *   <li>Nunca propone una receta con un ingrediente al que alguien del hogar es alérgico.</li>
 *   <li>Los días sin tiempo solo llevan recetas de {@value #MINUTOS_DIA_SIN_TIEMPO} minutos
 *       o menos (si no hay ninguna apta, la más rápida).</li>
 *   <li>Usa primero lo que hay en la despensa y, sobre todo, lo que está por vencer.</li>
 *   <li>Reparte el presupuesto que queda entre las comidas que faltan; si al final se pasa,
 *       cambia lo que propuso por recetas más baratas.</li>
 *   <li>Si en el hogar hay niños, prefiere las recetas que en general les gustan.</li>
 *   <li>No repite receta en el mismo día y evita repetirla en la semana.</li>
 *   <li>Las recetas que tocan una condición (diabetes, hipertensión…) quedan más abajo,
 *       pero no se excluyen: el recetario solo avisa.</li>
 * </ol>
 *
 * <p>Ante un empate gana la más barata y luego la de menor id, para que el resultado
 * sea siempre el mismo con los mismos datos.</p>
 */
public final class GeneradorPlan {

    /** Tope de tiempo en los días sin tiempo para cocinar (pantalla 29). */
    public static final int MINUTOS_DIA_SIN_TIEMPO = 20;

    /** Un producto "por vencer" es el que vence en estos días o menos. */
    public static final int DIAS_POR_VENCER = 3;

    /** Comidas que llena "Armar mi semana": el desayuno es opcional (pantalla 27). */
    public static final List<String> COMIDAS_A_LLENAR = List.of("ALMUERZO", "CENA");

    private static final double PESO_DESPENSA = 40;
    private static final double PESO_POR_VENCER = 20;
    private static final int MAX_POR_VENCER = 2;
    private static final double PESO_CONDICION = 15;
    private static final double PESO_NINOS = 20;
    private static final double PESO_COSTO = 30;
    private static final double PESO_REPETIDA = 50;
    private static final double PESO_DIA_VECINO = 20;
    private static final int MAX_CAMBIOS_PRESUPUESTO = 50;
    /** Sin presupuesto conocido, el costo se compara contra este valor por comida. */
    private static final BigDecimal COSTO_REFERENCIA = new BigDecimal("10000");

    /**
     * Llena los huecos vacíos de la lista. Lo que ya está en {@code contexto.fijas()} no
     * se cambia.
     *
     * @return receta propuesta por cada hueco que se llenó, en orden de la semana; un
     *         hueco sin ninguna receta posible queda por fuera
     */
    public Map<Hueco, Long> completar(ContextoPlan contexto, Collection<Hueco> huecos) {
        List<Hueco> pendientes = huecos.stream()
            .filter(h -> !contexto.fijas().containsKey(h))
            .distinct()
            .sorted(Hueco.ORDEN)
            .toList();

        Map<Hueco, Long> plan = new HashMap<>(contexto.fijas());
        Map<Hueco, Long> nuevas = new LinkedHashMap<>();
        for (int i = 0; i < pendientes.size(); i++) {
            Hueco hueco = pendientes.get(i);
            int restantes = pendientes.size() - i;
            elegir(contexto, plan, hueco, Set.of(), restantes).ifPresent(receta -> {
                plan.put(hueco, receta);
                nuevas.put(hueco, receta);
            });
        }
        // Si el plan quedó por encima del presupuesto, se abarata solo lo que se propuso.
        nuevas.putAll(abaratar(contexto, plan, nuevas.keySet()));
        return nuevas;
    }

    /**
     * La mejor receta para un hueco que no sea la que ya tiene ni ninguna de
     * {@code excluir} (las que la usuaria ya descartó con "Otra opción").
     */
    public Optional<Long> otraOpcion(ContextoPlan contexto, Hueco hueco, Set<Long> excluir) {
        Map<Hueco, Long> plan = new HashMap<>(contexto.fijas());
        Long actual = plan.remove(hueco);
        Set<Long> fuera = new HashSet<>(excluir);
        if (actual != null) {
            fuera.add(actual);
        }
        return elegir(contexto, plan, hueco, fuera, 1);
    }

    /**
     * Cambia recetas de los huecos indicados por otras más baratas hasta que el plan
     * quepa en el presupuesto o ya no haya cambio que ahorre.
     *
     * @param modificables huecos que se pueden cambiar (las comidas ya preparadas no)
     * @return solo los huecos que cambiaron, con su receta nueva
     */
    public Map<Hueco, Long> ajustarAlPresupuesto(ContextoPlan contexto, Collection<Hueco> modificables) {
        return abaratar(contexto, new HashMap<>(contexto.fijas()), Set.copyOf(modificables));
    }

    /** Costo del plan con la fórmula del costo semanal (ADR-14). */
    public BigDecimal costo(ContextoPlan contexto, Map<Hueco, Long> plan) {
        return plan.values().stream()
            .map(id -> contexto.recetas().get(id))
            .filter(r -> r != null)
            .map(RecetaCandidata::costo)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Cuántas de las recetas indicadas suelen gustarles a los niños. */
    public long paraNinos(ContextoPlan contexto, Collection<Long> recetas) {
        return recetas.stream()
            .map(id -> contexto.recetas().get(id))
            .filter(r -> r != null && r.gustaANinos())
            .count();
    }

    /** Productos por vencer que gastan las recetas indicadas. */
    public Set<Long> aprovechados(ContextoPlan contexto, Collection<Long> recetas) {
        Set<Long> usados = new TreeSet<>();
        for (Long id : recetas) {
            RecetaCandidata receta = contexto.recetas().get(id);
            if (receta == null) {
                continue;
            }
            receta.ingredientes().keySet().stream()
                .filter(contexto.porVencer()::contains)
                .filter(p -> contexto.stock().getOrDefault(p, BigDecimal.ZERO).signum() > 0)
                .forEach(usados::add);
        }
        return usados;
    }

    // ------------------------------------------------------------------

    private Optional<Long> elegir(
        ContextoPlan contexto, Map<Hueco, Long> plan, Hueco hueco, Set<Long> excluir, int restantes) {

        List<RecetaCandidata> opciones = permitidas(contexto, plan, hueco).stream()
            .filter(r -> !excluir.contains(r.id()))
            .toList();
        if (opciones.isEmpty()) {
            return Optional.empty();
        }

        Map<Long, BigDecimal> stock = stockRestante(contexto, plan);
        BigDecimal porComida = presupuestoPorComida(contexto, plan, restantes);
        Map<Long, Double> puntajes = new HashMap<>();
        for (RecetaCandidata receta : opciones) {
            puntajes.put(receta.id(), puntaje(contexto, plan, hueco, receta, stock, porComida));
        }

        return opciones.stream()
            .sorted(Comparator
                .comparing((RecetaCandidata r) -> puntajes.get(r.id()), Comparator.reverseOrder())
                .thenComparing(RecetaCandidata::costo)
                .thenComparing(RecetaCandidata::id))
            .map(RecetaCandidata::id)
            .findFirst();
    }

    /**
     * Recetas que pueden ir en el hueco: activas, plato fuerte, aptas para el hogar, que
     * no estén ya en otra comida del mismo día y, en un día sin tiempo, rápidas.
     */
    private List<RecetaCandidata> permitidas(ContextoPlan contexto, Map<Hueco, Long> plan, Hueco hueco) {
        Set<Long> delMismoDia = new HashSet<>();
        plan.forEach((h, id) -> {
            if (h.dia() == hueco.dia() && !h.equals(hueco)) {
                delMismoDia.add(id);
            }
        });

        List<RecetaCandidata> aptas = contexto.recetas().values().stream()
            .filter(RecetaCandidata::activa)
            .filter(RecetaCandidata::platoFuerte)
            .filter(r -> Collections.disjoint(r.alergias(), contexto.restricciones()))
            .filter(r -> !delMismoDia.contains(r.id()))
            .sorted(Comparator.comparing(RecetaCandidata::id))
            .toList();

        if (!contexto.diasSinTiempo().contains(hueco.dia()) || aptas.isEmpty()) {
            return aptas;
        }
        List<RecetaCandidata> rapidas = aptas.stream()
            .filter(r -> r.tiempoMinutos() <= MINUTOS_DIA_SIN_TIEMPO)
            .toList();
        if (!rapidas.isEmpty()) {
            return rapidas;
        }
        int minimo = aptas.stream().mapToInt(RecetaCandidata::tiempoMinutos).min().orElseThrow();
        return aptas.stream().filter(r -> r.tiempoMinutos() == minimo).toList();
    }

    private double puntaje(
        ContextoPlan contexto,
        Map<Hueco, Long> plan,
        Hueco hueco,
        RecetaCandidata receta,
        Map<Long, BigDecimal> stock,
        BigDecimal porComida) {

        double puntaje = 0;

        // Cuánto de la receta ya está en la despensa (0 a 1).
        if (!receta.ingredientes().isEmpty()) {
            double cubierto = 0;
            for (Map.Entry<Long, BigDecimal> ingrediente : receta.ingredientes().entrySet()) {
                BigDecimal hay = stock.getOrDefault(ingrediente.getKey(), BigDecimal.ZERO);
                if (ingrediente.getValue().signum() > 0) {
                    cubierto += Math.min(1.0, hay.doubleValue() / ingrediente.getValue().doubleValue());
                }
            }
            puntaje += PESO_DESPENSA * cubierto / receta.ingredientes().size();
        }

        long porVencer = receta.ingredientes().keySet().stream()
            .filter(contexto.porVencer()::contains)
            .filter(p -> stock.getOrDefault(p, BigDecimal.ZERO).signum() > 0)
            .count();
        puntaje += PESO_POR_VENCER * Math.min(porVencer, MAX_POR_VENCER);

        if (!Collections.disjoint(receta.condiciones(), contexto.restricciones())) {
            puntaje -= PESO_CONDICION;
        }

        if (contexto.hayNinos() && receta.gustaANinos()) {
            puntaje += PESO_NINOS;
        }

        puntaje -= PESO_COSTO * receta.costo().doubleValue() / porComida.doubleValue();

        long usos = plan.values().stream().filter(receta.id()::equals).count();
        puntaje -= PESO_REPETIDA * usos;

        boolean enDiaVecino = plan.entrySet().stream()
            .anyMatch(e -> e.getValue().equals(receta.id()) && Math.abs(e.getKey().dia() - hueco.dia()) == 1);
        if (enDiaVecino) {
            puntaje -= PESO_DIA_VECINO;
        }
        return puntaje;
    }

    /** Lo que queda en la despensa después de cocinar todo lo que ya está en el plan. */
    private Map<Long, BigDecimal> stockRestante(ContextoPlan contexto, Map<Hueco, Long> plan) {
        Map<Long, BigDecimal> restante = new HashMap<>(contexto.stock());
        for (Long id : plan.values()) {
            RecetaCandidata receta = contexto.recetas().get(id);
            if (receta == null) {
                continue;
            }
            receta.ingredientes().forEach((producto, cantidad) ->
                restante.computeIfPresent(producto, (p, hay) -> hay.subtract(cantidad).max(BigDecimal.ZERO)));
        }
        return restante;
    }

    /**
     * Presupuesto que le toca a cada comida que falta. Nunca baja de 1/56 del semanal
     * (cuatro veces menos que repartirlo en 14 comidas), para que el costo siga
     * pesando aunque el plan ya se haya comido el presupuesto.
     */
    private BigDecimal presupuestoPorComida(ContextoPlan contexto, Map<Hueco, Long> plan, int restantes) {
        BigDecimal semanal = contexto.presupuestoSemanal();
        if (semanal.signum() <= 0) {
            return COSTO_REFERENCIA;
        }
        BigDecimal libre = semanal.subtract(costo(contexto, plan)).max(BigDecimal.ZERO);
        BigDecimal porComida = libre.divide(BigDecimal.valueOf(Math.max(1, restantes)), 2, RoundingMode.HALF_UP);
        BigDecimal piso = semanal.divide(BigDecimal.valueOf(56), 2, RoundingMode.HALF_UP);
        BigDecimal resultado = porComida.max(piso);
        return resultado.signum() > 0 ? resultado : COSTO_REFERENCIA;
    }

    private Map<Hueco, Long> abaratar(ContextoPlan contexto, Map<Hueco, Long> plan, Set<Hueco> modificables) {
        Map<Hueco, Long> cambios = new LinkedHashMap<>();
        BigDecimal presupuesto = contexto.presupuestoSemanal();
        if (presupuesto.signum() <= 0) {
            return cambios;
        }
        List<Hueco> orden = modificables.stream().sorted(Hueco.ORDEN).toList();

        for (int vuelta = 0; vuelta < MAX_CAMBIOS_PRESUPUESTO; vuelta++) {
            if (costo(contexto, plan).compareTo(presupuesto) <= 0) {
                break;
            }
            Hueco mejorHueco = null;
            RecetaCandidata mejorReceta = null;
            BigDecimal mejorAhorro = BigDecimal.ZERO;

            for (Hueco hueco : orden) {
                RecetaCandidata actual = contexto.recetas().get(plan.get(hueco));
                if (actual == null) {
                    continue;
                }
                Optional<RecetaCandidata> masBarata = alternativaMasBarata(contexto, plan, hueco, actual);
                if (masBarata.isEmpty()) {
                    continue;
                }
                BigDecimal ahorro = actual.costo().subtract(masBarata.get().costo());
                if (ahorro.compareTo(mejorAhorro) > 0) {
                    mejorAhorro = ahorro;
                    mejorHueco = hueco;
                    mejorReceta = masBarata.get();
                }
            }
            if (mejorHueco == null) {
                break;
            }
            plan.put(mejorHueco, mejorReceta.id());
            cambios.put(mejorHueco, mejorReceta.id());
        }
        return cambios;
    }

    /**
     * La receta más barata que puede ir en el hueco, preferiblemente una que no esté en
     * la semana; como mucho se repite una vez (dos veces en la semana).
     */
    private Optional<RecetaCandidata> alternativaMasBarata(
        ContextoPlan contexto, Map<Hueco, Long> plan, Hueco hueco, RecetaCandidata actual) {

        Map<Hueco, Long> sinEste = new HashMap<>(plan);
        sinEste.remove(hueco);
        Map<Long, Long> usos = new HashMap<>();
        sinEste.values().forEach(id -> usos.merge(id, 1L, Long::sum));

        return permitidas(contexto, sinEste, hueco).stream()
            .filter(r -> r.costo().compareTo(actual.costo()) < 0)
            .filter(r -> usos.getOrDefault(r.id(), 0L) <= 1)
            .min(Comparator
                .comparing((RecetaCandidata r) -> usos.getOrDefault(r.id(), 0L))
                .thenComparing(RecetaCandidata::costo)
                .thenComparing(RecetaCandidata::id));
    }
}
