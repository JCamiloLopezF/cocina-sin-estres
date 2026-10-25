package co.edu.uniquindio.cocinasinestres.planificador.domain;

import co.edu.uniquindio.cocinasinestres.planificador.domain.AdaptacionReceta.Cambio;
import co.edu.uniquindio.cocinasinestres.planificador.domain.AdaptacionReceta.Estado;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Modo rescate (KAN-25): ante un imprevisto, qué se puede cocinar hoy <strong>sin
 * comprar nada</strong>, primero lo que usa los productos más cerca de vencer.
 *
 * <p>Una receta entra si la despensa alcanza para todos sus ingredientes obligatorios
 * (sin contar lo vencido) y si se puede servir a todo el hogar sin comprar: nadie tiene
 * alergia grave a algo de la receta, cada reemplazo por alergia ya está en la despensa
 * y, si a alguien le toca plato aparte, ese plato también se puede hacer con lo que hay.</p>
 *
 * <p>Sin Spring ni base de datos (ADR-09): se prueba con JUnit.</p>
 */
public final class Rescate {

    /** Ventana de "por vencer" con la que se ordena: la misma de la despensa. */
    public static final int DIAS_POR_VENCER = 7;

    /** Cuántas opciones se muestran como máximo. */
    public static final int MAX_OPCIONES = 12;

    private final AdaptadorAlergias adaptador = new AdaptadorAlergias();

    /**
     * Una receta que se puede hacer hoy.
     *
     * @param recetaId          receta
     * @param tiempoMinutos     tiempo total
     * @param aprovecha         productos por vencer que usa, del que vence primero al último
     * @param primerVencimiento el vencimiento más cercano entre esos productos, o {@code null}
     *                          si no usa ninguno por vencer
     * @param adaptacion        cómo queda frente a las alergias del hogar
     */
    public record Opcion(
        Long recetaId,
        int tiempoMinutos,
        List<Long> aprovecha,
        LocalDate primerVencimiento,
        AdaptacionReceta adaptacion) {

        public Opcion {
            aprovecha = List.copyOf(aprovecha);
        }
    }

    /**
     * @param contexto     catálogo, despensa (sin lo vencido) y alergias del hogar
     * @param vencimientos fecha de vencimiento de los productos que vencen pronto
     * @param maxMinutos   tiempo máximo de la receta, o {@code null} para no limitar
     * @return las opciones en orden: primero las que salvan el producto que vence antes,
     *         luego las que aprovechan más productos, los platos fuertes y las más rápidas
     */
    public List<Opcion> opciones(ContextoPlan contexto, Map<Long, LocalDate> vencimientos, Integer maxMinutos) {
        Objects.requireNonNull(contexto, "contexto");
        Map<Long, LocalDate> fechas = vencimientos == null ? Map.of() : vencimientos;

        List<Candidata> candidatas = new ArrayList<>();
        for (RecetaCandidata receta : contexto.recetas().values()) {
            if (!receta.activa() || (maxMinutos != null && receta.tiempoMinutos() > maxMinutos)) {
                continue;
            }
            if (!alcanza(receta.ingredientes(), contexto.stock())) {
                continue;
            }
            AdaptacionReceta adaptacion = adaptador.adaptar(contexto, receta);
            if (!sinComprar(contexto, adaptacion)) {
                continue;
            }
            List<Long> aprovecha = receta.ingredientes().keySet().stream()
                .filter(fechas::containsKey)
                .sorted(Comparator.comparing((Long p) -> fechas.get(p)).thenComparing(p -> p))
                .toList();
            LocalDate primero = aprovecha.isEmpty() ? null : fechas.get(aprovecha.get(0));
            candidatas.add(new Candidata(
                receta, new Opcion(receta.id(), receta.tiempoMinutos(), aprovecha, primero, adaptacion)));
        }

        return candidatas.stream()
            .sorted(Comparator
                .comparing((Candidata c) -> c.opcion().primerVencimiento(), Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(c -> -c.opcion().aprovecha().size())
                .thenComparing(c -> !c.receta().platoFuerte())
                .thenComparing(c -> c.opcion().adaptacion().estado())
                .thenComparing(c -> c.receta().tiempoMinutos())
                .thenComparing(c -> c.receta().id()))
            .limit(MAX_OPCIONES)
            .map(Candidata::opcion)
            .toList();
    }

    /** La despensa tiene al menos la cantidad que pide cada ingrediente. */
    static boolean alcanza(Map<Long, BigDecimal> necesita, Map<Long, BigDecimal> stock) {
        return necesita.entrySet().stream().allMatch(e ->
            stock.getOrDefault(e.getKey(), BigDecimal.ZERO).compareTo(e.getValue()) >= 0);
    }

    /** Se puede servir a todo el hogar sin salir a comprar. */
    private static boolean sinComprar(ContextoPlan contexto, AdaptacionReceta adaptacion) {
        if (adaptacion.estado() == Estado.NO_APTA) {
            return false;
        }
        for (Cambio cambio : adaptacion.cambios()) {
            boolean sirve = switch (cambio.tipo()) {
                case SIN -> true;
                case REEMPLAZO -> cambio.enDespensa();
                case PLATO_APARTE -> cambio.recetaAparteId() != null
                    && contexto.recetas().containsKey(cambio.recetaAparteId())
                    && alcanza(contexto.recetas().get(cambio.recetaAparteId()).ingredientes(), contexto.stock());
                case NO_APTA -> false;
            };
            if (!sirve) {
                return false;
            }
        }
        return true;
    }

    private record Candidata(RecetaCandidata receta, Opcion opcion) {
    }
}
