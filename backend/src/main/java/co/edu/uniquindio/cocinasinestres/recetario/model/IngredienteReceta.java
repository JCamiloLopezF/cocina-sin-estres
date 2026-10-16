package co.edu.uniquindio.cocinasinestres.recetario.model;

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
 * Ingrediente de una {@link Receta}: un producto del catálogo global y la cantidad
 * que pide la receta.
 *
 * <p>Mapea la tabla {@code ingrediente_receta} creada por
 * {@code V1__esquema_inicial.sql}. Un producto aparece una sola vez por receta
 * ({@code uk_ingrediente_receta_receta_id_producto_id}). Que el producto sea del
 * catálogo global ({@code producto.hogar_id IS NULL}) lo valida la aplicación, no la
 * base de datos.</p>
 *
 * <p>Se crea siempre desde {@link Receta#agregarIngrediente}.</p>
 */
@Entity
@Table(name = "ingrediente_receta")
public class IngredienteReceta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receta_id", nullable = false)
    private Receta receta;

    @Column(name = "producto_id", nullable = false)
    private Long productoId;

    /** En la unidad base del producto (g, ml o unidad), para las porciones de la receta. */
    @Column(name = "cantidad", nullable = false, precision = 12, scale = 3)
    private BigDecimal cantidad;

    @Column(name = "opcional", nullable = false)
    private boolean opcional;

    protected IngredienteReceta() {
    }

    IngredienteReceta(Receta receta, Long productoId, BigDecimal cantidad, boolean opcional) {
        this.receta = Objects.requireNonNull(receta, "receta");
        this.productoId = Objects.requireNonNull(productoId, "productoId");
        this.cantidad = Objects.requireNonNull(cantidad, "cantidad");

        if (cantidad.signum() <= 0) {
            throw new IllegalArgumentException("cantidad debe ser mayor que cero");
        }

        this.opcional = opcional;
    }

    public Long getId() {
        return id;
    }

    public Receta getReceta() {
        return receta;
    }

    public Long getProductoId() {
        return productoId;
    }

    public BigDecimal getCantidad() {
        return cantidad;
    }

    public boolean isOpcional() {
        return opcional;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof IngredienteReceta otro)) {
            return false;
        }

        return id != null && Objects.equals(id, otro.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "IngredienteReceta{" +
            "id=" + id +
            ", productoId=" + productoId +
            ", cantidad=" + cantidad +
            ", opcional=" + opcional +
            '}';
    }
}
