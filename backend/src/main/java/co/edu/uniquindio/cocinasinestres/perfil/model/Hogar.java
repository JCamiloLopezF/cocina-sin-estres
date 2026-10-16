package co.edu.uniquindio.cocinasinestres.perfil.model;

import co.edu.uniquindio.cocinasinestres.perfil.model.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Representa el perfil de un hogar.
 * Corresponde a la tabla hogar de la migración inicial.
 */
@Entity
@Table(name = "hogar")
public class Hogar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;

    @Column(name = "presupuesto_monto", nullable = false, precision = 12, scale = 2)
    private BigDecimal presupuestoMonto;

    @Column(name = "periodicidad_ingreso", nullable = false, length = 10)
    private String periodicidadIngreso;

    @Column(name = "dia_pago_1", nullable = false)
    private Integer diaPago1;

    @Column(name = "dia_pago_2")
    private Integer diaPago2;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "dias_sin_tiempo", columnDefinition = "json")
    private String diasSinTiempo;

    @Column(name = "restricciones", columnDefinition = "text")
    private String restricciones;

    @Column(name = "otras_condiciones", columnDefinition = "text")
    private String otrasCondiciones;

    @Column(name = "sin_restricciones", nullable = false)
    private boolean sinRestricciones = false;

    @Column(name = "consentimiento_datos_en")
    private LocalDateTime consentimientoDatosEn;

    @Column(
        name = "creado_en",
        nullable = false,
        insertable = false,
        updatable = false
    )
    private LocalDateTime creadoEn;

    @Column(
        name = "actualizado_en",
        nullable = false,
        insertable = false,
        updatable = false
    )
    private LocalDateTime actualizadoEn;

    protected Hogar() {
        // JPA
    }

    public Hogar(
        Usuario usuario,
        BigDecimal presupuestoMonto,
        String periodicidadIngreso,
        Integer diaPago1,
        Integer diaPago2) {

        this.usuario = Objects.requireNonNull(usuario, "El usuario es obligatorio");
        this.presupuestoMonto = Objects.requireNonNull(
            presupuestoMonto, "El presupuesto es obligatorio");
        this.periodicidadIngreso = Objects.requireNonNull(
            periodicidadIngreso, "La periodicidad es obligatoria");
        this.diaPago1 = Objects.requireNonNull(
            diaPago1, "El primer día de pago es obligatorio");
        this.diaPago2 = diaPago2;
    }

    public Long getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public BigDecimal getPresupuestoMonto() {
        return presupuestoMonto;
    }

    public void setPresupuestoMonto(BigDecimal presupuestoMonto) {
        this.presupuestoMonto = Objects.requireNonNull(
            presupuestoMonto, "El presupuesto es obligatorio");
    }

    public String getPeriodicidadIngreso() {
        return periodicidadIngreso;
    }

    public void setPeriodicidadIngreso(String periodicidadIngreso) {
        this.periodicidadIngreso = Objects.requireNonNull(
            periodicidadIngreso, "La periodicidad es obligatoria");
    }

    public Integer getDiaPago1() {
        return diaPago1;
    }

    public void setDiaPago1(Integer diaPago1) {
        this.diaPago1 = Objects.requireNonNull(
            diaPago1, "El primer día de pago es obligatorio");
    }

    public Integer getDiaPago2() {
        return diaPago2;
    }

    public void setDiaPago2(Integer diaPago2) {
        this.diaPago2 = diaPago2;
    }

    public String getDiasSinTiempo() {
        return diasSinTiempo;
    }

    public void setDiasSinTiempo(String diasSinTiempo) {
        this.diasSinTiempo = diasSinTiempo;
    }

    public String getRestricciones() {
        return restricciones;
    }

    public void setRestricciones(String restricciones) {
        this.restricciones = restricciones;
    }

    public String getOtrasCondiciones() {
        return otrasCondiciones;
    }

    public void setOtrasCondiciones(String otrasCondiciones) {
        this.otrasCondiciones = otrasCondiciones;
    }

    public boolean isSinRestricciones() {
        return sinRestricciones;
    }

    public void setSinRestricciones(boolean sinRestricciones) {
        this.sinRestricciones = sinRestricciones;
    }

    public LocalDateTime getConsentimientoDatosEn() {
        return consentimientoDatosEn;
    }

    public void registrarConsentimiento(LocalDateTime fecha) {
        this.consentimientoDatosEn = Objects.requireNonNull(
            fecha, "La fecha del consentimiento es obligatoria");
    }

    public void revocarConsentimiento() {
        this.consentimientoDatosEn = null;
    }

    public LocalDateTime getCreadoEn() {
        return creadoEn;
    }

    public LocalDateTime getActualizadoEn() {
        return actualizadoEn;
    }
}
