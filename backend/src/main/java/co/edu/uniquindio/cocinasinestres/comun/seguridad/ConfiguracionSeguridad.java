package co.edu.uniquindio.cocinasinestres.comun.seguridad;

import co.edu.uniquindio.cocinasinestres.comun.config.PropiedadesCocina;
import co.edu.uniquindio.cocinasinestres.perfil.domain.Rol;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Cadena de seguridad de la API.
 *
 * <p>La autorización es siempre del backend (ADR-16): el frontend puede esconder
 * botones, pero lo que decide es esto.</p>
 *
 * <p>El filtro de autenticación se inyecta por su clase base: en {@code prod} el único
 * bean disponible es {@link FiltroTokenFirebase} y en {@code dev} es
 * {@link FiltroUsuarioDev}. Así la cadena no sabe en qué modo está y el atajo de
 * desarrollo no puede quedar activo por error en producción.</p>
 */
@Configuration
@EnableMethodSecurity
public class ConfiguracionSeguridad {

    private final PropiedadesCocina propiedades;

    public ConfiguracionSeguridad(PropiedadesCocina propiedades) {
        this.propiedades = propiedades;
    }

    @Bean
    public SecurityFilterChain cadenaDeFiltros(HttpSecurity http,
                                               FiltroAutenticacionBase filtroAutenticacion,
                                               EntryPointNoAutenticado entryPoint,
                                               ManejadorAccesoDenegado accesoDenegado)
            throws Exception {

        http
                // No hay sesión ni cookies: cada petición trae su token, así que no hay
                // nada que un ataque CSRF pueda aprovechar.
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(fuenteCors()))
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .logout(logout -> logout.disable())
                // La autenticación anónima se deja activa a propósito: es lo que permite
                // distinguir "no te identificaste" (401) de "tu rol no alcanza" (403).
                .authorizeHttpRequests(rutas -> rutas
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/salud").permitAll()
                        .requestMatchers("/api/admin/**").hasAuthority(Rol.ADMIN.autoridad())
                        .requestMatchers("/api/curaduria/**")
                            .hasAnyAuthority(Rol.CURADOR.autoridad(), Rol.ADMIN.autoridad())
                        .requestMatchers("/api/**").authenticated()
                        // Nada vive fuera de /api: lo que no esté arriba no existe.
                        .anyRequest().denyAll())
                .exceptionHandling(errores -> errores
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accesoDenegado))
                .addFilterBefore(filtroAutenticacion, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Orígenes del navegador autorizados, desde {@code CORS_ORIGENES}. No se usa
     * {@code *}: el encabezado {@code Authorization} viaja en cada petición.
     */
    @Bean
    public CorsConfigurationSource fuenteCors() {
        CorsConfiguration configuracion = new CorsConfiguration();
        configuracion.setAllowedOrigins(propiedades.cors().origenes());
        configuracion.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        // X-Dev-User se declara para que el navegador pueda mandarlo en dev; en prod
        // el filtro que lo leía no existe, así que el encabezado no hace nada.
        configuracion.setAllowedHeaders(List.of("Authorization", "Content-Type", FiltroUsuarioDev.ENCABEZADO));
        configuracion.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource fuente = new UrlBasedCorsConfigurationSource();
        fuente.registerCorsConfiguration("/api/**", configuracion);
        return fuente;
    }
}
