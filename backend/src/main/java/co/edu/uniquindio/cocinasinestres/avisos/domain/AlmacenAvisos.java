package co.edu.uniquindio.cocinasinestres.avisos.domain;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * Dónde quedan los avisos (tabla {@code aviso}) y las preferencias por tipo
 * ({@code hogar_preferencia_aviso}).
 */
public interface AlmacenAvisos {

    /**
     * Un aviso tal como está guardado.
     *
     * @param leido la usuaria lo marcó como leído (o tocó su botón)
     */
    record AvisoGuardado(
        Long id,
        TipoAviso tipo,
        String titulo,
        String mensaje,
        String enlace,
        String accion,
        boolean leido,
        LocalDateTime creadoEn) {
    }

    /**
     * Guarda el aviso si su clave no existe. Si ya existe y no se ha leído, actualiza el
     * texto (la situación cambió en el día); si ya se leyó, no lo toca. Así el mismo
     * aviso no aparece dos veces (ADR-17).
     */
    void guardar(Long hogarId, Aviso aviso);

    /** Los avisos sin leer de esas claves, en el orden de los tipos. */
    List<AvisoGuardado> sinLeer(Long hogarId, Collection<String> claves);

    /** Los avisos del hogar desde esa fecha, del más nuevo al más viejo (centro de avisos). */
    List<AvisoGuardado> desde(Long hogarId, LocalDateTime desde, int maximo);

    /** @return {@code false} si el aviso no existe o es de otro hogar */
    boolean marcarLeido(Long hogarId, Long avisoId);

    /** Marca como leídos todos los avisos del hogar. @return cuántos cambiaron */
    int marcarTodosLeidos(Long hogarId);

    /** Enciende o apaga un tipo de aviso para el hogar. */
    void cambiarPreferencia(Long hogarId, TipoAviso tipo, boolean activo);
}
