package co.edu.uniquindio.cocinasinestres.comun.error;

/**
 * Un campo rechazado por la validación, dentro del arreglo {@code errores} de la
 * respuesta. Permite que el formulario marque exactamente el campo con problema.
 *
 * @param campo   nombre del campo en el JSON que llegó
 * @param mensaje qué le falta, en español y en tono cordial (RNF-14)
 */
public record ErrorCampo(String campo, String mensaje) {
}
