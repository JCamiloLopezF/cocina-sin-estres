package co.edu.uniquindio.cocinasinestres.curaduria.service;

import co.edu.uniquindio.cocinasinestres.comun.error.DatosInvalidosException;
import co.edu.uniquindio.cocinasinestres.comun.error.RecursoNoEncontradoException;
import co.edu.uniquindio.cocinasinestres.curaduria.domain.AccionAuditoria;
import co.edu.uniquindio.cocinasinestres.curaduria.domain.EntidadCatalogo;
import co.edu.uniquindio.cocinasinestres.curaduria.domain.ReglaReemplazo;
import co.edu.uniquindio.cocinasinestres.curaduria.dto.ReemplazoResponse;
import co.edu.uniquindio.cocinasinestres.curaduria.model.AuditoriaCatalogo;
import co.edu.uniquindio.cocinasinestres.curaduria.repository.AuditoriaCatalogoRepository;
import co.edu.uniquindio.cocinasinestres.perfil.service.UsuarioActualService;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tabla de reemplazos para adaptar recetas a una alergia (ADR-24).
 *
 * <p>La aplicación la mantiene sola con {@link ReglaReemplazo}: al arrancar y cada vez
 * que cambia un producto del catálogo. El curador puede agregar un reemplazo que la
 * regla no ve, quitar uno que no tenga sentido o volver a usarlo; lo que él decide
 * queda con origen {@code CURADOR} y el recálculo no lo toca.</p>
 */
@Service
public class ReemplazosService {

    private static final Logger log = LoggerFactory.getLogger(ReemplazosService.class);
    private static final String ERROR = "REEMPLAZO_INVALIDO";

    private final JdbcClient jdbc;
    private final AuditoriaCatalogoRepository auditoriaRepository;
    private final UsuarioActualService usuarioActualService;

    public ReemplazosService(
        JdbcClient jdbc,
        AuditoriaCatalogoRepository auditoriaRepository,
        UsuarioActualService usuarioActualService) {
        this.jdbc = jdbc;
        this.auditoriaRepository = auditoriaRepository;
        this.usuarioActualService = usuarioActualService;
    }

    /**
     * Agrega los reemplazos que propone la regla y borra los que la aplicación había
     * propuesto y ya no aplican. No toca lo que decidió un curador.
     *
     * @return cuántas filas cambiaron
     */
    @Transactional
    public int recalcularAutomaticos() {
        Map<Long, String> categorias = new HashMap<>();
        jdbc.sql("SELECT id, categoria FROM producto WHERE hogar_id IS NULL AND activo = TRUE")
            .query(rs -> {
                categorias.put(rs.getLong("id"), rs.getString("categoria"));
            });
        Map<Long, Set<String>> alergenos = new HashMap<>();
        jdbc.sql("""
                SELECT pr.producto_id, r.codigo
                FROM producto_restriccion pr
                JOIN restriccion r ON r.id = pr.restriccion_id
                WHERE r.tipo = 'ALERGIA'
                """)
            .query(rs -> {
                alergenos.computeIfAbsent(rs.getLong("producto_id"), k -> new HashSet<>()).add(rs.getString("codigo"));
            });

        Set<ReglaReemplazo.Par> propuestos = ReglaReemplazo.proponer(ReglaReemplazo.catalogo(categorias, alergenos));

        Map<ReglaReemplazo.Par, String> existentes = new HashMap<>();
        jdbc.sql("SELECT producto_id, reemplazo_id, origen FROM reemplazo_producto")
            .query(rs -> {
                existentes.put(
                    new ReglaReemplazo.Par(rs.getLong("producto_id"), rs.getLong("reemplazo_id")),
                    rs.getString("origen"));
            });

        int cambios = 0;
        for (ReglaReemplazo.Par par : propuestos) {
            if (!existentes.containsKey(par)) {
                // IGNORE: si dos instancias arrancan a la vez, la segunda no falla.
                cambios += jdbc.sql("""
                        INSERT IGNORE INTO reemplazo_producto (producto_id, reemplazo_id, origen, activo)
                        VALUES (:producto, :reemplazo, 'SISTEMA', TRUE)
                        """)
                    .param("producto", par.productoId())
                    .param("reemplazo", par.reemplazoId())
                    .update();
            }
        }
        for (Map.Entry<ReglaReemplazo.Par, String> fila : existentes.entrySet()) {
            if ("SISTEMA".equals(fila.getValue()) && !propuestos.contains(fila.getKey())) {
                cambios += jdbc.sql("""
                        DELETE FROM reemplazo_producto
                        WHERE producto_id = :producto AND reemplazo_id = :reemplazo AND origen = 'SISTEMA'
                        """)
                    .param("producto", fila.getKey().productoId())
                    .param("reemplazo", fila.getKey().reemplazoId())
                    .update();
            }
        }
        if (cambios > 0) {
            log.info("Reemplazos automáticos recalculados: {} cambios", cambios);
        }
        return cambios;
    }

    /** Reemplazos de un producto del catálogo, activos e inactivos. */
    @Transactional(readOnly = true)
    public List<ReemplazoResponse> listar(Long productoId) {
        nombreDelCatalogo(productoId);
        return jdbc.sql("""
                SELECT rp.reemplazo_id, p.nombre, p.categoria, rp.origen, rp.activo
                FROM reemplazo_producto rp
                JOIN producto p ON p.id = rp.reemplazo_id
                WHERE rp.producto_id = :producto
                ORDER BY rp.activo DESC, p.nombre
                """)
            .param("producto", productoId)
            .query((rs, n) -> new ReemplazoResponse(
                rs.getLong("reemplazo_id"),
                rs.getString("nombre"),
                rs.getString("categoria"),
                rs.getString("origen"),
                rs.getBoolean("activo")))
            .list();
    }

    /** El curador agrega un reemplazo o vuelve a usar uno que había quitado. */
    @Transactional
    public List<ReemplazoResponse> usar(Long productoId, Long reemplazoId) {
        return decidir(productoId, reemplazoId, true);
    }

    /** El curador quita un reemplazo; la aplicación no lo vuelve a proponer. */
    @Transactional
    public List<ReemplazoResponse> quitar(Long productoId, Long reemplazoId) {
        return decidir(productoId, reemplazoId, false);
    }

    private List<ReemplazoResponse> decidir(Long productoId, Long reemplazoId, boolean activo) {
        String producto = nombreDelCatalogo(productoId);
        String reemplazo = nombreDelCatalogo(reemplazoId);
        if (productoId.equals(reemplazoId)) {
            throw new DatosInvalidosException(ERROR, "Un producto no se puede reemplazar por sí mismo.");
        }
        Long curador = usuarioActualService.obtener().id();

        Optional<Boolean> actual = jdbc.sql("""
                SELECT activo FROM reemplazo_producto
                WHERE producto_id = :producto AND reemplazo_id = :reemplazo
                """)
            .param("producto", productoId)
            .param("reemplazo", reemplazoId)
            .query(Boolean.class)
            .optional();

        if (actual.isPresent()) {
            jdbc.sql("""
                    UPDATE reemplazo_producto
                    SET activo = :activo, origen = 'CURADOR', modificado_por = :curador
                    WHERE producto_id = :producto AND reemplazo_id = :reemplazo
                    """)
                .param("activo", activo)
                .param("curador", curador)
                .param("producto", productoId)
                .param("reemplazo", reemplazoId)
                .update();
        } else {
            jdbc.sql("""
                    INSERT INTO reemplazo_producto (producto_id, reemplazo_id, origen, activo, modificado_por)
                    VALUES (:producto, :reemplazo, 'CURADOR', :activo, :curador)
                    """)
                .param("producto", productoId)
                .param("reemplazo", reemplazoId)
                .param("activo", activo)
                .param("curador", curador)
                .update();
        }

        if (actual.isEmpty() || actual.get() != activo) {
            Map<String, Object> cambios = new LinkedHashMap<>();
            Map<String, Object> detalle = new LinkedHashMap<>();
            detalle.put("antes", actual.isPresent() && actual.get() ? reemplazo : null);
            detalle.put("despues", activo ? reemplazo : null);
            cambios.put("reemplazo", detalle);
            auditoriaRepository.save(new AuditoriaCatalogo(
                EntidadCatalogo.PRODUCTO, productoId, AccionAuditoria.EDITAR, curador, cambios));
            log.info("Reemplazo {}: {} -> {}", activo ? "en uso" : "quitado", producto, reemplazo);
        }
        return listar(productoId);
    }

    private String nombreDelCatalogo(Long productoId) {
        return jdbc.sql("SELECT nombre FROM producto WHERE id = :id AND hogar_id IS NULL")
            .param("id", productoId)
            .query(String.class)
            .optional()
            .orElseThrow(() -> new RecursoNoEncontradoException("El producto solicitado no existe."));
    }
}
