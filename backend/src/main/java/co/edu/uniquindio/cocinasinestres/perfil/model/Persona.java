package co.edu.uniquindio.cocinasinestres.perfil.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Objects;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Representa a una persona integrante de un hogar.
 * Corresponde a la tabla persona de la migración inicial.
 */
@Entity
@Table(name = "persona")
public class Persona {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hogar_id", nullable = false)
    private Hogar hogar;

    @Column(name = "nombre", length = 60)
    private String nombre;

    // Columna TINYINT UNSIGNED: sin esto, la validación de Hibernate espera INTEGER.
    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "edad", nullable = false)
    private Integer edad;

    /**
     * Constructor requerido por JPA.
     */
    protected Persona() {
    }

    /**
     * Crea una persona asociada a un hogar.
     */
    public Persona(Hogar hogar, String nombre, Integer edad) {
        this.hogar = Objects.requireNonNull(
            hogar, "El hogar es obligatorio");
        this.nombre = nombre;
        this.edad = Objects.requireNonNull(
            edad, "La edad es obligatoria");
    }

    public Long getId() {
        return id;
    }

    public Hogar getHogar() {
        return hogar;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Integer getEdad() {
        return edad;
    }

    public void setEdad(Integer edad) {
        this.edad = Objects.requireNonNull(
            edad, "La edad es obligatoria");
    }
}
