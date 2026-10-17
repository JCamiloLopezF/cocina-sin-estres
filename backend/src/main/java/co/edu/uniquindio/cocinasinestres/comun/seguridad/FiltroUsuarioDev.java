package co.edu.uniquindio.cocinasinestres.comun.seguridad;

import co.edu.uniquindio.cocinasinestres.comun.error.EscritorProblemDetail;
import co.edu.uniquindio.cocinasinestres.perfil.service.UsuarioActualService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Atajo <strong>solo para desarrollo</strong>: acepta el encabezado
 * {@code X-Dev-User: <firebase_uid>} y autentica a esa usuaria sin Firebase (ADR-19).
 *
 * <p>Sirve para probar la API con curl o con el frontend sin montar un proyecto de
 * Firebase. Al no haber token, el correo se inventa a partir del uid con el dominio
 * {@code dev.local}, que no existe.</p>
 *
 * <p>La clase está marcada con {@code @Profile("dev")}: en {@code prod} el bean no se
 * crea y el encabezado no significa nada. Es la única pieza de la aplicación que lo
 * mira, así que no hay forma de que se cuele por otro lado.</p>
 */
@Component
@Profile("dev")
public class FiltroUsuarioDev extends FiltroAutenticacionBase {

    /** Nombre del encabezado. No existe en producción. */
    public static final String ENCABEZADO = "X-Dev-User";

    private static final String DOMINIO_FICTICIO = "@dev.local";

    public FiltroUsuarioDev(UsuarioActualService usuarioActualService,
                            EscritorProblemDetail escritorProblemDetail) {
        super(usuarioActualService, escritorProblemDetail);
    }

    @Override
    protected Optional<IdentidadExterna> identificar(HttpServletRequest peticion) {
        String uid = peticion.getHeader(ENCABEZADO);

        if (uid == null || uid.isBlank()) {
            return Optional.empty();
        }

        String limpio = uid.trim();
        return Optional.of(new IdentidadExterna(limpio, limpio + DOMINIO_FICTICIO));
    }
}
