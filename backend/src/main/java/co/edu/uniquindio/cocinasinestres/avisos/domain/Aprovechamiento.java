package co.edu.uniquindio.cocinasinestres.avisos.domain;

import java.util.List;

/**
 * Una receta que se puede hacer hoy sin comprar nada y que usa productos por vencer
 * (sale del modo rescate, KAN-25).
 *
 * @param productos los que salva, del que vence primero al último
 */
public record Aprovechamiento(Long recetaId, String receta, List<ProductoAviso> productos) {

    public Aprovechamiento {
        productos = List.copyOf(productos);
    }
}
