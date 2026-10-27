package co.edu.uniquindio.cocinasinestres.comun.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Topes que protegen la API del abuso, bajo el prefijo {@code cocina.limites}.
 *
 * <p>Un valor en cero apaga ese límite. En el perfil {@code dev} los límites de
 * peticiones están apagados para que las pruebas y el trabajo local no se frenen.</p>
 *
 * @param peticionesPorMinuto peticiones que puede hacer una cuenta (o una IP sin sesión)
 *                            en un minuto, sumando todas las rutas
 * @param costosasPorMinuto   peticiones por minuto a las rutas que más trabajan: generar o
 *                            ajustar el plan, pedir otra opción, el modo rescate y el análisis
 * @param cuerpoMaximoBytes   tamaño máximo del cuerpo de una petición
 */
@ConfigurationProperties(prefix = "cocina.limites")
public record PropiedadesLimites(
        @DefaultValue("240") int peticionesPorMinuto,
        @DefaultValue("20") int costosasPorMinuto,
        @DefaultValue("65536") long cuerpoMaximoBytes) {
}
