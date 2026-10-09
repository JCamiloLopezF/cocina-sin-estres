package co.edu.uniquindio.cocinasinestres.curaduria.model;

import co.edu.uniquindio.cocinasinestres.curaduria.domain.TipoCantidad;
import co.edu.uniquindio.cocinasinestres.curaduria.domain.UnidadBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/**
 * Producto disponible en el catálogo de la aplicación.
 *
 * <p>Mapea la tabla {@code producto} creada por
 * {@code V1__esquema_inicial.sql}.</p>
 */
@Entity
@Table(name = "producto")
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "hogar_id")
    private Long hogarId;

    @Column(name = "nombre", nullable = false, length = 120)
    private String nombre;

    @Column(name = "categoria", nullable = false, length = 60)
    private String categoria;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_cantidad", nullable = false, length = 10)
    private TipoCantidad tipoCantidad;

    @Enumerated(EnumType.STRING)
    @Column(name = "unidad_base", nullable = false, length = 10)
    private UnidadBase unidadBase;

    @Column(name = "unidad_compra", length = 30)
    private String unidadCompra;

    @Column(name = "cantidad_unidad_compra", precision = 12, scale = 3)
    private BigDecimal cantidadUnidadCompra;

    @Column(name = "precio_unidad_compra", precision = 12, scale = 2)
    private BigDecimal precioUnidadCompra;

    @Column(name = "capacidad_referencia", precision = 12, scale = 3)
    private BigDecimal capacidadReferencia;

    @Column(name = "es_basico", nullable = false)
    private boolean esBasico;

    @Column(name = "activo", nullable = false)
    private boolean activo;

    @Column(name = "modificado_por")
    private Long modificadoPor;

    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @Column(name = "modificado_en", nullable = false)
    private Instant modificadoEn;

    protected Producto() {
    }

    public Producto(
        Long hogarId,
        String nombre,
        String categoria,
        TipoCantidad tipoCantidad,
        UnidadBase unidadBase,
        String unidadCompra,
        BigDecimal cantidadUnidadCompra,
        BigDecimal precioUnidadCompra,
        BigDecimal capacidadReferencia,
        boolean esBasico,
        boolean activo,
        Long modificadoPor) {

        this.hogarId = hogarId;
        this.nombre = Objects.requireNonNull(nombre, "nombre");
        this.categoria = Objects.requireNonNull(categoria, "categoria");
        this.tipoCantidad = Objects.requireNonNull(tipoCantidad, "tipoCantidad");
        this.unidadBase = Objects.requireNonNull(unidadBase, "unidadBase");
        this.unidadCompra = unidadCompra;
        this.cantidadUnidadCompra = cantidadUnidadCompra;
        this.precioUnidadCompra = precioUnidadCompra;
        this.capacidadReferencia = capacidadReferencia;
        this.esBasico = esBasico;
        this.activo = activo;
        this.modificadoPor = modificadoPor;
    }

    public Long getId() {
        return id;
    }

    public Long getHogarId() {
        return hogarId;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = Objects.requireNonNull(nombre, "nombre");
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = Objects.requireNonNull(categoria, "categoria");
    }

    public TipoCantidad getTipoCantidad() {
        return tipoCantidad;
    }

    public void setTipoCantidad(TipoCantidad tipoCantidad) {
        this.tipoCantidad = Objects.requireNonNull(tipoCantidad, "tipoCantidad");
    }

    public UnidadBase getUnidadBase() {
        return unidadBase;
    }

    public void setUnidadBase(UnidadBase unidadBase) {
        this.unidadBase = Objects.requireNonNull(unidadBase, "unidadBase");
    }

    public String getUnidadCompra() {
        return unidadCompra;
    }

    public void setUnidadCompra(String unidadCompra) {
        this.unidadCompra = unidadCompra;
    }

    public BigDecimal getCantidadUnidadCompra() {
        return cantidadUnidadCompra;
    }

    public void setCantidadUnidadCompra(BigDecimal cantidadUnidadCompra) {
        this.cantidadUnidadCompra = cantidadUnidadCompra;
    }

    public BigDecimal getPrecioUnidadCompra() {
        return precioUnidadCompra;
    }

    public void setPrecioUnidadCompra(BigDecimal precioUnidadCompra) {
        this.precioUnidadCompra = precioUnidadCompra;
    }

    public BigDecimal getCapacidadReferencia() {
        return capacidadReferencia;
    }

    public void setCapacidadReferencia(BigDecimal capacidadReferencia) {
        this.capacidadReferencia = capacidadReferencia;
    }

    public boolean isEsBasico() {
        return esBasico;
    }

    public void setEsBasico(boolean esBasico) {
        this.esBasico = esBasico;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public Long getModificadoPor() {
        return modificadoPor;
    }

    public void setModificadoPor(Long modificadoPor) {
        this.modificadoPor = modificadoPor;
    }

    public Instant getModificadoEn() {
        return modificadoEn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof Producto producto)) {
            return false;
        }

        return id != null && Objects.equals(id, producto.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Producto{" +
            "id=" + id +
            ", nombre='" + nombre + '\'' +
            ", categoria='" + categoria + '\'' +
            ", tipoCantidad=" + tipoCantidad +
            ", unidadBase=" + unidadBase +
            ", activo=" + activo +
            '}';
    }
}
