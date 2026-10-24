package co.edu.uniquindio.cocinasinestres.planificador.domain;

import java.util.Comparator;
import java.util.List;

/**
 * Un espacio del plan: un día (1 = lunes … 7 = domingo) y una comida.
 */
public record Hueco(int dia, String tipoComida) {

    /** Orden de las comidas dentro del día. */
    public static final List<String> COMIDAS = List.of("DESAYUNO", "ALMUERZO", "CENA");

    /** Lunes antes que martes; en el mismo día, almuerzo antes que cena. */
    public static final Comparator<Hueco> ORDEN = Comparator
        .comparingInt(Hueco::dia)
        .thenComparingInt(h -> COMIDAS.indexOf(h.tipoComida()));
}
