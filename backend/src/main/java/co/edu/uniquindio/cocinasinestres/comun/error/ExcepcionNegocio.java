package co.edu.uniquindio.cocinasinestres.comun.error;

import org.springframework.http.HttpStatus;

/**
 * Base de los errores previstos del negocio.
 *
 * <p>La excepción carga su propio código, título y estado HTTP para que
 * {@link ManejadorErrores} la traduzca sin saber de qué módulo viene. Así ningún
 * controlador arma respuestas de error a mano (CONTRIBUTING.md §7).</p>
 */
public abstract class ExcepcionNegocio extends RuntimeException {

    private final String codigo;

    /**
     * @param codigo  código estable en mayúsculas, ver {@link CodigoError}
     * @param detalle mensaje en español, cordial y entendible por la usuaria (RNF-14)
     */
    protected ExcepcionNegocio(String codigo, String detalle) {
        super(detalle);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }

    /** Estado HTTP con el que se responde, según la tabla de CONTRIBUTING.md §7. */
    public abstract HttpStatus getEstado();

    /** Título corto del problema, en español. */
    public abstract String getTitulo();
}
