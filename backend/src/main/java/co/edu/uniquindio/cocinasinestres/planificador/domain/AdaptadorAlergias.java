package co.edu.uniquindio.cocinasinestres.planificador.domain;

import co.edu.uniquindio.cocinasinestres.planificador.domain.AdaptacionReceta.Cambio;
import co.edu.uniquindio.cocinasinestres.planificador.domain.AdaptacionReceta.Estado;
import co.edu.uniquindio.cocinasinestres.planificador.domain.AdaptacionReceta.TipoCambio;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Decide cómo adaptar una receta a las alergias de cada persona, como lo haría la mamá
 * (ADR-24, retroalimentación del profesor):
 *
 * <ol>
 *   <li>Alergia grave a un ingrediente: la receta no se cocina en la casa.</li>
 *   <li>Alergia al ingrediente principal (el que define el plato): a esa persona se le
 *       prepara otra receta aparte, de las que sí puede comer.</li>
 *   <li>Alergia a otro ingrediente: en su porción se cambia por un reemplazo que no le
 *       haga daño (primero uno que haya en la despensa) o, si no hay, se le sirve sin él.</li>
 * </ol>
 *
 * <p>Las condiciones de salud (diabetes, hipertensión…) no se adaptan: solo restan
 * puntos en el generador, como antes.</p>
 */
public final class AdaptadorAlergias {

    /** Un plato aparte prefiere recetas así de rápidas: es trabajo extra. */
    private static final int MINUTOS_PLATO_APARTE = 20;

    public AdaptacionReceta adaptar(ContextoPlan contexto, RecetaCandidata receta) {
        ReglasAlergia reglas = contexto.alergias();
        if (!reglas.tieneDetalle()) {
            // Sin detalle por persona, las alergias del hogar descartan la receta, como antes.
            return Collections.disjoint(receta.alergias(), contexto.restricciones())
                ? AdaptacionReceta.APTA
                : new AdaptacionReceta(Estado.NO_APTA, List.of());
        }

        List<Cambio> cambios = new ArrayList<>();
        for (Comensal persona : reglas.comensales()) {
            Set<String> choque = interseccion(receta.alergias(), persona.restricciones());
            if (choque.isEmpty()) {
                continue;
            }
            Set<String> graves = interseccion(choque, persona.alergiasGraves());
            if (!graves.isEmpty()) {
                return new AdaptacionReceta(Estado.NO_APTA, List.of(
                    new Cambio(TipoCambio.NO_APTA, persona.nombre(), graves, null, null, false, null)));
            }
            cambios.addAll(cambiosPara(contexto, receta, persona, choque));
        }

        Estado estado = cambios.isEmpty() ? Estado.APTA
            : cambios.stream().anyMatch(c -> c.tipo() == TipoCambio.PLATO_APARTE) ? Estado.PLATO_APARTE
            : Estado.ADAPTADA;
        return new AdaptacionReceta(estado, cambios);
    }

    private List<Cambio> cambiosPara(
        ContextoPlan contexto, RecetaCandidata receta, Comensal persona, Set<String> choque) {

        ReglasAlergia reglas = contexto.alergias();
        List<Long> ofensores = receta.ingredientes().keySet().stream()
            .filter(p -> !Collections.disjoint(reglas.alergenos().getOrDefault(p, Set.of()), persona.restricciones()))
            .sorted()
            .toList();

        // Si no se sabe qué ingrediente es, o es el principal, le toca otro plato.
        if (ofensores.isEmpty() || ofensores.stream().anyMatch(receta.principales()::contains)) {
            return List.of(new Cambio(
                TipoCambio.PLATO_APARTE, persona.nombre(), choque, null, null, false,
                platoAparte(contexto, receta, persona).orElse(null)));
        }

        List<Cambio> cambios = new ArrayList<>();
        for (Long producto : ofensores) {
            Set<String> motivo = interseccion(reglas.alergenos().getOrDefault(producto, Set.of()), persona.restricciones());
            Optional<Long> reemplazo = reglas.reemplazos().getOrDefault(producto, Set.of()).stream()
                .filter(r -> Collections.disjoint(reglas.alergenos().getOrDefault(r, Set.of()), persona.restricciones()))
                .min(Comparator
                    .comparing((Long r) -> !enDespensa(contexto, r))
                    .thenComparing(r -> r));
            cambios.add(reemplazo
                .map(r -> new Cambio(TipoCambio.REEMPLAZO, persona.nombre(), motivo, producto, r, enDespensa(contexto, r), null))
                .orElseGet(() -> new Cambio(TipoCambio.SIN, persona.nombre(), motivo, producto, null, false, null)));
        }
        return cambios;
    }

    /**
     * La receta que se le prepara aparte: plato fuerte sin sus alergias, de la misma comida
     * que la original (un almuerzo por un almuerzo), rápida y barata.
     */
    private Optional<Long> platoAparte(ContextoPlan contexto, RecetaCandidata original, Comensal persona) {
        return contexto.recetas().values().stream()
            .filter(RecetaCandidata::activa)
            .filter(RecetaCandidata::platoFuerte)
            .filter(r -> !r.id().equals(original.id()))
            .filter(r -> Collections.disjoint(r.alergias(), persona.restricciones()))
            .min(Comparator
                .comparing((RecetaCandidata r) -> Collections.disjoint(r.momentos(), original.momentos()))
                .thenComparing(r -> r.tiempoMinutos() > MINUTOS_PLATO_APARTE)
                .thenComparing(RecetaCandidata::costo)
                .thenComparing(RecetaCandidata::id))
            .map(RecetaCandidata::id);
    }

    private static boolean enDespensa(ContextoPlan contexto, Long producto) {
        return contexto.stock().getOrDefault(producto, BigDecimal.ZERO).signum() > 0;
    }

    private static Set<String> interseccion(Set<String> a, Set<String> b) {
        Set<String> comunes = new HashSet<>(a);
        comunes.retainAll(b);
        return comunes;
    }
}
