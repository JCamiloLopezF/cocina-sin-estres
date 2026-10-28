package co.edu.uniquindio.cocinasinestres.curaduria.domain;

import java.util.Collection;
import java.util.Set;

/**
 * Qué es un almuerzo (V17, ADR-24): un plato completo, con una proteína (carne, pollo,
 * pescado, huevo o legumbres) y algo que llene (arroz, pasta, papa, yuca, plátano o arepa).
 * Unos huevos pericos solos, una crema o una pasta con queso son desayuno o cena.
 *
 * <p>Se mira la categoría de los ingredientes obligatorios: los opcionales no se compran
 * ni siempre se sirven. Es una condición mínima; el curador decide además si la receta
 * le parece almuerzo.</p>
 */
public final class ReglaAlmuerzo {

    /** Categorías que aportan la proteína. */
    public static final Set<String> PROTEINA = Set.of("PROTEINAS", "LEGUMBRES");

    /** Categorías que llenan: granos, tubérculos y plátanos, harinas y arepas. */
    public static final Set<String> ACOMPANANTE = Set.of("GRANOS", "TUBERCULOS", "HARINAS");

    public static final String MENSAJE = "Para el almuerzo, la receta necesita una proteína (carne, pollo, "
        + "pescado, huevo o legumbres) y algo que llene (arroz, pasta, papa, yuca, plátano o arepa). "
        + "Agrega lo que falta o márcala solo para desayuno o cena.";

    private ReglaAlmuerzo() {
    }

    /**
     * @param categorias categorías de los ingredientes obligatorios de la receta
     * @return {@code true} si tiene una proteína y algo que llene
     */
    public static boolean esCompleto(Collection<String> categorias) {
        return categorias.stream().anyMatch(PROTEINA::contains)
            && categorias.stream().anyMatch(ACOMPANANTE::contains);
    }
}
