package co.edu.uniquindio.cocinasinestres.perfil.repository;

import co.edu.uniquindio.cocinasinestres.perfil.model.Usuario;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso a las cuentas. Solo el módulo {@code perfil} lo usa: los demás módulos
 * piden la usuaria autenticada a {@code UsuarioActualService}.
 */
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /**
     * Busca la cuenta por su identificador de Firebase, que es la llave con la que
     * entra toda petición autenticada.
     */
    Optional<Usuario> findByFirebaseUid(String firebaseUid);
}
