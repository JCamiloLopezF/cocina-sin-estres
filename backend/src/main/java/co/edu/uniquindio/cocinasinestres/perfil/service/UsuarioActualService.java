package co.edu.uniquindio.cocinasinestres.perfil.service;

import co.edu.uniquindio.cocinasinestres.comun.config.PropiedadesCocina;
import co.edu.uniquindio.cocinasinestres.comun.error.CodigoError;
import co.edu.uniquindio.cocinasinestres.comun.error.NoAutenticadoException;
import co.edu.uniquindio.cocinasinestres.perfil.domain.Rol;
import co.edu.uniquindio.cocinasinestres.perfil.domain.UsuarioAutenticado;
import co.edu.uniquindio.cocinasinestres.perfil.model.Usuario;
import co.edu.uniquindio.cocinasinestres.perfil.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Resuelve quién es la usuaria de la petición.
 *
 * <p>Es el único punto donde nace una cuenta: no hay endpoint de registro. La
 * primera vez que llega un {@code firebase_uid} desconocido se crea la fila
 * (alta perezosa, ADR-05). Los demás módulos no tocan {@code UsuarioRepository}:
 * le piden la usuaria actual a este servicio.</p>
 *
 * <p>A propósito no hay {@code @Transactional} en los métodos: cada llamada al
 * repositorio abre su propia transacción. Así, cuando dos peticiones simultáneas
 * de la misma cuenta chocan contra {@code uk_usuario_firebase_uid}, la que pierde
 * puede volver a consultar sin arrastrar una transacción marcada para rollback.</p>
 */
@Service
public class UsuarioActualService {

    private static final Logger log = LoggerFactory.getLogger(UsuarioActualService.class);

    private final UsuarioRepository usuarioRepository;
    private final String correoAdminInicial;

    public UsuarioActualService(UsuarioRepository usuarioRepository, PropiedadesCocina propiedades) {
        this.usuarioRepository = usuarioRepository;
        this.correoAdminInicial = propiedades.adminInicial().correo();
    }

    /**
     * Devuelve la cuenta de ese {@code firebase_uid} y la crea si es la primera vez
     * que entra. Lo usa la capa de seguridad en cada petición autenticada.
     *
     * @param firebaseUid identificador que trae el token ya verificado
     * @param correo      correo que trae el token
     * @throws NoAutenticadoException si faltan datos del token o la cuenta está desactivada
     */
    public UsuarioAutenticado registrarSiNoExiste(String firebaseUid, String correo) {
        if (estaEnBlanco(firebaseUid) || estaEnBlanco(correo)) {
            throw new NoAutenticadoException("No pudimos identificar tu cuenta. Vuelve a entrar.");
        }

        Usuario usuario = usuarioRepository.findByFirebaseUid(firebaseUid)
                .orElseGet(() -> crear(firebaseUid, correo));

        if (!usuario.isActivo()) {
            throw new NoAutenticadoException(CodigoError.AUTH_CUENTA_INACTIVA,
                    "Tu cuenta está desactivada. Escríbenos para recuperarla.");
        }

        return aUsuarioAutenticado(usuario);
    }

    /**
     * Devuelve la usuaria de la petición en curso, puesta por la capa de seguridad.
     *
     * @throws NoAutenticadoException si la petición no está autenticada
     */
    public UsuarioAutenticado obtener() {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();

        if (autenticacion == null
                || !autenticacion.isAuthenticated()
                || !(autenticacion.getPrincipal() instanceof UsuarioAutenticado usuario)) {
            throw new NoAutenticadoException("Inicia sesión para continuar.");
        }

        return usuario;
    }

    private Usuario crear(String firebaseUid, String correo) {
        Rol rol = esAdminInicial(correo) ? Rol.ADMIN : Rol.PREDETERMINADO;
        try {
            Usuario creado = usuarioRepository.save(Usuario.nueva(firebaseUid, correo, rol));
            // Sin el correo en el log: es un dato personal (RNF-21).
            log.info("Cuenta creada en el primer ingreso: id={} rol={}", creado.getId(), rol);
            return creado;
        } catch (DataIntegrityViolationException choque) {
            // Otra petición de la misma cuenta la creó entre la consulta y el insert.
            return usuarioRepository.findByFirebaseUid(firebaseUid).orElseThrow(() -> choque);
        }
    }

    /**
     * La primera cuenta con el correo de {@code ADMIN_INICIAL_CORREO} entra como ADMIN:
     * es la forma de tener un administrador sin sembrar usuarios en una migración (ADR-16).
     */
    private boolean esAdminInicial(String correo) {
        return !estaEnBlanco(correoAdminInicial) && correoAdminInicial.equalsIgnoreCase(correo);
    }

    private UsuarioAutenticado aUsuarioAutenticado(Usuario usuario) {
        return new UsuarioAutenticado(
                usuario.getId(), usuario.getFirebaseUid(), usuario.getCorreo(), usuario.getRol(),
                usuario.isPerfilCompleto());
    }

    private boolean estaEnBlanco(String valor) {
        return valor == null || valor.isBlank();
    }
}
