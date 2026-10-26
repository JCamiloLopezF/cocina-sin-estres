package co.edu.uniquindio.cocinasinestres.planificador.domain;

import java.util.Optional;

/**
 * El número de WhatsApp de una tienda, como lo necesita el enlace {@code https://wa.me/}:
 * solo dígitos y con el indicativo del país (KAN-53).
 *
 * <p>La usuaria lo escribe como lo tiene en el celular: "300 123 4567", "+57 300-123-4567"
 * o "573001234567". Un celular colombiano tiene 10 dígitos y empieza por 3. Dominio puro:
 * se prueba sin Spring.</p>
 */
public final class NumeroWhatsapp {

    /** Indicativo de Colombia. */
    public static final String INDICATIVO = "57";

    private NumeroWhatsapp() {
    }

    /**
     * @return el número listo para {@code wa.me} (por ejemplo {@code 573001234567}), o vacío
     *         si no es un celular colombiano
     */
    public static Optional<String> normalizar(String escrito) {
        if (escrito == null) {
            return Optional.empty();
        }
        String digitos = escrito.replaceAll("\\D", "");
        if (digitos.length() == 12 && digitos.startsWith(INDICATIVO + "3")) {
            return Optional.of(digitos);
        }
        if (digitos.length() == 10 && digitos.startsWith("3")) {
            return Optional.of(INDICATIVO + digitos);
        }
        return Optional.empty();
    }
}
