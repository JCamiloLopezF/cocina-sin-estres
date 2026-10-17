package co.edu.uniquindio.cocinasinestres.comun.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Reloj de la aplicación en la zona horaria de las usuarias.
 *
 * <p>Las fechas se guardan en UTC y se muestran en America/Bogota (docs/modelo-datos.md),
 * pero "hoy" para una usuaria es el día de Colombia: un producto que vence hoy no debe
 * aparecer vencido a las 8 p. m. porque en UTC ya es mañana. Se inyecta en vez de llamar a
 * {@code LocalDate.now()} para poder fijar la fecha en las pruebas.</p>
 */
@Configuration
public class ConfiguracionReloj {

    /** Zona horaria en la que se calcula "hoy" para la usuaria. */
    public static final ZoneId ZONA_COLOMBIA = ZoneId.of("America/Bogota");

    @Bean
    public Clock reloj() {
        return Clock.system(ZONA_COLOMBIA);
    }
}
