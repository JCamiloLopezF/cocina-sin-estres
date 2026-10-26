package co.edu.uniquindio.cocinasinestres.planificador.service;

import co.edu.uniquindio.cocinasinestres.comun.error.ConflictoException;
import co.edu.uniquindio.cocinasinestres.perfil.model.Hogar;
import co.edu.uniquindio.cocinasinestres.perfil.repository.HogarRepository;
import co.edu.uniquindio.cocinasinestres.perfil.service.UsuarioActualService;
import co.edu.uniquindio.cocinasinestres.planificador.domain.CatalogoParaPlan;
import co.edu.uniquindio.cocinasinestres.planificador.domain.RegistroUso;
import co.edu.uniquindio.cocinasinestres.planificador.domain.Rescate;
import co.edu.uniquindio.cocinasinestres.planificador.domain.StockEnDespensa;
import co.edu.uniquindio.cocinasinestres.planificador.dto.RescateResponse;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Modo rescate (KAN-25): junta la despensa, el catálogo y las alergias del hogar y le
 * pide a {@link Rescate} qué se puede cocinar hoy sin comprar nada. También lo usa el
 * motor de avisos para el aviso "aprovechar un producto" (ADR-17).
 */
@Service
public class RescateService {

    private final HogarRepository hogarRepository;
    private final UsuarioActualService usuarioActualService;
    private final ContextoPlanFactory contextos;
    private final StockEnDespensa stockEnDespensa;
    private final CatalogoParaPlan catalogo;
    private final Clock reloj;
    private final RegistroUso registroUso;
    private final Rescate rescate = new Rescate();

    public RescateService(
        HogarRepository hogarRepository,
        UsuarioActualService usuarioActualService,
        ContextoPlanFactory contextos,
        StockEnDespensa stockEnDespensa,
        CatalogoParaPlan catalogo,
        Clock reloj,
        RegistroUso registroUso) {
        this.hogarRepository = hogarRepository;
        this.usuarioActualService = usuarioActualService;
        this.contextos = contextos;
        this.stockEnDespensa = stockEnDespensa;
        this.catalogo = catalogo;
        this.reloj = reloj;
        this.registroUso = registroUso;
    }

    /**
     * @param maxMinutos tiempo máximo de la receta ("tengo 20 minutos"), o {@code null}
     */
    // No es de solo lectura: anota el uso del modo rescate (KAN-54).
    @Transactional
    public RescateResponse opciones(Integer maxMinutos) {
        Hogar hogar = hogarRepository.findByUsuario_Id(usuarioActualService.obtener().id())
            .orElseThrow(() -> new ConflictoException(
                "PLAN_SIN_PERFIL_HOGAR", "Completa el perfil de tu hogar antes de planear la semana."));
        LocalDate hoy = LocalDate.now(reloj);
        Map<Long, LocalDate> vencimientos = stockEnDespensa.vencimientos(
            hogar.getId(), hoy, hoy.plusDays(Rescate.DIAS_POR_VENCER));
        List<Rescate.Opcion> opciones = rescate.opciones(
            contextos.construir(hogar, List.of()).plan(), vencimientos, maxMinutos);
        registroUso.rescate(hogar.getId(), opciones.size());

        Map<Long, String> recetas = catalogo.nombresReceta();
        Map<Long, String> productos = catalogo.nombresProducto();
        return new RescateResponse(opciones.stream()
            .map(o -> new RescateResponse.Opcion(
                o.recetaId(),
                recetas.getOrDefault(o.recetaId(), "Receta"),
                o.tiempoMinutos(),
                o.adaptacion().estado().name(),
                o.aprovecha().stream()
                    .map(p -> new RescateResponse.PorVencer(
                        p,
                        productos.getOrDefault(p, "Producto"),
                        vencimientos.get(p),
                        (int) ChronoUnit.DAYS.between(hoy, vencimientos.get(p))))
                    .toList()))
            .toList());
    }
}
