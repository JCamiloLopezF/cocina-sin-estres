package co.edu.uniquindio.cocinasinestres.avisos.repository;

import co.edu.uniquindio.cocinasinestres.avisos.domain.AlmacenAvisos;
import co.edu.uniquindio.cocinasinestres.avisos.domain.Aviso;
import co.edu.uniquindio.cocinasinestres.avisos.domain.TipoAviso;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Tablas {@code aviso} y {@code hogar_preferencia_aviso} (V1, V11). */
@Repository
public class AlmacenAvisosJdbc implements AlmacenAvisos {

    private static final String COLUMNAS = "id, tipo, titulo, mensaje, enlace, accion, leido, creado_en";

    /**
     * La clave de idempotencia es UNIQUE (ADR-17). Si ya existe: un aviso sin leer toma
     * el texto nuevo; uno leído se queda como está. MySQL 8.0.19+: {@code AS nuevo} es la
     * fila que se intentó insertar.
     */
    private static final String SQL_GUARDAR = """
        INSERT INTO aviso (hogar_id, tipo, titulo, mensaje, enlace, accion, clave_idempotencia)
        VALUES (:hogar, :tipo, :titulo, :mensaje, :enlace, :accion, :clave) AS nuevo
        ON DUPLICATE KEY UPDATE
            titulo  = IF(aviso.leido, aviso.titulo, nuevo.titulo),
            mensaje = IF(aviso.leido, aviso.mensaje, nuevo.mensaje),
            enlace  = IF(aviso.leido, aviso.enlace, nuevo.enlace),
            accion  = IF(aviso.leido, aviso.accion, nuevo.accion)
        """;

    private final JdbcClient jdbc;

    public AlmacenAvisosJdbc(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void guardar(Long hogarId, Aviso aviso) {
        jdbc.sql(SQL_GUARDAR)
            .param("hogar", hogarId)
            .param("tipo", aviso.tipo().name())
            .param("titulo", aviso.titulo())
            .param("mensaje", aviso.mensaje())
            .param("enlace", aviso.enlace())
            .param("accion", aviso.accion())
            .param("clave", aviso.clave())
            .update();
    }

    @Override
    public List<AvisoGuardado> sinLeer(Long hogarId, Collection<String> claves) {
        if (claves.isEmpty()) {
            return List.of();
        }
        return jdbc.sql("SELECT " + COLUMNAS + " FROM aviso"
                + " WHERE hogar_id = :hogar AND leido = FALSE AND clave_idempotencia IN (:claves)")
            .param("hogar", hogarId)
            .param("claves", List.copyOf(claves))
            .query(AlmacenAvisosJdbc::leer)
            .list()
            .stream()
            .filter(a -> a.tipo() != null)
            .sorted(Comparator.comparing(AvisoGuardado::tipo).thenComparing(AvisoGuardado::id))
            .toList();
    }

    @Override
    public List<AvisoGuardado> desde(Long hogarId, LocalDateTime desde, int maximo) {
        return jdbc.sql("SELECT " + COLUMNAS + " FROM aviso"
                + " WHERE hogar_id = :hogar AND creado_en >= :desde"
                + " ORDER BY creado_en DESC, id DESC LIMIT :maximo")
            .param("hogar", hogarId)
            .param("desde", desde)
            .param("maximo", maximo)
            .query(AlmacenAvisosJdbc::leer)
            .list()
            .stream()
            .filter(a -> a.tipo() != null)
            .toList();
    }

    @Override
    public boolean marcarLeido(Long hogarId, Long avisoId) {
        Integer existe = jdbc.sql("SELECT COUNT(*) FROM aviso WHERE id = :id AND hogar_id = :hogar")
            .param("id", avisoId)
            .param("hogar", hogarId)
            .query(Integer.class)
            .single();
        if (Objects.requireNonNullElse(existe, 0) == 0) {
            return false;
        }
        jdbc.sql("UPDATE aviso SET leido = TRUE WHERE id = :id AND hogar_id = :hogar")
            .param("id", avisoId)
            .param("hogar", hogarId)
            .update();
        return true;
    }

    @Override
    public int marcarTodosLeidos(Long hogarId) {
        return jdbc.sql("UPDATE aviso SET leido = TRUE WHERE hogar_id = :hogar AND leido = FALSE")
            .param("hogar", hogarId)
            .update();
    }

    @Override
    public void cambiarPreferencia(Long hogarId, TipoAviso tipo, boolean activo) {
        jdbc.sql("""
                INSERT INTO hogar_preferencia_aviso (hogar_id, tipo, activo)
                VALUES (:hogar, :tipo, :activo) AS nuevo
                ON DUPLICATE KEY UPDATE activo = nuevo.activo
                """)
            .param("hogar", hogarId)
            .param("tipo", tipo.name())
            .param("activo", activo)
            .update();
    }

    @Override
    public void registrarApertura(Long hogarId, int vigentes, int mostrados, int reglas, long duracionMs) {
        try {
            jdbc.sql("""
                    INSERT INTO apertura_app (hogar_id, avisos_vigentes, avisos_mostrados, reglas_evaluadas, duracion_ms)
                    VALUES (:hogar, :vigentes, :mostrados, :reglas, :ms)
                    """)
                .param("hogar", hogarId)
                .param("vigentes", vigentes)
                .param("mostrados", mostrados)
                .param("reglas", reglas)
                .param("ms", Math.max(0, duracionMs))
                .update();
        } catch (DataAccessException e) {
            LoggerFactory.getLogger(AlmacenAvisosJdbc.class).warn("No se pudo registrar la apertura: hogarId={}", hogarId, e);
        }
    }

    private static AvisoGuardado leer(ResultSet rs, int fila) throws SQLException {
        String titulo = rs.getString("titulo");
        String mensaje = rs.getString("mensaje");
        return new AvisoGuardado(
            rs.getLong("id"),
            TipoAviso.desde(rs.getString("tipo")),
            titulo != null ? titulo : mensaje,
            mensaje,
            rs.getString("enlace"),
            rs.getString("accion"),
            rs.getBoolean("leido"),
            rs.getObject("creado_en", LocalDateTime.class));
    }
}
