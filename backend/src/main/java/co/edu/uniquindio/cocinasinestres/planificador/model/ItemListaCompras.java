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
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Mapea la tabla item_lista_compras (V1).
 */
@Entity
@Table(name = "item_lista_compras")
public class ItemListaCompras {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lista_compras_id", nullable = false)
    private ListaCompras listaCompras;

    @Column(name = "producto_id", nullable = false)
    private Long productoId;

    @Column(name = "cantidad", nullable = false, precision = 12, scale = 3)
    private BigDecimal cantidad;

    @Column(name = "costo_estimado", nullable = false, precision = 12, scale = 2)
    private BigDecimal costoEstimado;

    @Column(name = "prioridad", nullable = false)
    private Integer prioridad;

    @Column(name = "comprado", nullable = false)
    private boolean comprado = false;

    protected ItemListaCompras() {
        // JPA
    }

    public ItemListaCompras(ListaCompras listaCompras, Long productoId, BigDecimal cantidad,
                            BigDecimal costoEstimado, Integer prioridad) {
        this.listaCompras = Objects.requireNonNull(listaCompras, "listaCompras");
        this.productoId = Objects.requireNonNull(productoId, "productoId");
        this.cantidad = Objects.requireNonNull(cantidad, "cantidad");
        this.costoEstimado = Objects.requireNonNull(costoEstimado, "costoEstimado");
        this.prioridad = Objects.requireNonNull(prioridad, "prioridad");
    }

    public Long getId() {
        return id;
    }

    public ListaCompras getListaCompras() {
        return listaCompras;
    }

    public Long getProductoId() {
        return productoId;
    }

    public BigDecimal getCantidad() {
        return cantidad;
    }

    public BigDecimal getCostoEstimado() {
        return costoEstimado;
    }

    public Integer getPrioridad() {
        return prioridad;
    }

    public boolean isComprado() {
        return comprado;
    }

    public void setComprado(boolean comprado) {
        this.comprado = comprado;
    }
}
