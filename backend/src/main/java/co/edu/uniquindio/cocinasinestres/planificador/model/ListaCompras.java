package co.edu.uniquindio.cocinasinestres.planificador.model;

import co.edu.uniquindio.cocinasinestres.planificador.model.PlanSemanal;
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
import java.util.Objects;

/**
 * Mapea la tabla lista_compras (V1).
 */
@Entity
@Table(name = "lista_compras")
public class ListaCompras {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_semanal_id", nullable = false)
    private PlanSemanal planSemanal;

    @Column(name = "generada_en", nullable = false, insertable = false, updatable = false)
    private LocalDateTime generadaEn;

    protected ListaCompras() {
        // JPA
    }

    public ListaCompras(PlanSemanal planSemanal) {
        this.planSemanal = Objects.requireNonNull(planSemanal, "planSemanal");
    }

    public Long getId() {
        return id;
    }

    public PlanSemanal getPlanSemanal() {
        return planSemanal;
    }

    public LocalDateTime getGeneradaEn() {
        return generadaEn;
    }
}
