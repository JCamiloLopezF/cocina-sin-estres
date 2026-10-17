package co.edu.uniquindio.cocinasinestres.comun.error;

import org.springframework.http.HttpStatus;

/**
 * No hay usuaria autenticada en la petición, o su cuenta ya no sirve para operar.
 *
 * <p>Normalmente el 401 lo responde la cadena de seguridad antes de llegar a un
 * controlador. Esta excepción cubre el caso en que un servicio pide la usuaria
 * actual y no la encuentra.</p>
 */
public class NoAutenticadoException extends ExcepcionNegocio {

    public NoAutenticadoException(String detalle) {
        super(CodigoError.AUTH_TOKEN_INVALIDO, detalle);
    }

    public NoAutenticadoException(String codigo, String detalle) {
        super(codigo, detalle);
    }

    @Override
    public HttpStatus getEstado() {
        return HttpStatus.UNAUTHORIZED;
    }

    @Override
    public String getTitulo() {
        return "Sesión no válida";
    }
}
