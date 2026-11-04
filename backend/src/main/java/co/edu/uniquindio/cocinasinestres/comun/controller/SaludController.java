package co.edu.uniquindio.cocinasinestres.comun.controller;

import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Comprobación de salud. Es la única ruta pública: sirve para que el despliegue, Elastic
 * Beanstalk y la vigilancia (.github/workflows/vigilancia.yml) sepan que la API responde,
 * sin necesidad de token.
 *
 * <p>Además de responder, pregunta a la base de datos (auditoría del 4 de noviembre:
 * «hacer que el chequeo verifique la base»). Si la base no contesta, responde 503 para
 * que la vigilancia lo registre como incidente, aunque la API siga encendida.</p>
 *
 * <p>No dice nada más del estado interno (ni versión de la base, ni migraciones, ni
 * nombres de servicios): es información que no le hace falta a nadie de afuera.</p>
 */
@RestController
@RequestMapping("/api/salud")
public class SaludController {

    private static final Logger log = LoggerFactory.getLogger(SaludController.class);

    private final String version;
    private final JdbcTemplate jdbc;

    public SaludController(@Value("${spring.application.version:desconocida}") String version, JdbcTemplate jdbc) {
        this.version = version;
        this.jdbc = jdbc;
    }

    @GetMapping
    public ResponseEntity<SaludResponse> consultar() {
        if (baseResponde()) {
            return ResponseEntity.ok(new SaludResponse("ok", version, Instant.now()));
        }
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(new SaludResponse("sin-base-de-datos", version, Instant.now()));
    }

    private boolean baseResponde() {
        try {
            Integer uno = jdbc.queryForObject("SELECT 1", Integer.class);
            return uno != null && uno == 1;
        } catch (RuntimeException error) {
            log.error("La base de datos no responde a la comprobación de salud", error);
            return false;
        }
    }

    /**
     * @param estado  {@code ok}, o {@code sin-base-de-datos} si la base no contesta (503)
     * @param version versión de la aplicación
     * @param horaUtc hora del servidor en UTC, útil para detectar relojes desfasados
     */
    public record SaludResponse(String estado, String version, Instant horaUtc) {
    }
}
