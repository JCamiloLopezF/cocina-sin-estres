package co.edu.uniquindio.cocinasinestres.avisos.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Lo que el motor de avisos lee del hogar: despensa, plan y preferencias.
 *
 * <p>Puerto propio del módulo de avisos: no usa las entidades de despensa ni del
 * planificador (RNF-26). La implementación lee las tablas con consultas de solo
 * lectura.</p>
 */
public interface FuenteAvisos {

    /** El hogar de la usuaria, o vacío si todavía no completa el perfil. */
    Optional<Long> hogarDe(Long usuarioId);

    /** Tipos de aviso que la usuaria apagó. Sin fila, el tipo está encendido. */
    Set<TipoAviso> apagados(Long hogarId);

    /**
     * Productos en su mínimo o por debajo (la misma regla de la despensa:
     * {@code cantidad <= cantidad_minima}).
     */
    List<ProductoAviso> bajoMinimo(Long hogarId);

    /** Productos con cantidad que vencen entre {@code desde} y {@code hasta}, inclusive. */
    List<ProductoAviso> porVencer(Long hogarId, LocalDate desde, LocalDate hasta);

    /** Comidas no preparadas de esa fecha cuya receta pide alistar algo desde antes. */
    List<PreparacionPendiente> preparacionesPara(Long hogarId, LocalDate fecha);

    /** Cómo está planeada la semana que empieza en {@code lunes}. */
    EstadoSemana semana(Long hogarId, LocalDate lunes);
}
