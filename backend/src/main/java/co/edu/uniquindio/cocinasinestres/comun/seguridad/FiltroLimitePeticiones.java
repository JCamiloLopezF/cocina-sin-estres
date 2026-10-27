package co.edu.uniquindio.cocinasinestres.comun.seguridad;

import co.edu.uniquindio.cocinasinestres.comun.config.PropiedadesLimites;
import co.edu.uniquindio.cocinasinestres.comun.error.CodigoError;
import co.edu.uniquindio.cocinasinestres.comun.error.EscritorProblemDetail;
import co.edu.uniquindio.cocinasinestres.perfil.domain.UsuarioAutenticado;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Límite de peticiones por minuto, por cuenta (o por IP si no hay sesión).
 *
 * <p>Va después del filtro de autenticación, así que cuenta por la usuaria ya
 * identificada: cambiar de IP no sirve para saltárselo. Hay dos cubetas: una general
 * para todas las rutas y otra, más estrecha, para las rutas que más trabajan (generar y
 * ajustar el plan, otra opción, rescate y análisis). Al pasarse responde 429 con
 * {@code Retry-After}.</p>
 *
 * <p>Los contadores viven en memoria: la API corre en una sola instancia. Si algún día
 * corre en varias, cada una contará por su lado y el tope real será N veces este.</p>
 *
 * <p>No es un bean: lo crea {@link ConfiguracionSeguridad} y solo vive en la cadena de
 * seguridad.</p>
 */
public class FiltroLimitePeticiones extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(FiltroLimitePeticiones.class);

    private static final long MINUTO_MS = 60_000L;

    /** Al pasar de este número de llaves se barren las de minutos anteriores. */
    private static final int LLAVES_ANTES_DE_LIMPIAR = 10_000;

    private static final Pattern RUTA_COSTOSA = Pattern.compile(
            "^/api/(planes-semanales/[^/]+/(generar|ajustar-presupuesto|comidas/[^/]+/otra-opcion)"
                    + "|rescate|admin/analisis)/?$");

    private final PropiedadesLimites limites;
    private final EscritorProblemDetail escritorProblemDetail;
    private final Clock reloj;
    private final Map<String, Ventana> ventanas = new ConcurrentHashMap<>();

    public FiltroLimitePeticiones(PropiedadesLimites limites,
                                  EscritorProblemDetail escritorProblemDetail,
                                  Clock reloj) {
        this.limites = limites;
        this.escritorProblemDetail = escritorProblemDetail;
        this.reloj = reloj;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest peticion) {
        // El preflight de CORS y la ruta de salud (la usa el monitor) no cuentan.
        return "OPTIONS".equals(peticion.getMethod()) || "/api/salud".equals(peticion.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest peticion,
                                    HttpServletResponse respuesta,
                                    FilterChain cadena) throws ServletException, IOException {

        long ahora = reloj.millis();
        long minuto = ahora / MINUTO_MS;
        String quien = quien(peticion);

        boolean pasa = admite(quien, minuto, limites.peticionesPorMinuto());
        if (pasa && esCostosa(peticion)) {
            pasa = admite("costosa|" + quien, minuto, limites.costosasPorMinuto());
        }

        if (!pasa) {
            long segundos = Math.max(1, ((minuto + 1) * MINUTO_MS - ahora + 999) / 1000);
            // Sin el correo ni el uid: solo el id interno o la IP (RNF-21).
            log.warn("Límite de peticiones: {} en {} {}", quien, peticion.getMethod(), peticion.getRequestURI());
            respuesta.setHeader(HttpHeaders.RETRY_AFTER, Long.toString(segundos));
            escritorProblemDetail.escribir(peticion, respuesta, HttpStatus.TOO_MANY_REQUESTS,
                    "Demasiadas peticiones", CodigoError.LIMITE_PETICIONES,
                    "Vas muy rápido. Espera un momento y vuelve a intentar.");
            return;
        }

        limpiarSiHaceFalta(minuto);
        cadena.doFilter(peticion, respuesta);
    }

    static boolean esCostosa(HttpServletRequest peticion) {
        return RUTA_COSTOSA.matcher(peticion.getRequestURI()).matches();
    }

    private boolean admite(String llave, long minuto, int tope) {
        if (tope <= 0) {
            return true;
        }
        Ventana ventana = ventanas.compute(llave, (k, actual) ->
                actual == null || actual.minuto() != minuto ? new Ventana(minuto, 1) : actual.sumar());
        return ventana.cuenta() <= tope;
    }

    private void limpiarSiHaceFalta(long minuto) {
        if (ventanas.size() > LLAVES_ANTES_DE_LIMPIAR) {
            ventanas.values().removeIf(ventana -> ventana.minuto() < minuto);
        }
    }

    private static String quien(HttpServletRequest peticion) {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion != null && autenticacion.getPrincipal() instanceof UsuarioAutenticado usuario) {
            return "usuaria:" + usuario.id();
        }
        return "ip:" + peticion.getRemoteAddr();
    }

    private record Ventana(long minuto, int cuenta) {

        Ventana sumar() {
            return new Ventana(minuto, cuenta + 1);
        }
    }
}
