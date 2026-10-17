package co.edu.uniquindio.cocinasinestres.planificador.model;

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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Entidad JPA que mapea la tabla comida_plan (V1).
 */
@Entity
@Table(name = "comida_plan")
public class ComidaPlan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // FK a plan_semanal
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_semanal_id", nullable = false)
    private PlanSemanal planSemanal;

    // 1 = lunes … 7 = domingo
    // Columna TINYINT: sin esto, la validación de Hibernate espera INTEGER.
    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "dia", nullable = false)
    private Integer dia;

    @Column(name = "tipo_comida", nullable = false, length = 10)
    private String tipoComida;

    @Column(name = "receta_id", nullable = false)
    private Long recetaId;

    @Column(name = "preparada", nullable = false)
    private boolean preparada = false;

    @Column(name = "preparada_en")
    private LocalDateTime preparadaEn;

    @Column(name = "id_cambio_cliente", length = 64, nullable = true, unique = true)
    private String idCambioCliente;

    protected ComidaPlan() {
        // JPA
    }

    public ComidaPlan(PlanSemanal planSemanal, Integer dia, String tipoComida, Long recetaId, String idCambioCliente) {
        this.planSemanal = Objects.requireNonNull(planSemanal, "planSemanal");
        this.dia = Objects.requireNonNull(dia, "dia");
        this.tipoComida = Objects.requireNonNull(tipoComida, "tipoComida");
        this.recetaId = Objects.requireNonNull(recetaId, "recetaId");
        this.idCambioCliente = idCambioCliente;
    }

    public Long getId() {
        return id;
    }

    public PlanSemanal getPlanSemanal() {
        return planSemanal;
    }

    public Integer getDia() {
        return dia;
    }

    public void setDia(Integer dia) {
        this.dia = dia;
    }

    public String getTipoComida() {
        return tipoComida;
    }

    public void setTipoComida(String tipoComida) {
        this.tipoComida = tipoComida;
    }

    public Long getRecetaId() {
        return recetaId;
    }

    public void setRecetaId(Long recetaId) {
        this.recetaId = recetaId;
    }

    public boolean isPreparada() {
        return preparada;
    }

    public void setPreparada(boolean preparada) {
        this.preparada = preparada;
    }

    public LocalDateTime getPreparadaEn() {
        return preparadaEn;
    }

    public void setPreparadaEn(LocalDateTime preparadaEn) {
        this.preparadaEn = preparadaEn;
    }

    public String getIdCambioCliente() {
        return idCambioCliente;
    }

    public void setIdCambioCliente(String idCambioCliente) {
        this.idCambioCliente = idCambioCliente;
    }
}
