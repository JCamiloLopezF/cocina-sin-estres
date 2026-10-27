package co.edu.uniquindio.cocinasinestres;

import co.edu.uniquindio.cocinasinestres.comun.config.PropiedadesCocina;
import co.edu.uniquindio.cocinasinestres.comun.config.PropiedadesLimites;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Punto de entrada de la API de Cocina sin estrés.
 *
 * <p>Se ejecuta con un perfil activo: {@code dev} para trabajar sin Firebase,
 * {@code prod} para el ambiente desplegado.</p>
 */
@SpringBootApplication
@EnableConfigurationProperties({PropiedadesCocina.class, PropiedadesLimites.class})
public class CocinaSinEstresApplication {

    public static void main(String[] args) {
        SpringApplication.run(CocinaSinEstresApplication.class, args);
    }
}
