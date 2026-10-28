package co.edu.uniquindio.cocinasinestres.admin.dto;

/**
 * Lo que mide la web en un inicio de sesión que terminó bien (KAN-54).
 *
 * @param duracionMs      milisegundos desde «Entrar» hasta que la API reconoció la cuenta
 * @param fallidosPrevios intentos fallidos antes de este, en el mismo navegador
 */
public record MedicionInicioSesionRequest(Integer duracionMs, Integer fallidosPrevios) {
}
