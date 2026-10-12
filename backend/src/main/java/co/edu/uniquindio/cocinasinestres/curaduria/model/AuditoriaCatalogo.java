package co.edu.uniquindio.cocinasinestres.curaduria.model;

import co.edu.uniquindio.cocinasinestres.curaduria.domain.AccionAuditoria;
import co.edu.uniquindio.cocinasinestres.curaduria.domain.EntidadCatalogo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.generator.EventType;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Registro de un cambio hecho al catálogo (RNF-24, ADR-23).
 *
 * <p>Mapea la tabla {@code auditoria_catalogo} creada por
 * {@code V1__esquema_inicial.sql}. Es un registro histórico: solo se inserta.</p>
 */
@Entity
@Table(name = "auditoria_catalogo")
public class AuditoriaCatalogo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "entidad", nullable = false, length = 20, updatable = false)
    private EntidadCatalogo entidad;

    @Column(name = "entidad_id", nullable = false, updatable = false)
    private Long entidadId;

    @Enumerated(EnumType.STRING)
    @Column(name = "accion", nullable = false, length = 12, updatable = false)
    private AccionAuditoria accion;

    @Column(name = "usuario_id", nullable = false, updatable = false)
    private Long usuarioId;

    @Generated(event = EventType.INSERT)
    @Column(name = "fecha", nullable = false, updatable = false)
    private Instant fecha;

    /**
     * Campos cambiados: {@code {"campo": {"antes": ..., "despues": ...}}}.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "cambios", columnDefinition = "json", updatable = false)
    private Map<String, Object> cambios;

    protected AuditoriaCatalogo() {
    }

    public AuditoriaCatalogo(
        EntidadCatalogo entidad,
        Long entidadId,
        AccionAuditoria accion,
        Long usuarioId,
        Map<String, Object> cambios) {

        this.entidad = Objects.requireNonNull(entidad, "entidad");
        this.entidadId = Objects.requireNonNull(entidadId, "entidadId");
        this.accion = Objects.requireNonNull(accion, "accion");
        this.usuarioId = Objects.requireNonNull(usuarioId, "usuarioId");
        this.cambios = cambios;
    }

    public Long getId() {
        return id;
    }

    public EntidadCatalogo getEntidad() {
        return entidad;
    }

    public Long getEntidadId() {
        return entidadId;
    }

    public AccionAuditoria getAccion() {
        return accion;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public Instant getFecha() {
        return fecha;
    }

    public Map<String, Object> getCambios() {
        return cambios;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof AuditoriaCatalogo otra)) {
            return false;
        }

        return id != null && Objects.equals(id, otra.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
