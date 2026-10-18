package co.edu.uniquindio.cocinasinestres.avisos.model;

import co.edu.uniquindio.cocinasinestres.perfil.model.Hogar;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

/**
 * Entidad JPA que mapea la tabla {@code aviso}.
 */
@Entity
@Table(name = "aviso")
public class Aviso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relación con hogar (FK hogar_id)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hogar_id", nullable = false)
    private Hogar hogar;

    @Column(name = "tipo", length = 25, nullable = false)
    private String tipo;

    @Column(name = "mensaje", length = 255, nullable = false)
    private String mensaje;

    @Column(name = "clave_idempotencia", length = 120, nullable = false, unique = true)
    private String claveIdempotencia;

    @Column(name = "leido", nullable = false)
    private boolean leido = false;

    // creado_en lo gestiona la BD por defecto; no lo insertamos desde Java
    @Generated(event = {EventType.INSERT})
    @Column(name = "creado_en", nullable = false, insertable = false, updatable = false)
    private LocalDateTime creadoEn;

    protected Aviso() {
        // JPA
    }

    public Aviso(Hogar hogar, String tipo, String mensaje, String claveIdempotencia) {
        this.hogar = hogar;
        this.tipo = tipo;
        this.mensaje = mensaje;
        this.claveIdempotencia = claveIdempotencia;
        this.leido = false;
    }

    // Getters y setters mínimos

    public Long getId() {
        return id;
    }

    public Hogar getHogar() {
        return hogar;
    }

    public void setHogar(Hogar hogar) {
        this.hogar = hogar;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public String getClaveIdempotencia() {
        return claveIdempotencia;
    }

    public void setClaveIdempotencia(String claveIdempotencia) {
        this.claveIdempotencia = claveIdempotencia;
    }

    public boolean isLeido() {
        return leido;
    }

    public void setLeido(boolean leido) {
        this.leido = leido;
    }

    public LocalDateTime getCreadoEn() {
        return creadoEn;
    }
}
