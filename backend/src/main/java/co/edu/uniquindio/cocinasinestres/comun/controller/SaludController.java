package co.edu.uniquindio.cocinasinestres.comun.controller;

import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Comprobación de salud. Es la única ruta pública: sirve para que el despliegue y el
 * frontend sepan que la API responde, sin necesidad de token.
 *
 * <p>No dice nada del estado interno (ni versión de la base, ni migraciones, ni
 * nombres de servicios): es información que no le hace falta a nadie de afuera.</p>
 */
@RestController
@RequestMapping("/api/salud")
public class SaludController {

    private final String version;

    public SaludController(@Value("${spring.application.version:desconocida}") String version) {
        this.version = version;
    }

    @GetMapping
    public SaludResponse consultar() {
        return new SaludResponse("ok", version, Instant.now());
    }

    /**
     * @param estado  siempre {@code ok}: si la API no puede responder, no responde
     * @param version versión de la aplicación
     * @param horaUtc hora del servidor en UTC, útil para detectar relojes desfasados
     */
    public record SaludResponse(String estado, String version, Instant horaUtc) {
    }
}
