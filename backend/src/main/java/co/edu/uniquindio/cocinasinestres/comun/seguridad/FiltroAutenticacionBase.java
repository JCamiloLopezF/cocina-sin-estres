package co.edu.uniquindio.cocinasinestres.comun.seguridad;

import co.edu.uniquindio.cocinasinestres.comun.error.CodigoError;
import co.edu.uniquindio.cocinasinestres.comun.error.EscritorProblemDetail;
import co.edu.uniquindio.cocinasinestres.comun.error.ExcepcionNegocio;
import co.edu.uniquindio.cocinasinestres.perfil.domain.UsuarioAutenticado;
import co.edu.uniquindio.cocinasinestres.perfil.service.UsuarioActualService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Parte común de los dos modos de autenticación: el token de Firebase en producción
 * y el encabezado {@code X-Dev-User} en el perfil {@code dev} (ADR-19).
 *
 * <p>Solo cambia de dónde sale la credencial. Lo demás —resolver la cuenta con alta
 * perezosa, publicar la autenticación y responder un error con el formato del resto
 * de la API— se hace una sola vez aquí.</p>
 *
 * <p>Si no llega credencial, el filtro deja pasar la petición sin autenticar: será la
 * cadena de seguridad la que decida si esa ruta es pública o responde 401.</p>
 */
public abstract class FiltroAutenticacionBase extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(FiltroAutenticacionBase.class);

    private final UsuarioActualService usuarioActualService;
    private final EscritorProblemDetail escritorProblemDetail;

    protected FiltroAutenticacionBase(UsuarioActualService usuarioActualService,
                                      EscritorProblemDetail escritorProblemDetail) {
        this.usuarioActualService = usuarioActualService;
        this.escritorProblemDetail = escritorProblemDetail;
    }

    /**
     * Saca la credencial de la petición y la traduce a la identidad que afirma.
     *
     * @return vacío si la petición no trae credencial de este modo
     * @throws ExcepcionNegocio si la trae pero no es válida
     */
    protected abstract Optional<IdentidadExterna> identificar(HttpServletRequest peticion);

    @Override
    protected void doFilterInternal(HttpServletRequest peticion,
                                    HttpServletResponse respuesta,
                                    FilterChain cadena) throws ServletException, IOException {

        try {
            Optional<IdentidadExterna> identidad = identificar(peticion);
            if (identidad.isPresent()) {
                autenticar(identidad.get());
            }
        } catch (ExcepcionNegocio excepcion) {
            SecurityContextHolder.clearContext();
            escritorProblemDetail.escribir(peticion, respuesta, excepcion.getEstado(),
                    excepcion.getTitulo(), excepcion.getCodigo(), excepcion.getMessage());
            return;
        } catch (RuntimeException excepcion) {
            SecurityContextHolder.clearContext();
            log.warn("No se pudo autenticar la petición {} {}",
                    peticion.getMethod(), peticion.getRequestURI(), excepcion);
            escritorProblemDetail.escribir(peticion, respuesta, HttpStatus.UNAUTHORIZED,
                    "Sesión no válida", CodigoError.AUTH_TOKEN_INVALIDO,
                    "Tu sesión no es válida. Vuelve a entrar.");
            return;
        }

        cadena.doFilter(peticion, respuesta);
    }

    private void autenticar(IdentidadExterna identidad) {
        UsuarioAutenticado usuario =
                usuarioActualService.registrarSiNoExiste(identidad.firebaseUid(), identidad.correo());

        var autenticacion = UsernamePasswordAuthenticationToken.authenticated(
                usuario,
                null,
                List.of(new SimpleGrantedAuthority(usuario.rol().autoridad())));

        SecurityContextHolder.getContext().setAuthentication(autenticacion);
    }

    /**
     * Identidad que afirma la credencial, antes de buscarla en la base.
     *
     * @param firebaseUid identificador de la cuenta en Firebase
     * @param correo      correo asociado a esa cuenta
     */
    protected record IdentidadExterna(String firebaseUid, String correo) {
    }
}
