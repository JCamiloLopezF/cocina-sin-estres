package co.edu.uniquindio.cocinasinestres.planificador.service;

import co.edu.uniquindio.cocinasinestres.comun.error.ConflictoException;
import co.edu.uniquindio.cocinasinestres.comun.error.DatosInvalidosException;
import co.edu.uniquindio.cocinasinestres.comun.error.RecursoNoEncontradoException;
import co.edu.uniquindio.cocinasinestres.perfil.model.Hogar;
import co.edu.uniquindio.cocinasinestres.perfil.repository.HogarRepository;
import co.edu.uniquindio.cocinasinestres.perfil.service.UsuarioActualService;
import co.edu.uniquindio.cocinasinestres.planificador.domain.NumeroWhatsapp;
import co.edu.uniquindio.cocinasinestres.planificador.dto.TiendaRequest;
import co.edu.uniquindio.cocinasinestres.planificador.dto.TiendaResponse;
import co.edu.uniquindio.cocinasinestres.planificador.model.Tienda;
import co.edu.uniquindio.cocinasinestres.planificador.repository.TiendaRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Las tiendas a las que la usuaria le pide el mercado por WhatsApp (KAN-53). El mensaje
 * lo arma la pantalla con la lista de compras; aquí solo se guardan nombre y número.
 */
@Service
public class TiendasService {

    private static final Logger log = LoggerFactory.getLogger(TiendasService.class);

    /** Tiendas que puede guardar un hogar. */
    public static final int MAXIMO_TIENDAS = 5;
    /** Largo de {@code tienda.nombre} (V14). */
    public static final int LARGO_NOMBRE = 60;

    private final TiendaRepository tiendaRepository;
    private final HogarRepository hogarRepository;
    private final UsuarioActualService usuarioActualService;

    public TiendasService(
        TiendaRepository tiendaRepository,
        HogarRepository hogarRepository,
        UsuarioActualService usuarioActualService) {
        this.tiendaRepository = tiendaRepository;
        this.hogarRepository = hogarRepository;
        this.usuarioActualService = usuarioActualService;
    }

    @Transactional(readOnly = true)
    public List<TiendaResponse> listar() {
        return tiendaRepository.findByHogarIdOrderByNombreAsc(hogarId()).stream()
            .map(TiendasService::aRespuesta)
            .toList();
    }

    @Transactional
    public TiendaResponse crear(TiendaRequest request) {
        String nombre = request == null || request.nombre() == null ? "" : request.nombre().strip();
        if (nombre.isEmpty() || nombre.length() > LARGO_NOMBRE) {
            throw new DatosInvalidosException(
                "TIENDA_DATOS_INVALIDOS", "Escribe el nombre de la tienda (hasta 60 letras).");
        }
        String whatsapp = NumeroWhatsapp.normalizar(request.whatsapp())
            .orElseThrow(() -> new DatosInvalidosException(
                "TIENDA_DATOS_INVALIDOS", "Escribe el celular de la tienda: 10 dígitos que empiezan por 3."));

        Long hogarId = hogarId();
        if (tiendaRepository.existsByHogarIdAndWhatsapp(hogarId, whatsapp)) {
            throw new ConflictoException("TIENDA_REPETIDA", "Ya tienes guardada una tienda con ese número.");
        }
        if (tiendaRepository.countByHogarId(hogarId) >= MAXIMO_TIENDAS) {
            throw new ConflictoException(
                "TIENDAS_MAXIMO", "Puedes guardar hasta 5 tiendas. Borra una para agregar otra.");
        }
        Tienda guardada = tiendaRepository.save(new Tienda(hogarId, nombre, whatsapp));
        log.info("Tienda guardada: hogarId={} tiendaId={}", hogarId, guardada.getId());
        return aRespuesta(guardada);
    }

    @Transactional
    public void borrar(Long tiendaId) {
        Tienda tienda = tiendaRepository.findByIdAndHogarId(tiendaId, hogarId())
            // Tienda de otro hogar: mismo 404 que "no existe" (RNF-22)
            .orElseThrow(() -> new RecursoNoEncontradoException("RECURSO_NO_ENCONTRADO", "Tienda no encontrada."));
        tiendaRepository.delete(tienda);
    }

    private Long hogarId() {
        Hogar hogar = hogarRepository.findByUsuario_Id(usuarioActualService.obtener().id())
            .orElseThrow(() -> new ConflictoException(
                "PLAN_SIN_PERFIL_HOGAR", "Completa el perfil de tu hogar antes de planear la semana."));
        return hogar.getId();
    }

    private static TiendaResponse aRespuesta(Tienda tienda) {
        return new TiendaResponse(tienda.getId(), tienda.getNombre(), tienda.getWhatsapp());
    }
}
