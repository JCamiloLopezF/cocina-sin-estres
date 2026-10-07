package co.edu.uniquindio.cocinasinestres.comun.error;

import org.springframework.http.HttpStatus;

/**
 * Los datos llegaron bien formados pero una regla del negocio los rechaza.
 *
 * <p>Para los errores de las anotaciones de validación no hace falta lanzarla:
 * {@link ManejadorErrores} ya traduce {@code MethodArgumentNotValidException}.</p>
 */
public class DatosInvalidosException extends ExcepcionNegocio {

    public DatosInvalidosException(String codigo, String detalle) {
        super(codigo, detalle);
    }

    @Override
    public HttpStatus getEstado() {
        return HttpStatus.BAD_REQUEST;
    }

    @Override
    public String getTitulo() {
        return "Datos inválidos";
    }
}
