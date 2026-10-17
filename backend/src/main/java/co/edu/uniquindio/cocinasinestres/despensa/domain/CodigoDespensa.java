package co.edu.uniquindio.cocinasinestres.despensa.domain;

/**
 * Códigos de error estables del módulo despensa, para el campo {@code codigo} de la
 * respuesta (CONTRIBUTING.md §7). Un código nunca se renombra: si cambia el
 * significado, se agrega uno nuevo.
 */
public final class CodigoDespensa {

    /** La usuaria todavía no tiene hogar: falta completar el perfil. */
    public static final String HOGAR_REQUERIDO = "DESPENSA_HOGAR_REQUERIDO";

    /** No llegó el producto que se quiere agregar. */
    public static final String PRODUCTO_REQUERIDO = "DESPENSA_PRODUCTO_REQUERIDO";

    /** Ese producto ya está en la despensa del hogar. */
    public static final String PRODUCTO_REPETIDO = "DESPENSA_PRODUCTO_REPETIDO";

    /** Producto contable sin cantidad. */
    public static final String CANTIDAD_REQUERIDA = "DESPENSA_CANTIDAD_REQUERIDA";

    /** Cantidad negativa o con más de tres decimales. */
    public static final String CANTIDAD_INVALIDA = "DESPENSA_CANTIDAD_INVALIDA";

    /** Producto a granel con una cantidad numérica: se registra por nivel. */
    public static final String CANTIDAD_NO_PERMITIDA = "DESPENSA_CANTIDAD_NO_PERMITIDA";

    /** Producto a granel sin nivel. */
    public static final String NIVEL_REQUERIDO = "DESPENSA_NIVEL_REQUERIDO";

    /** Producto contable con un nivel: se registra por cantidad. */
    public static final String NIVEL_NO_PERMITIDO = "DESPENSA_NIVEL_NO_PERMITIDO";

    /** Mínimo numérico negativo o con más de tres decimales. */
    public static final String MINIMO_INVALIDO = "DESPENSA_MINIMO_INVALIDO";

    /** Producto a granel con un mínimo numérico: el mínimo es un nivel. */
    public static final String MINIMO_NO_PERMITIDO = "DESPENSA_MINIMO_NO_PERMITIDO";

    /** Producto contable con un nivel mínimo: el mínimo es una cantidad. */
    public static final String NIVEL_MINIMO_NO_PERMITIDO = "DESPENSA_NIVEL_MINIMO_NO_PERMITIDO";

    /** El nivel mínimo "Lleno" avisaría siempre; no tiene sentido. */
    public static final String NIVEL_MINIMO_INVALIDO = "DESPENSA_NIVEL_MINIMO_INVALIDO";

    /** Ubicación de más de 40 caracteres. */
    public static final String UBICACION_LARGA = "DESPENSA_UBICACION_LARGA";

    /** El filtro del listado no es TODO, POR_VENCER ni BAJO_MINIMO. */
    public static final String FILTRO_INVALIDO = "DESPENSA_FILTRO_INVALIDO";

    private CodigoDespensa() {
    }
}
