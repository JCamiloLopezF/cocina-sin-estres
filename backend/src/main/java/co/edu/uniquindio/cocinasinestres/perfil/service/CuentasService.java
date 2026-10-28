package co.edu.uniquindio.cocinasinestres.perfil.service;

import co.edu.uniquindio.cocinasinestres.comun.error.ConflictoException;
import co.edu.uniquindio.cocinasinestres.comun.error.DatosInvalidosException;
import co.edu.uniquindio.cocinasinestres.comun.error.RecursoNoEncontradoException;
import co.edu.uniquindio.cocinasinestres.perfil.domain.CuentaResumen;
import co.edu.uniquindio.cocinasinestres.perfil.domain.Rol;
import co.edu.uniquindio.cocinasinestres.perfil.model.Usuario;
import co.edu.uniquindio.cocinasinestres.perfil.repository.UsuarioRepository;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gestión de cuentas para el administrador: listar, cambiar el rol y activar o
 * desactivar (ADR-16). El módulo {@code admin} lo llama; el repositorio de cuentas
 * sigue siendo exclusivo de {@code perfil} (RNF-26).
 *
 * <p>El rol se lee de la base en cada petición autenticada, así que un cambio aplica
 * desde la siguiente petición de esa cuenta, sin que tenga que volver a entrar.</p>
 */
@Service
public class CuentasService {

    /** El rol enviado no es USUARIA, CURADOR ni ADMIN. */
    public static final String CODIGO_ROL_INVALIDO = "ADMIN_ROL_INVALIDO";

    /** La petición no trae ni rol ni estado. */
    public static final String CODIGO_SIN_CAMBIOS = "ADMIN_SIN_CAMBIOS";

    /** Un administrador no puede quitarse su propio rol ni desactivarse: se quedaría sin acceso. */
    public static final String CODIGO_CUENTA_PROPIA = "ADMIN_CUENTA_PROPIA";

    private static final Logger log = LoggerFactory.getLogger(CuentasService.class);

    private final UsuarioRepository usuarioRepository;

    public CuentasService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    /** Todas las cuentas de personas, de la más antigua a la más reciente (sin la de sistema). */
    @Transactional(readOnly = true)
    public List<CuentaResumen> listar() {
        return usuarioRepository.findAll(Sort.by(Sort.Direction.ASC, "id")).stream()
                .filter(usuario -> !Usuario.UID_SISTEMA.equals(usuario.getFirebaseUid()))
                .map(CuentasService::aResumen)
                .toList();
    }

    /**
     * Cambia el rol, el estado o ambos de una cuenta.
     *
     * @param cuentaId cuenta que se cambia
     * @param rol      nuevo rol como texto ({@code USUARIA}, {@code CURADOR} o {@code ADMIN}); {@code null} lo deja igual
     * @param activo   nuevo estado; {@code null} lo deja igual
     * @param adminId  administrador que hace el cambio
     * @throws DatosInvalidosException      si no hay nada que cambiar o el rol no existe
     * @throws ConflictoException           si el administrador intenta cambiarse a sí mismo
     * @throws RecursoNoEncontradoException si la cuenta no existe
     */
    @Transactional
    public CuentaResumen cambiar(Long cuentaId, String rol, Boolean activo, Long adminId) {
        if (rol == null && activo == null) {
            throw new DatosInvalidosException(CODIGO_SIN_CAMBIOS, "Indica el nuevo rol o si la cuenta queda activa.");
        }
        Rol nuevoRol = rol == null ? null : leerRol(rol);
        if (cuentaId.equals(adminId)) {
            throw new ConflictoException(CODIGO_CUENTA_PROPIA,
                    "No puedes cambiar tu propio rol ni desactivar tu cuenta. Pídeselo a otro administrador.");
        }

        Usuario cuenta = usuarioRepository.findById(cuentaId)
                // La cuenta de sistema no se administra: para el administrador no existe.
                .filter(usuario -> !Usuario.UID_SISTEMA.equals(usuario.getFirebaseUid()))
                .orElseThrow(() -> new RecursoNoEncontradoException("La cuenta no existe."));
        if (nuevoRol != null) {
            cuenta.setRol(nuevoRol);
        }
        if (activo != null) {
            cuenta.setActivo(activo);
        }
        Usuario guardada = usuarioRepository.save(cuenta);
        // Sin correos en el log (RNF-21).
        log.info("Cuenta cambiada por el administrador {}: id={} rol={} activo={}",
                adminId, guardada.getId(), guardada.getRol(), guardada.isActivo());
        return aResumen(guardada);
    }

    private static Rol leerRol(String texto) {
        try {
            return Rol.valueOf(texto.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException desconocido) {
            throw new DatosInvalidosException(CODIGO_ROL_INVALIDO, "El rol debe ser USUARIA, CURADOR o ADMIN.");
        }
    }

    private static CuentaResumen aResumen(Usuario usuario) {
        return new CuentaResumen(usuario.getId(), usuario.getCorreo(), usuario.getRol(), usuario.isActivo(),
                usuario.isPerfilCompleto(), usuario.getCreadoEn());
    }
}
