package co.edu.uniquindio.cocinasinestres.planificador.dto;

/**
 * Cuerpo de {@code POST /api/tiendas} (KAN-53).
 *
 * @param nombre   cómo la llama la usuaria ("Tienda de don Jorge")
 * @param whatsapp el número como lo tenga escrito ("300 123 4567")
 */
public record TiendaRequest(String nombre, String whatsapp) {
}
