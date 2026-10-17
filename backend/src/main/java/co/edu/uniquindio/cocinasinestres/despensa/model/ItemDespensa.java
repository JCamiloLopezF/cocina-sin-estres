package co.edu.uniquindio.cocinasinestres.despensa.model;

import co.edu.uniquindio.cocinasinestres.despensa.domain.DatosItemDespensa;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Un producto en la despensa de un hogar, con su cantidad, vencimiento y mínimo.
 *
 * <p>Mapea la tabla {@code item_despensa} de {@code V1__esquema_inicial.sql}. Un hogar
 * tiene como máximo una fila por producto ({@code uk_item_despensa_hogar_id_producto_id}).
 * La cantidad siempre está en la unidad base del producto; en los productos a granel
 * sale de un nivel y {@code esEstimada} es verdadero (ADR-15).</p>
 */
@Entity
@Table(name = "item_despensa")
public class ItemDespensa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "hogar_id", nullable = false, updatable = false)
    private Long hogarId;

    @Column(name = "producto_id", nullable = false, updatable = false)
    private Long productoId;

    @Column(name = "cantidad", nullable = false, precision = 12, scale = 3)
    private BigDecimal cantidad;

    @Column(name = "es_estimada", nullable = false)
    private boolean esEstimada;

    @Column(name = "cantidad_minima", precision = 12, scale = 3)
    private BigDecimal cantidadMinima;

    @Column(name = "vence_en")
    private LocalDate venceEn;

    @Column(name = "ubicacion", length = 40)
    private String ubicacion;

    // Las dos marcas de tiempo las pone MySQL; Hibernate solo las lee.
    @Generated(event = EventType.INSERT)
    @Column(name = "creado_en")
    private Instant creadoEn;

    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @Column(name = "actualizado_en")
    private Instant actualizadoEn;

    protected ItemDespensa() {
        // Requerido por JPA.
    }

    /**
     * Crea el registro de un producto en la despensa de un hogar.
     *
     * @param hogarId    hogar dueño de la despensa
     * @param productoId producto del catálogo
     * @param datos      datos ya validados por {@code ValidadorItemDespensa}
     */
    public ItemDespensa(Long hogarId, Long productoId, DatosItemDespensa datos) {
        this.hogarId = Objects.requireNonNull(hogarId, "hogarId");
        this.productoId = Objects.requireNonNull(productoId, "productoId");
        aplicar(datos);
    }

    /**
     * Reemplaza la cantidad, el mínimo, el vencimiento y la ubicación. El hogar y el
     * producto no cambian: para otro producto se crea otro registro.
     */
    public void actualizar(DatosItemDespensa datos) {
        aplicar(datos);
    }

    private void aplicar(DatosItemDespensa datos) {
        Objects.requireNonNull(datos, "datos");

        this.cantidad = datos.cantidad();
        this.esEstimada = datos.esEstimada();
        this.cantidadMinima = datos.cantidadMinima();
        this.venceEn = datos.fechaVencimiento();
        this.ubicacion = datos.ubicacion();
    }

    public Long getId() {
        return id;
    }

    public Long getHogarId() {
        return hogarId;
    }

    public Long getProductoId() {
        return productoId;
    }

    public BigDecimal getCantidad() {
        return cantidad;
    }

    public boolean isEsEstimada() {
        return esEstimada;
    }

    public BigDecimal getCantidadMinima() {
        return cantidadMinima;
    }

    public LocalDate getVenceEn() {
        return venceEn;
    }

    public String getUbicacion() {
        return ubicacion;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }

    public Instant getActualizadoEn() {
        return actualizadoEn;
    }

    @Override
    public boolean equals(Object otro) {
        if (this == otro) {
            return true;
        }

        if (!(otro instanceof ItemDespensa item)) {
            return false;
        }

        return id != null && id.equals(item.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "ItemDespensa{" +
            "id=" + id +
            ", hogarId=" + hogarId +
            ", productoId=" + productoId +
            ", cantidad=" + cantidad +
            ", esEstimada=" + esEstimada +
            '}';
    }
}
