package co.edu.uniquindio.cocinasinestres.planificador.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Mapea la tabla tienda (V14): una tienda del barrio a la que la usuaria le pide el
 * mercado por WhatsApp (KAN-53).
 */
@Entity
@Table(name = "tienda")
public class Tienda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "hogar_id", nullable = false)
    private Long hogarId;

    @Column(name = "nombre", nullable = false, length = 60)
    private String nombre;

    /** Solo dígitos, con indicativo: 573001234567. */
    @Column(name = "whatsapp", nullable = false, length = 15)
    private String whatsapp;

    @Column(name = "creada_en", nullable = false, insertable = false, updatable = false)
    private LocalDateTime creadaEn;

    protected Tienda() {
        // JPA
    }

    public Tienda(Long hogarId, String nombre, String whatsapp) {
        this.hogarId = Objects.requireNonNull(hogarId, "hogarId");
        this.nombre = Objects.requireNonNull(nombre, "nombre");
        this.whatsapp = Objects.requireNonNull(whatsapp, "whatsapp");
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

    public String getWhatsapp() {
        return whatsapp;
    }

    public LocalDateTime getCreadaEn() {
        return creadaEn;
    }
}
