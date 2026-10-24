package co.edu.uniquindio.cocinasinestres.planificador.model;

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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entidad JPA que mapea la tabla plan_semanal (V1).
 */
@Entity
@Table(name = "plan_semanal")
public class PlanSemanal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hogar_id", nullable = false)
    private Hogar hogar;

    // lunes de la semana planificada
    @Column(name = "semana_inicio", nullable = false)
    private LocalDate semanaInicio;

    // Dejar que la BD establezca creado_en (INSERT DEFAULT); no forzamos anotaciones Hibernate aquí.
    @Column(name = "creado_en", nullable = false, insertable = false, updatable = false)
    private LocalDateTime creadoEn;

    /** La armó la app sola (motor de avisos, ADR-24); la usuaria todavía no la confirma. V11. */
    @Column(name = "borrador", nullable = false)
    private boolean borrador;

    protected PlanSemanal() {
        // JPA
    }

    public PlanSemanal(Hogar hogar, LocalDate semanaInicio) {
        this.hogar = Objects.requireNonNull(hogar, "hogar");
        this.semanaInicio = Objects.requireNonNull(semanaInicio, "semanaInicio");
    }

    public Long getId() {
        return id;
    }

    public Hogar getHogar() {
        return hogar;
    }

    public LocalDate getSemanaInicio() {
        return semanaInicio;
    }

    public LocalDateTime getCreadoEn() {
        return creadoEn;
    }

    public boolean isBorrador() {
        return borrador;
    }

    public void setBorrador(boolean borrador) {
        this.borrador = borrador;
    }
}
