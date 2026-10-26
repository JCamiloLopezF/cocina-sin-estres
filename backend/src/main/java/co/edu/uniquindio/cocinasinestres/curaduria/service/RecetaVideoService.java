package co.edu.uniquindio.cocinasinestres.curaduria.service;

import co.edu.uniquindio.cocinasinestres.comun.error.DatosInvalidosException;
import co.edu.uniquindio.cocinasinestres.comun.error.RecursoNoEncontradoException;
import co.edu.uniquindio.cocinasinestres.curaduria.domain.AccionAuditoria;
import co.edu.uniquindio.cocinasinestres.curaduria.domain.EntidadCatalogo;
import co.edu.uniquindio.cocinasinestres.curaduria.domain.VideoYoutube;
import co.edu.uniquindio.cocinasinestres.curaduria.dto.EtiquetasRecetaRequest;
import co.edu.uniquindio.cocinasinestres.curaduria.dto.RecetaVideoRequest;
import co.edu.uniquindio.cocinasinestres.curaduria.dto.RecetaVideoResponse;
import co.edu.uniquindio.cocinasinestres.curaduria.model.AuditoriaCatalogo;
import co.edu.uniquindio.cocinasinestres.curaduria.repository.AuditoriaCatalogoRepository;
import co.edu.uniquindio.cocinasinestres.perfil.service.UsuarioActualService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * El curador carga el video de cada receta (ajuste de la sustentación) y la marca como plato
 * fuerte o como receta que les gusta a los niños (KAN-17). Cada cambio queda en la auditoría
 * del catálogo (ADR-23, RNF-24).
 */
@Service
public class RecetaVideoService {

    private final JdbcClient jdbc;
    private final AuditoriaCatalogoRepository auditoriaRepository;
    private final UsuarioActualService usuarioActualService;

    public RecetaVideoService(
        JdbcClient jdbc,
        AuditoriaCatalogoRepository auditoriaRepository,
        UsuarioActualService usuarioActualService) {
        this.jdbc = jdbc;
        this.auditoriaRepository = auditoriaRepository;
        this.usuarioActualService = usuarioActualService;
    }

    /** Recetas activas con su video, por nombre. */
    @Transactional(readOnly = true)
    public List<RecetaVideoResponse> listar() {
        return jdbc.sql("""
                SELECT id, nombre, tiempo_minutos, video_youtube_id, video_inicio_seg, video_fin_seg,
                       es_plato_fuerte, gusta_a_ninos
                FROM receta
                WHERE activa = TRUE
                ORDER BY nombre
                """)
            .query((rs, n) -> aRespuesta(
                rs.getLong("id"),
                rs.getString("nombre"),
                rs.getInt("tiempo_minutos"),
                rs.getString("video_youtube_id"),
                (Integer) rs.getObject("video_inicio_seg", Integer.class),
                (Integer) rs.getObject("video_fin_seg", Integer.class),
                rs.getBoolean("es_plato_fuerte"),
                rs.getBoolean("gusta_a_ninos")))
            .list();
    }

    @Transactional
    public RecetaVideoResponse guardar(Long recetaId, RecetaVideoRequest request) {
        RecetaVideoResponse antes = buscar(recetaId);
        String youtubeId = VideoYoutube.idDesde(request == null ? null : request.enlace());
        int inicio = request.inicioSeg() == null ? 0 : request.inicioSeg();
        VideoYoutube.validarFragmento(inicio, request.finSeg(), antes.tiempoMinutos());

        Long curador = usuarioActualService.obtener().id();
        jdbc.sql("""
                UPDATE receta
                SET video_youtube_id = :video, video_inicio_seg = :inicio, video_fin_seg = :fin,
                    modificado_por = :curador
                WHERE id = :id
                """)
            .param("video", youtubeId)
            .param("inicio", inicio)
            .param("fin", request.finSeg())
            .param("curador", curador)
            .param("id", recetaId)
            .update();
        RecetaVideoResponse despues = buscar(recetaId);
        auditar(recetaId, antes, despues, curador);
        return despues;
    }

    @Transactional
    public RecetaVideoResponse quitar(Long recetaId) {
        RecetaVideoResponse antes = buscar(recetaId);
        if (antes.youtubeId() == null) {
            return antes;
        }
        Long curador = usuarioActualService.obtener().id();
        jdbc.sql("""
                UPDATE receta
                SET video_youtube_id = NULL, video_inicio_seg = NULL, video_fin_seg = NULL,
                    modificado_por = :curador
                WHERE id = :id
                """)
            .param("curador", curador)
            .param("id", recetaId)
            .update();
        RecetaVideoResponse despues = buscar(recetaId);
        auditar(recetaId, antes, despues, curador);
        return despues;
    }

    /**
     * El curador marca si la receta es plato fuerte y si suele gustarles a los niños
     * (KAN-17). "Armar mi semana" usa estas marcas (ADR-24). Queda en la auditoría.
     */
    @Transactional
    public RecetaVideoResponse cambiarEtiquetas(Long recetaId, EtiquetasRecetaRequest request) {
        if (request == null || (request.platoFuerte() == null && request.gustaANinos() == null)) {
            throw new DatosInvalidosException("RECETA_SIN_CAMBIOS", "Indica si es plato fuerte o si les gusta a los niños.");
        }
        RecetaVideoResponse antes = buscar(recetaId);
        boolean platoFuerte = request.platoFuerte() == null ? antes.platoFuerte() : request.platoFuerte();
        boolean ninos = request.gustaANinos() == null ? antes.gustaANinos() : request.gustaANinos();
        if (platoFuerte == antes.platoFuerte() && ninos == antes.gustaANinos()) {
            return antes;
        }
        Long curador = usuarioActualService.obtener().id();
        jdbc.sql("""
                UPDATE receta
                SET es_plato_fuerte = :platoFuerte, gusta_a_ninos = :ninos, modificado_por = :curador
                WHERE id = :id
                """)
            .param("platoFuerte", platoFuerte)
            .param("ninos", ninos)
            .param("curador", curador)
            .param("id", recetaId)
            .update();
        Map<String, Object> cambios = new LinkedHashMap<>();
        if (platoFuerte != antes.platoFuerte()) {
            cambios.put("es_plato_fuerte", Map.of("antes", antes.platoFuerte(), "despues", platoFuerte));
        }
        if (ninos != antes.gustaANinos()) {
            cambios.put("gusta_a_ninos", Map.of("antes", antes.gustaANinos(), "despues", ninos));
        }
        auditoriaRepository.save(new AuditoriaCatalogo(
            EntidadCatalogo.RECETA, recetaId, AccionAuditoria.EDITAR, curador, cambios));
        return buscar(recetaId);
    }

    private RecetaVideoResponse buscar(Long recetaId) {
        return jdbc.sql("""
                SELECT id, nombre, tiempo_minutos, video_youtube_id, video_inicio_seg, video_fin_seg,
                       es_plato_fuerte, gusta_a_ninos
                FROM receta
                WHERE id = :id AND activa = TRUE
                """)
            .param("id", recetaId)
            .query((rs, n) -> aRespuesta(
                rs.getLong("id"),
                rs.getString("nombre"),
                rs.getInt("tiempo_minutos"),
                rs.getString("video_youtube_id"),
                (Integer) rs.getObject("video_inicio_seg", Integer.class),
                (Integer) rs.getObject("video_fin_seg", Integer.class),
                rs.getBoolean("es_plato_fuerte"),
                rs.getBoolean("gusta_a_ninos")))
            .optional()
            .orElseThrow(() -> new RecursoNoEncontradoException("La receta solicitada no existe."));
    }

    private static RecetaVideoResponse aRespuesta(
        long id, String nombre, int tiempo, String video, Integer inicio, Integer fin, boolean platoFuerte, boolean ninos) {
        return new RecetaVideoResponse(
            id, nombre, tiempo, VideoYoutube.maximoSegundos(tiempo), video, inicio, fin, platoFuerte, ninos);
    }

    private void auditar(Long recetaId, RecetaVideoResponse antes, RecetaVideoResponse despues, Long curador) {
        Map<String, Object> detalle = new LinkedHashMap<>();
        detalle.put("antes", descripcion(antes));
        detalle.put("despues", descripcion(despues));
        Map<String, Object> cambios = new LinkedHashMap<>();
        cambios.put("video", detalle);
        auditoriaRepository.save(new AuditoriaCatalogo(
            EntidadCatalogo.RECETA, recetaId, AccionAuditoria.EDITAR, curador, cambios));
    }

    private static String descripcion(RecetaVideoResponse receta) {
        if (receta.youtubeId() == null) {
            return null;
        }
        return receta.youtubeId() + " " + VideoYoutube.formato(receta.inicioSeg()) + "-" + VideoYoutube.formato(receta.finSeg());
    }
}
