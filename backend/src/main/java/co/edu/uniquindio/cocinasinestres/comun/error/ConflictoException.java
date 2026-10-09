package co.edu.uniquindio.cocinasinestres.comun.error;

import org.springframework.http.HttpStatus;

/**
 * La operación choca con el estado actual: un duplicado, o una versión
 * desactualizada de un dato que alguien más ya cambió.
 */
public class ConflictoException extends ExcepcionNegocio {

    public ConflictoException(String codigo, String detalle) {
        super(codigo, detalle);
    }

    @Override
    public HttpStatus getEstado() {
        return HttpStatus.CONFLICT;
    }

    @Override
    public String getTitulo() {
        return "Conflicto";
    }
}
