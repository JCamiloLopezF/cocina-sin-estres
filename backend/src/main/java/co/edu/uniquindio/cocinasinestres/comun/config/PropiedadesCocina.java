package co.edu.uniquindio.cocinasinestres.comun.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración propia de la aplicación, bajo el prefijo {@code cocina}.
 *
 * <p>Cada valor sale de una variable de entorno documentada en
 * {@code backend/.env.example}. El repositorio es público: aquí no hay valores
 * por defecto para nada sensible.</p>
 *
 * @param firebase      verificación del token de Firebase (ADR-05)
 * @param cifrado       claves para cifrar datos de salud (ADR-20)
 * @param adminInicial  primera cuenta que recibe el rol ADMIN (ADR-16)
 * @param cors          orígenes del navegador autorizados
 */
@ConfigurationProperties(prefix = "cocina")
public record PropiedadesCocina(
        Firebase firebase,
        Cifrado cifrado,
        AdminInicial adminInicial,
        Cors cors) {

    /**
     * Rellena los bloques que no estén en el yml, para que nadie tenga que
     * preguntar por {@code null} antes de leer una propiedad.
     */
    public PropiedadesCocina {
        firebase = firebase != null ? firebase : new Firebase(null);
        cifrado = cifrado != null ? cifrado : new Cifrado(null);
        adminInicial = adminInicial != null ? adminInicial : new AdminInicial(null);
        cors = cors != null ? cors : new Cors(List.of());
    }

    /**
     * @param credencialesRuta ruta al JSON de la cuenta de servicio, fuera del repositorio.
     *                         Vacío en el perfil {@code dev}, obligatorio en {@code prod}.
     */
    public record Firebase(String credencialesRuta) {
    }

    /**
     * @param claveV1 clave AES-256 en Base64. La versión en el nombre permite rotarla
     *                sin perder los datos cifrados con la anterior.
     */
    public record Cifrado(String claveV1) {
    }

    /**
     * @param correo correo que, al entrar por primera vez, se da de alta como ADMIN
     */
    public record AdminInicial(String correo) {
    }

    /**
     * @param origenes orígenes permitidos por CORS
     */
    public record Cors(List<String> origenes) {

        public Cors {
            origenes = origenes != null ? List.copyOf(origenes) : List.of();
        }
    }
}
