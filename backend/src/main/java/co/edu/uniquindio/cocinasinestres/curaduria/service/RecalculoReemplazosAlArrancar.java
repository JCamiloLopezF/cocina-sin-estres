package co.edu.uniquindio.cocinasinestres.curaduria.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Al arrancar, pone al día los reemplazos automáticos: así un cambio en el catálogo
 * hecho por una migración (por ejemplo, un producto nuevo con su alérgeno) también
 * queda cubierto.
 */
@Component
public class RecalculoReemplazosAlArrancar {

    private static final Logger log = LoggerFactory.getLogger(RecalculoReemplazosAlArrancar.class);

    private final ReemplazosService reemplazosService;

    public RecalculoReemplazosAlArrancar(ReemplazosService reemplazosService) {
        this.reemplazosService = reemplazosService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void recalcular() {
        try {
            reemplazosService.recalcularAutomaticos();
        } catch (RuntimeException e) {
            // Sin reemplazos la app sigue funcionando: las recetas quedan como "plato aparte".
            log.warn("No se pudieron recalcular los reemplazos al arrancar", e);
        }
    }
}
