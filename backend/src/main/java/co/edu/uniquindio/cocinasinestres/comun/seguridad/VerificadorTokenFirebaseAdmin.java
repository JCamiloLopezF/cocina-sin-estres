package co.edu.uniquindio.cocinasinestres.comun.seguridad;

import co.edu.uniquindio.cocinasinestres.comun.error.NoAutenticadoException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Verifica el token contra Firebase con el Admin SDK.
 *
 * <p>Se pide la comprobación de revocación ({@code checkRevoked}) para que cerrar
 * sesión o desactivar una cuenta en Firebase surta efecto de inmediato, sin esperar
 * a que el token venza.</p>
 */
@Component
@Profile("!dev")
public class VerificadorTokenFirebaseAdmin implements VerificadorTokenFirebase {

    private final FirebaseAuth firebaseAuth;

    public VerificadorTokenFirebaseAdmin(FirebaseAuth firebaseAuth) {
        this.firebaseAuth = firebaseAuth;
    }

    @Override
    public Identidad verificar(String idToken) {
        try {
            FirebaseToken token = firebaseAuth.verifyIdToken(idToken, true);
            return new Identidad(token.getUid(), token.getEmail());
        } catch (FirebaseAuthException excepcion) {
            // El motivo exacto no se le cuenta al cliente; queda en la traza del log.
            throw new NoAutenticadoException("Tu sesión expiró. Vuelve a entrar.");
        }
    }
}
