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
     * @param pendientes comidas que todavía no se han preparado
     * @return cantidad extra por producto, en su unidad base
     */
    public Map<Long, BigDecimal> extras(Hogar hogar, List<ComidaPlan> pendientes) {
        if (pendientes.isEmpty()) {
            return Map.of();
        }
        ContextoPlan contexto = contextos.construir(hogar, List.of()).plan();
        if (!contexto.alergias().tieneDetalle()) {
            return Map.of();
        }
        Map<Long, Integer> porciones = catalogo.porciones();
        Map<Long, ProductoPlan> productos = catalogo.productos();

        Map<Long, BigDecimal> extras = new HashMap<>();
        for (ComidaPlan comida : pendientes) {
            RecetaCandidata receta = contexto.recetas().get(comida.getRecetaId());
            if (receta == null) {
                continue;
            }
            for (AdaptacionReceta.Cambio cambio : adaptador.adaptar(contexto, receta).cambios()) {
                if (cambio.tipo() == AdaptacionReceta.TipoCambio.REEMPLAZO && !cambio.enDespensa()) {
                    sumar(extras, cambio.reemplazoId(), porcionDeReemplazo(receta, cambio, porciones, productos));
                } else if (cambio.tipo() == AdaptacionReceta.TipoCambio.PLATO_APARTE && cambio.recetaAparteId() != null) {
                    RecetaCandidata aparte = contexto.recetas().get(cambio.recetaAparteId());
                    if (aparte != null) {
                        BigDecimal division = BigDecimal.valueOf(Math.max(1, porciones.getOrDefault(aparte.id(), 1)));
                        aparte.ingredientes().forEach((producto, cantidad) ->
                            sumar(extras, producto, cantidad.divide(division, 3, RoundingMode.HALF_UP)));
                    }
                }
            }
        }
        return extras;
    }

    /**
     * Una porción del ingrediente original. Si el reemplazo se mide distinto (huevos por
     * unidad, pollo por gramos), se compra una presentación del reemplazo.
     */
    private static BigDecimal porcionDeReemplazo(
        RecetaCandidata receta, AdaptacionReceta.Cambio cambio,
        Map<Long, Integer> porciones, Map<Long, ProductoPlan> productos) {

        BigDecimal original = receta.ingredientes().getOrDefault(cambio.productoId(), BigDecimal.ZERO);
        BigDecimal porcion = original.divide(
            BigDecimal.valueOf(Math.max(1, porciones.getOrDefault(receta.id(), 1))), 3, RoundingMode.HALF_UP);
        ProductoPlan deOriginal = productos.get(cambio.productoId());
        ProductoPlan deReemplazo = productos.get(cambio.reemplazoId());
        boolean mismaUnidad = deOriginal != null && deReemplazo != null
            && Objects.equals(deOriginal.unidadBase(), deReemplazo.unidadBase());
        if (mismaUnidad || deReemplazo == null || deReemplazo.presentacion() == null) {
            return porcion;
        }
        return deReemplazo.presentacion();
    }

    private static void sumar(Map<Long, BigDecimal> extras, Long producto, BigDecimal cantidad) {
        if (producto != null && cantidad != null && cantidad.signum() > 0) {
            extras.merge(producto, cantidad, BigDecimal::add);
        }
    }
}
