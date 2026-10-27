package co.edu.uniquindio.cocinasinestres.comun.seguridad;

import co.edu.uniquindio.cocinasinestres.comun.error.CodigoError;
import co.edu.uniquindio.cocinasinestres.comun.error.EscritorProblemDetail;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Rechaza los cuerpos más grandes de lo que la API necesita.
 *
 * <p>El JSON más grande que manda la app (el perfil del hogar con sus personas) ocupa
 * unos pocos KB. Sin tope, una sola petición de cientos de MB obliga a Jackson a leerla
 * entera en memoria. Si la petición declara su tamaño ({@code Content-Length}) se
 * responde 413 sin leer nada; si no lo declara, se corta la lectura al pasar el tope.</p>
 *
 * <p>No es un bean: lo crea {@link ConfiguracionSeguridad} y solo vive en la cadena de
 * seguridad, para que Spring Boot no lo registre una segunda vez en el contenedor.</p>
 */
public class FiltroTamanoCuerpo extends OncePerRequestFilter {

    private final long maximoBytes;
    private final EscritorProblemDetail escritorProblemDetail;

    public FiltroTamanoCuerpo(long maximoBytes, EscritorProblemDetail escritorProblemDetail) {
        this.maximoBytes = maximoBytes;
        this.escritorProblemDetail = escritorProblemDetail;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest peticion,
                                    HttpServletResponse respuesta,
                                    FilterChain cadena) throws ServletException, IOException {

        if (maximoBytes <= 0) {
            cadena.doFilter(peticion, respuesta);
            return;
        }

        if (peticion.getContentLengthLong() > maximoBytes) {
            escritorProblemDetail.escribir(peticion, respuesta, HttpStatus.CONTENT_TOO_LARGE,
                    "Petición demasiado grande", CodigoError.CUERPO_DEMASIADO_GRANDE,
                    "Lo que enviaste es demasiado grande. Revisa los datos y vuelve a intentar.");
            return;
        }

        cadena.doFilter(new CuerpoLimitado(peticion, maximoBytes), respuesta);
    }

    /** Envoltorio que corta la lectura del cuerpo cuando pasa el tope. */
    private static final class CuerpoLimitado extends HttpServletRequestWrapper {

        private final long maximo;
        private ServletInputStream flujo;

        CuerpoLimitado(HttpServletRequest peticion, long maximo) {
            super(peticion);
            this.maximo = maximo;
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            if (flujo == null) {
                flujo = new FlujoLimitado(super.getInputStream(), maximo);
            }
            return flujo;
        }
    }

    private static final class FlujoLimitado extends ServletInputStream {

        private final ServletInputStream origen;
        private final long maximo;
        private long leidos;

        FlujoLimitado(ServletInputStream origen, long maximo) {
            this.origen = origen;
            this.maximo = maximo;
        }

        @Override
        public int read() throws IOException {
            int byteLeido = origen.read();
            if (byteLeido != -1) {
                contar(1);
            }
            return byteLeido;
        }

        @Override
        public int read(byte[] destino, int desde, int largo) throws IOException {
            int n = origen.read(destino, desde, largo);
            if (n > 0) {
                contar(n);
            }
            return n;
        }

        private void contar(int n) throws IOException {
            leidos += n;
            if (leidos > maximo) {
                throw new IOException("El cuerpo de la petición pasa de " + maximo + " bytes");
            }
        }

        @Override
        public boolean isFinished() {
            return origen.isFinished();
        }

        @Override
        public boolean isReady() {
            return origen.isReady();
        }

        @Override
        public void setReadListener(ReadListener oyente) {
            origen.setReadListener(oyente);
        }
    }
}
