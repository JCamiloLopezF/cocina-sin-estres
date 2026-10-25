package co.edu.uniquindio.cocinasinestres.avisos.domain;

/**
 * Los cinco avisos de la Definición (ADR-17), en el orden en que se muestran.
 */
public enum TipoAviso {

    BAJO_MINIMO("Productos que se acaban", "Cuando un producto llega al mínimo que definiste."),
    POR_VENCER("Productos por vencer", "Cuando algo vence en los próximos 3 días."),
    PREPARACION_PREVIA("Alistar desde la víspera", "Cuando una comida de mañana pide preparar algo antes, como remojar."),
    SEMANA_SIN_PLANIFICAR("Próxima semana", "Desde el viernes, si la semana siguiente no tiene plan. Te dejamos un borrador."),
    APROVECHAR_PRODUCTO("Aprovechar lo que tienes", "Una receta que usa lo que está por vencer, sin comprar nada.");

    private final String titulo;
    private final String descripcion;

    TipoAviso(String titulo, String descripcion) {
        this.titulo = titulo;
        this.descripcion = descripcion;
    }

    /** Nombre del tipo en la pantalla de preferencias. */
    public String titulo() {
        return titulo;
    }

    /** Cuándo llega, en una frase. */
    public String descripcion() {
        return descripcion;
    }

    /** El tipo por su nombre, o {@code null} si no existe. */
    public static TipoAviso desde(String nombre) {
        if (nombre == null) {
            return null;
        }
        for (TipoAviso tipo : values()) {
            if (tipo.name().equalsIgnoreCase(nombre.trim())) {
                return tipo;
            }
        }
        return null;
    }
}
