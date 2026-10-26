package co.edu.uniquindio.cocinasinestres.planificador.dto;

/**
 * Una tienda de la usuaria (KAN-53).
 *
 * @param id       tienda
 * @param nombre   cómo la llama la usuaria
 * @param whatsapp número listo para {@code https://wa.me/} (573001234567)
 */
public record TiendaResponse(Long id, String nombre, String whatsapp) {
}
