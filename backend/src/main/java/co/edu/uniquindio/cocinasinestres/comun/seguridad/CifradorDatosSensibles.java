package co.edu.uniquindio.cocinasinestres.comun.seguridad;

import co.edu.uniquindio.cocinasinestres.comun.config.PropiedadesCocina;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

/**
 * Cifra y descifra información sensible mediante AES-256-GCM.
 */
@Component
public class CifradorDatosSensibles {

    private static final String ALGORITMO = "AES/GCM/NoPadding";
    private static final String PREFIJO = "v1:";
    private static final int TAMANO_NONCE = 12;
    private static final int TAMANO_TAG_BITS = 128;
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final byte[] clave;

    public CifradorDatosSensibles(PropiedadesCocina propiedades) {
        String claveConfigurada = propiedades.cifrado().claveV1();

        if (claveConfigurada == null || claveConfigurada.isBlank()) {
            throw new IllegalStateException(
                "Falta configurar la clave de cifrado CIFRADO_CLAVE_V1."
            );
        }

        try {
            this.clave = Base64.getDecoder().decode(claveConfigurada);
        } catch (IllegalArgumentException excepcion) {
            throw new IllegalStateException(
                "CIFRADO_CLAVE_V1 debe estar codificada en Base64.",
                excepcion
            );
        }

        if (this.clave.length != 32) {
            Arrays.fill(this.clave, (byte) 0);
            throw new IllegalStateException(
                "CIFRADO_CLAVE_V1 debe representar exactamente 32 bytes."
            );
        }
    }

    /**
     * Cifra un texto usando un nonce aleatorio nuevo para cada operación.
     */
    public String cifrar(String texto) {
        if (texto == null) {
            return null;
        }

        byte[] nonce = new byte[TAMANO_NONCE];
        ALEATORIO.nextBytes(nonce);

        try {
            Cipher cipher = Cipher.getInstance(ALGORITMO);
            SecretKeySpec llave = new SecretKeySpec(clave, "AES");
            GCMParameterSpec parametros =
                new GCMParameterSpec(TAMANO_TAG_BITS, nonce);

            cipher.init(Cipher.ENCRYPT_MODE, llave, parametros);

            byte[] cifrado = cipher.doFinal(
                texto.getBytes(StandardCharsets.UTF_8)
            );

            byte[] resultado = new byte[nonce.length + cifrado.length];
            System.arraycopy(nonce, 0, resultado, 0, nonce.length);
            System.arraycopy(
                cifrado, 0, resultado, nonce.length, cifrado.length
            );

            return PREFIJO + Base64.getEncoder().encodeToString(resultado);
        } catch (GeneralSecurityException excepcion) {
            throw new IllegalStateException(
                "No fue posible proteger los datos sensibles.",
                excepcion
            );
        }
    }

    /**
     * Descifra un texto previamente cifrado por este componente.
     */
    public String descifrar(String textoCifrado) {
        if (textoCifrado == null) {
            return null;
        }

        if (!textoCifrado.startsWith(PREFIJO)) {
            throw new IllegalArgumentException(
                "El dato no tiene una versión de cifrado reconocida."
            );
        }

        final byte[] datos;
        try {
            datos = Base64.getDecoder().decode(
                textoCifrado.substring(PREFIJO.length())
            );
        } catch (IllegalArgumentException excepcion) {
            throw new IllegalArgumentException(
                "El dato cifrado no tiene un formato válido.",
                excepcion
            );
        }

        if (datos.length <= TAMANO_NONCE) {
            throw new IllegalArgumentException(
                "El dato cifrado está incompleto."
            );
        }

        byte[] nonce = Arrays.copyOfRange(datos, 0, TAMANO_NONCE);
        byte[] cifrado = Arrays.copyOfRange(
            datos, TAMANO_NONCE, datos.length
        );

        try {
            Cipher cipher = Cipher.getInstance(ALGORITMO);
            SecretKeySpec llave = new SecretKeySpec(clave, "AES");
            GCMParameterSpec parametros =
                new GCMParameterSpec(TAMANO_TAG_BITS, nonce);

            cipher.init(Cipher.DECRYPT_MODE, llave, parametros);

            return new String(
                cipher.doFinal(cifrado),
                StandardCharsets.UTF_8
            );
        } catch (GeneralSecurityException excepcion) {
            // Incluye clave incorrecta o alteración del contenido cifrado.
            throw new IllegalArgumentException(
                "No fue posible verificar los datos protegidos.",
                excepcion
            );
        }
    }
}
