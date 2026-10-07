package co.edu.uniquindio.cocinasinestres.comun.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Inicializa el Firebase Admin SDK, con el que se verifican los tokens (ADR-05).
 *
 * <p>No se carga en el perfil {@code dev}: ahí se entra con el encabezado
 * {@code X-Dev-User} y nadie necesita una cuenta de servicio. En cualquier otro
 * perfil la ruta al JSON es obligatoria y el arranque falla de una si falta, en
 * lugar de dejar la API aceptando peticiones sin poder verificar nada.</p>
 *
 * <p>El JSON de la cuenta de servicio vive <strong>fuera del repositorio</strong>,
 * que es público (CONTRIBUTING.md §8).</p>
 */
@Configuration
@Profile("!dev")
public class ConfiguracionFirebase {

    private static final Logger log = LoggerFactory.getLogger(ConfiguracionFirebase.class);

    @Bean
    public FirebaseApp firebaseApp(PropiedadesCocina propiedades) throws IOException {
        String ruta = propiedades.firebase().credencialesRuta();

        if (ruta == null || ruta.isBlank()) {
            throw new IllegalStateException(
                    "Falta FIREBASE_CREDENCIALES_RUTA: sin la cuenta de servicio no se pueden "
                    + "verificar los tokens. Para trabajar sin Firebase usa el perfil dev.");
        }

        Path archivo = Path.of(ruta);
        if (!Files.isReadable(archivo)) {
            throw new IllegalStateException(
                    "No se puede leer la cuenta de servicio de Firebase en la ruta configurada.");
        }

        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }

        try (InputStream credenciales = Files.newInputStream(archivo)) {
            FirebaseOptions opciones = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(credenciales))
                    .build();
            log.info("Firebase Admin SDK inicializado");
            return FirebaseApp.initializeApp(opciones);
        }
    }

    @Bean
    public FirebaseAuth firebaseAuth(FirebaseApp firebaseApp) {
        return FirebaseAuth.getInstance(firebaseApp);
    }
}
