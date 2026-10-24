package co.edu.uniquindio.cocinasinestres.planificador.service;

import co.edu.uniquindio.cocinasinestres.comun.error.ConflictoException;
import co.edu.uniquindio.cocinasinestres.perfil.model.Hogar;
import co.edu.uniquindio.cocinasinestres.perfil.repository.HogarRepository;
import co.edu.uniquindio.cocinasinestres.perfil.service.UsuarioActualService;
import co.edu.uniquindio.cocinasinestres.planificador.dto.PlanGeneradoResponse;
import co.edu.uniquindio.cocinasinestres.planificador.model.PlanSemanal;
import co.edu.uniquindio.cocinasinestres.planificador.repository.ComidaPlanRepository;
import co.edu.uniquindio.cocinasinestres.planificador.repository.PlanSemanalRepository;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Borrador automático de una semana (ADR-24, pieza 3): cuando la semana siguiente llega
 * sin plan, el motor de avisos le pide a este servicio que la arme con el generador. La
 * semana queda marcada como borrador hasta que la usuaria la confirme.
 */
@Service
public class BorradorSemanaService {

    private static final Logger log = LoggerFactory.getLogger(BorradorSemanaService.class);

    /**
     * @param planId  plan que quedó armado
     * @param lunes   semana del plan
     * @param comidas cuántas comidas puso el generador
     * @param costo   costo estimado de la semana
     */
    public record BorradorSemana(Long planId, LocalDate lunes, int comidas, BigDecimal costo) {
    }

    private final PlanSemanalRepository planSemanalRepository;
    private final ComidaPlanRepository comidaPlanRepository;
    private final HogarRepository hogarRepository;
    private final UsuarioActualService usuarioActualService;
    private final GeneradorPlanService generadorPlanService;

    public BorradorSemanaService(
        PlanSemanalRepository planSemanalRepository,
        ComidaPlanRepository comidaPlanRepository,
        HogarRepository hogarRepository,
        UsuarioActualService usuarioActualService,
        GeneradorPlanService generadorPlanService) {
        this.planSemanalRepository = planSemanalRepository;
        this.comidaPlanRepository = comidaPlanRepository;
        this.hogarRepository = hogarRepository;
        this.usuarioActualService = usuarioActualService;
        this.generadorPlanService = generadorPlanService;
    }

    /**
     * Arma la semana que empieza en {@code lunes} si todavía no tiene comidas.
     *
     * @return el borrador, o vacío si la semana ya tenía comidas o el generador no
     *         encontró recetas para ponerle
     */
    @Transactional
    public Optional<BorradorSemana> armar(LocalDate lunes) {
        if (lunes == null || lunes.getDayOfWeek() != DayOfWeek.MONDAY) {
            return Optional.empty();
        }
        Hogar hogar = hogarRepository.findByUsuario_Id(usuarioActualService.obtener().id())
            .orElseThrow(() -> new ConflictoException(
                "PLAN_SIN_PERFIL_HOGAR", "Completa el perfil de tu hogar antes de planear la semana."));

        PlanSemanal plan = planSemanalRepository.findByHogar_IdAndSemanaInicio(hogar.getId(), lunes)
            .orElseGet(() -> planSemanalRepository.save(new PlanSemanal(hogar, lunes)));
        if (!comidaPlanRepository.findByPlanSemanal_IdOrderByDiaAscTipoComidaAsc(plan.getId()).isEmpty()) {
            // La usuaria ya empezó esta semana: no se toca.
            return Optional.empty();
        }

        plan.setBorrador(true);
        planSemanalRepository.save(plan);
        PlanGeneradoResponse generado = generadorPlanService.generar(plan.getId(), null);
        if (generado.cambios().isEmpty()) {
            return Optional.empty();
        }
        log.info("Borrador de semana armado: planId={} lunes={} comidas={}", plan.getId(), lunes, generado.cambios().size());
        return Optional.of(new BorradorSemana(plan.getId(), lunes, generado.cambios().size(), generado.costoEstimado()));
    }
}
