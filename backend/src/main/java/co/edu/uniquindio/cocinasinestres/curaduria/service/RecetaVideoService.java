package co.edu.uniquindio.cocinasinestres.curaduria.service;

import co.edu.uniquindio.cocinasinestres.comun.error.RecursoNoEncontradoException;
import co.edu.uniquindio.cocinasinestres.curaduria.domain.AccionAuditoria;
import co.edu.uniquindio.cocinasinestres.curaduria.domain.EntidadCatalogo;
import co.edu.uniquindio.cocinasinestres.curaduria.domain.VideoYoutube;
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
 * El curador carga el video de cada receta (ajuste de la sustentación). Cada cambio queda
 * en la auditoría del catálogo (ADR-23, RNF-24).
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
                SELECT id, nombre, tiempo_minutos, video_youtube_id, video_inicio_seg, video_fin_seg
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
                (Integer) rs.getObject("video_fin_seg", Integer.class)))
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

    private RecetaVideoResponse buscar(Long recetaId) {
        return jdbc.sql("""
                SELECT id, nombre, tiempo_minutos, video_youtube_id, video_inicio_seg, video_fin_seg
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
                (Integer) rs.getObject("video_fin_seg", Integer.class)))
            .optional()
            .orElseThrow(() -> new RecursoNoEncontradoException("La receta solicitada no existe."));
    }

    private static RecetaVideoResponse aRespuesta(
        long id, String nombre, int tiempo, String video, Integer inicio, Integer fin) {
        return new RecetaVideoResponse(id, nombre, tiempo, VideoYoutube.maximoSegundos(tiempo), video, inicio, fin);
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
