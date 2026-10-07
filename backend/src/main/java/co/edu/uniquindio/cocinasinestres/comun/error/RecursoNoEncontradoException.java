package co.edu.uniquindio.cocinasinestres.comun.error;

import org.springframework.http.HttpStatus;

/**
 * El recurso no existe, <strong>o pertenece a otro hogar</strong>.
 *
 * <p>Los dos casos responden 404 a propósito: un 403 confirmaría que el recurso
 * existe (RNF-22). Por eso el detalle nunca dice de quién es.</p>
 */
public class RecursoNoEncontradoException extends ExcepcionNegocio {

    public RecursoNoEncontradoException(String detalle) {
        super(CodigoError.RECURSO_NO_ENCONTRADO, detalle);
    }

    public RecursoNoEncontradoException(String codigo, String detalle) {
        super(codigo, detalle);
    }

    @Override
    public HttpStatus getEstado() {
        return HttpStatus.NOT_FOUND;
    }

    @Override
    public String getTitulo() {
        return "No encontrado";
    }
}
