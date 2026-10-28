package co.edu.uniquindio.cocinasinestres.recetario.model;

import co.edu.uniquindio.cocinasinestres.recetario.domain.PasosReceta;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Receta del catálogo de la aplicación.
 *
 * <p>Mapea la tabla {@code receta} creada por {@code V1__esquema_inicial.sql}. Sus
 * ingredientes son productos del catálogo global y se guardan en
 * {@code ingrediente_receta}; la receta solo conserva el identificador del producto,
 * no la entidad, porque {@code Producto} pertenece al módulo {@code curaduria}.</p>
 *
 * <p>Las recetas no se borran: se desactivan ({@code activa = false}), porque los
 * planes semanales siguen apuntando a ellas.</p>
 */
@Entity
@Table(name = "receta")
public class Receta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre", nullable = false, length = 120)
    private String nombre;

    @Column(name = "descripcion", columnDefinition = "text")
    private String descripcion;

    /** Tiempo total en minutos (SMALLINT UNSIGNED). */
    @Column(name = "tiempo_minutos", nullable = false)
    private short tiempoMinutos;

    /** Porciones para las que están calculadas las cantidades (TINYINT UNSIGNED). */
    @Column(name = "porciones", nullable = false)
    private byte porciones;

    /** Un paso por línea, sin numerar. Ver {@link PasosReceta}. */
    @Column(name = "pasos", nullable = false, columnDefinition = "text")
    private String pasos;

    /** Indicación para el aviso PREPARACION_PREVIA, p. ej. remojar los frijoles. */
    @Column(name = "preparacion_previa", columnDefinition = "text")
    private String preparacionPrevia;

    @Column(name = "activa", nullable = false)
    private boolean activa;

    // Video de la receta (V10): id de YouTube y el fragmento que se muestra. Lo edita el
    // curador por SQL desde su módulo; aquí solo se lee.
    @Column(name = "video_youtube_id", length = 11)
    private String videoYoutubeId;

    @Column(name = "video_inicio_seg")
    private Integer videoInicioSeg;

    @Column(name = "video_fin_seg")
    private Integer videoFinSeg;

    // Para qué comida sirve (V7 y V17). Los edita el curador desde su módulo; aquí solo se leen.
    @Column(name = "es_plato_fuerte", nullable = false)
    private boolean platoFuerte = true;

    @Column(name = "gusta_a_ninos", nullable = false)
    private boolean gustaANinos;

    @Column(name = "para_desayuno", nullable = false)
    private boolean paraDesayuno;

    @Column(name = "para_almuerzo", nullable = false)
    private boolean paraAlmuerzo;

    @Column(name = "para_cena", nullable = false)
    private boolean paraCena;

    @Column(name = "modificado_por", nullable = false)
    private Long modificadoPor;

    @Generated(event = {EventType.INSERT, EventType.UPDATE})
    @Column(name = "modificado_en", nullable = false)
    private Instant modificadoEn;

    @OneToMany(mappedBy = "receta", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<IngredienteReceta> ingredientes = new ArrayList<>();

    protected Receta() {
    }

    public Receta(
        String nombre,
        String descripcion,
        int tiempoMinutos,
        int porciones,
        String pasos,
        String preparacionPrevia,
        boolean activa,
        Long modificadoPor) {

        this.nombre = Objects.requireNonNull(nombre, "nombre");
        this.descripcion = descripcion;
        this.tiempoMinutos = positivoComoShort(tiempoMinutos, "tiempoMinutos");
        this.porciones = positivoComoByte(porciones, "porciones");
        this.pasos = Objects.requireNonNull(pasos, "pasos");
        this.preparacionPrevia = preparacionPrevia;
        this.activa = activa;
        this.modificadoPor = Objects.requireNonNull(modificadoPor, "modificadoPor");
    }

    /**
     * Agrega un ingrediente a la receta.
     *
     * @param productoId identificador de un producto del catálogo global
     * @param cantidad   cantidad en la unidad base del producto, para las porciones de la receta
     * @param opcional   si la receta se puede preparar sin este ingrediente
     */
    public IngredienteReceta agregarIngrediente(Long productoId, BigDecimal cantidad, boolean opcional) {
        IngredienteReceta ingrediente = new IngredienteReceta(this, productoId, cantidad, opcional);
        ingredientes.add(ingrediente);

        return ingrediente;
    }

    /** Los pasos como lista ordenada, ya separados y sin numerar. */
    public List<String> pasosEnLista() {
        return PasosReceta.dividir(pasos);
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public int getTiempoMinutos() {
        return tiempoMinutos;
    }

    public int getPorciones() {
        return porciones;
    }

    public String getPasos() {
        return pasos;
    }

    public String getPreparacionPrevia() {
        return preparacionPrevia;
    }

    /** Id del video de YouTube; {@code null} si la receta no tiene video. */
    public String getVideoYoutubeId() {
        return videoYoutubeId;
    }

    public Integer getVideoInicioSeg() {
        return videoInicioSeg;
    }

    public Integer getVideoFinSeg() {
        return videoFinSeg;
    }

    public boolean isActiva() {
        return activa;
    }

    /** Es una comida y no un acompañamiento (un jugo, una ensalada). */
    public boolean isPlatoFuerte() {
        return platoFuerte;
    }

    public boolean isGustaANinos() {
        return gustaANinos;
    }

    /** Comidas en que va la receta, en el orden del día: DESAYUNO, ALMUERZO, CENA. */
    public List<String> momentos() {
        List<String> momentos = new ArrayList<>();
        if (paraDesayuno) {
            momentos.add("DESAYUNO");
        }
        if (paraAlmuerzo) {
            momentos.add("ALMUERZO");
        }
        if (paraCena) {
            momentos.add("CENA");
        }
        return momentos;
    }

    public Long getModificadoPor() {
        return modificadoPor;
    }

    public Instant getModificadoEn() {
        return modificadoEn;
    }

    public List<IngredienteReceta> getIngredientes() {
        return Collections.unmodifiableList(ingredientes);
    }

    private static short positivoComoShort(int valor, String campo) {
        if (valor <= 0 || valor > Short.MAX_VALUE) {
            throw new IllegalArgumentException(campo + " debe estar entre 1 y " + Short.MAX_VALUE);
        }

        return (short) valor;
    }

    private static byte positivoComoByte(int valor, String campo) {
        if (valor <= 0 || valor > Byte.MAX_VALUE) {
            throw new IllegalArgumentException(campo + " debe estar entre 1 y " + Byte.MAX_VALUE);
        }

        return (byte) valor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof Receta receta)) {
            return false;
        }

        return id != null && Objects.equals(id, receta.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Receta{" +
            "id=" + id +
            ", nombre='" + nombre + '\'' +
            ", tiempoMinutos=" + tiempoMinutos +
            ", porciones=" + porciones +
            ", activa=" + activa +
            '}';
    }
}
