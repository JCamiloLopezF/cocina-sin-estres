package co.edu.uniquindio.cocinasinestres.planificador.service;

import co.edu.uniquindio.cocinasinestres.perfil.model.Hogar;
import co.edu.uniquindio.cocinasinestres.planificador.domain.AdaptacionReceta;
import co.edu.uniquindio.cocinasinestres.planificador.domain.AdaptadorAlergias;
import co.edu.uniquindio.cocinasinestres.planificador.domain.CatalogoParaPlan;
import co.edu.uniquindio.cocinasinestres.planificador.domain.CatalogoParaPlan.ProductoPlan;
import co.edu.uniquindio.cocinasinestres.planificador.domain.ContextoPlan;
import co.edu.uniquindio.cocinasinestres.planificador.domain.RecetaCandidata;
import co.edu.uniquindio.cocinasinestres.planificador.model.ComidaPlan;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * Lo que hay que comprar de más por quien tiene alergia (ADR-24): el reemplazo de su
 * porción, si no está en la despensa, y los ingredientes de su plato aparte. Se compra
 * una sola porción.
 */
@Component
public class ComprasPorAlergias {

    private final ContextoPlanFactory contextos;
    private final CatalogoParaPlan catalogo;
    private final AdaptadorAlergias adaptador = new AdaptadorAlergias();

    public ComprasPorAlergias(ContextoPlanFactory contextos, CatalogoParaPlan catalogo) {
        this.contextos = contextos;
        this.catalogo = catalogo;
    }

    /**
     * Lo que hay que comprar de más: los reemplazos que no están en la despensa y los
     * platos aparte (lista de compras).
     *
     * @param pendientes comidas que todavía no se han preparado
     * @return cantidad extra por producto, en su unidad base
     */
    public Map<Long, BigDecimal> extras(Hogar hogar, List<ComidaPlan> pendientes) {
        Map<Long, BigDecimal> total = new HashMap<>();
        extrasPorDia(hogar, pendientes, true).values()
            .forEach(delDia -> delDia.forEach((producto, cantidad) -> sumar(total, producto, cantidad)));
        return total;
    }

    /**
     * Lo extra por alergias, separado por día de la semana (1 = lunes … 7 = domingo).
     *
     * @param comidas        comidas a revisar
     * @param soloLoQueFalta {@code true} para la lista de compras (un reemplazo que ya está en
     *                       la despensa no se compra); {@code false} para el costo del plan, que
     *                       cuenta todo lo que pide la receta, como el resto del costo (KAN-24)
     * @return por día, la cantidad extra por producto en su unidad base
     */
    public Map<Integer, Map<Long, BigDecimal>> extrasPorDia(
        Hogar hogar, List<ComidaPlan> comidas, boolean soloLoQueFalta) {
        if (comidas.isEmpty()) {
            return Map.of();
        }
        ContextoPlanFactory.Contexto construido = contextos.construir(hogar, List.of());
        ContextoPlan contexto = construido.plan();
        if (!contexto.alergias().tieneDetalle()) {
            return Map.of();
        }
        // Las recetas del contexto ya vienen para las personas del hogar: una porción es
        // dividir entre ellas. Sin personas conocidas, entre las porciones de la receta.
        int personas = construido.personas();
        Map<Long, Integer> porciones = catalogo.porciones();
        Map<Long, ProductoPlan> productos = catalogo.productos();

        Map<Integer, Map<Long, BigDecimal>> porDia = new HashMap<>();
        for (ComidaPlan comida : comidas) {
            RecetaCandidata receta = contexto.recetas().get(comida.getRecetaId());
            if (receta == null) {
                continue;
            }
            Map<Long, BigDecimal> extras = porDia.computeIfAbsent(comida.getDia(), d -> new HashMap<>());
            for (AdaptacionReceta.Cambio cambio : adaptador.adaptar(contexto, receta).cambios()) {
                if (cambio.tipo() == AdaptacionReceta.TipoCambio.REEMPLAZO && !(soloLoQueFalta && cambio.enDespensa())) {
                    sumar(extras, cambio.reemplazoId(), porcionDeReemplazo(receta, cambio, divisor(personas, porciones, receta.id()), productos));
                } else if (cambio.tipo() == AdaptacionReceta.TipoCambio.PLATO_APARTE && cambio.recetaAparteId() != null) {
                    RecetaCandidata aparte = contexto.recetas().get(cambio.recetaAparteId());
                    if (aparte != null) {
                        BigDecimal division = BigDecimal.valueOf(divisor(personas, porciones, aparte.id()));
                        aparte.ingredientes().forEach((producto, cantidad) ->
                            sumar(extras, producto, cantidad.divide(division, 3, RoundingMode.HALF_UP)));
                    }
                }
            }
        }
        porDia.values().removeIf(Map::isEmpty);
        return porDia;
    }

    /**
     * Una porción del ingrediente original. Si el reemplazo se mide distinto (huevos por
     * unidad, pollo por gramos), se compra una presentación del reemplazo.
     */
    private static BigDecimal porcionDeReemplazo(
        RecetaCandidata receta, AdaptacionReceta.Cambio cambio,
        int divisor, Map<Long, ProductoPlan> productos) {

        BigDecimal original = receta.ingredientes().getOrDefault(cambio.productoId(), BigDecimal.ZERO);
        BigDecimal porcion = original.divide(BigDecimal.valueOf(divisor), 3, RoundingMode.HALF_UP);
        ProductoPlan deOriginal = productos.get(cambio.productoId());
        ProductoPlan deReemplazo = productos.get(cambio.reemplazoId());
        boolean mismaUnidad = deOriginal != null && deReemplazo != null
            && Objects.equals(deOriginal.unidadBase(), deReemplazo.unidadBase());
        if (mismaUnidad || deReemplazo == null || deReemplazo.presentacion() == null) {
            return porcion;
        }
        return deReemplazo.presentacion();
    }

    /** Entre cuántos se reparte la receta para sacar una porción. */
    private static int divisor(int personas, Map<Long, Integer> porciones, Long recetaId) {
        return personas > 0 ? personas : Math.max(1, porciones.getOrDefault(recetaId, 1));
    }

    private static void sumar(Map<Long, BigDecimal> extras, Long producto, BigDecimal cantidad) {
        if (producto != null && cantidad != null && cantidad.signum() > 0) {
            extras.merge(producto, cantidad, BigDecimal::add);
        }
    }
}
