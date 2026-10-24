package co.edu.uniquindio.cocinasinestres.planificador.service;

import co.edu.uniquindio.cocinasinestres.comun.error.ConflictoException;
import co.edu.uniquindio.cocinasinestres.comun.error.DatosInvalidosException;
import co.edu.uniquindio.cocinasinestres.comun.error.RecursoNoEncontradoException;
import co.edu.uniquindio.cocinasinestres.planificador.domain.AdaptacionReceta;
import co.edu.uniquindio.cocinasinestres.planificador.dto.AdaptacionResponse;
import co.edu.uniquindio.cocinasinestres.perfil.model.Hogar;
import co.edu.uniquindio.cocinasinestres.perfil.repository.HogarRepository;
import co.edu.uniquindio.cocinasinestres.perfil.service.UsuarioActualService;
import co.edu.uniquindio.cocinasinestres.planificador.domain.CatalogoParaPlan;
import co.edu.uniquindio.cocinasinestres.planificador.domain.ContextoPlan;
import co.edu.uniquindio.cocinasinestres.planificador.domain.GeneradorPlan;
import co.edu.uniquindio.cocinasinestres.planificador.domain.Hueco;
import co.edu.uniquindio.cocinasinestres.planificador.dto.CambioPlanResponse;
import co.edu.uniquindio.cocinasinestres.planificador.dto.ComidaPlanResponse;
import co.edu.uniquindio.cocinasinestres.planificador.dto.GenerarPlanRequest;
import co.edu.uniquindio.cocinasinestres.planificador.dto.OtraOpcionRequest;
import co.edu.uniquindio.cocinasinestres.planificador.dto.PlanGeneradoResponse;
import co.edu.uniquindio.cocinasinestres.planificador.dto.PlanSemanalResponse;
import co.edu.uniquindio.cocinasinestres.planificador.model.ComidaPlan;
import co.edu.uniquindio.cocinasinestres.planificador.model.PlanSemanal;
import co.edu.uniquindio.cocinasinestres.planificador.service.ContextoPlanFactory.Contexto;
import co.edu.uniquindio.cocinasinestres.planificador.repository.ComidaPlanRepository;
import co.edu.uniquindio.cocinasinestres.planificador.repository.PlanSemanalRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * "Armar mi semana", "Otra opción" y "Ajustar al presupuesto" (ADR-24).
 *
 * <p>Junta lo que el generador necesita (catálogo, despensa, perfil y plan actual),
 * le pide la propuesta a {@link GeneradorPlan} y la guarda. Al final regenera la lista
 * de compras una sola vez, no por cada comida.</p>
 */
@Service
public class GeneradorPlanService {

    private static final Logger log = LoggerFactory.getLogger(GeneradorPlanService.class);
    private static final String NO_ENCONTRADO = "RECURSO_NO_ENCONTRADO";

    private final PlanSemanalRepository planSemanalRepository;
    private final ComidaPlanRepository comidaPlanRepository;
    private final HogarRepository hogarRepository;
    private final UsuarioActualService usuarioActualService;
    private final ContextoPlanFactory contextos;
    private final CatalogoParaPlan catalogo;
    private final ListaComprasService listaComprasService;
    private final GeneradorPlan generador = new GeneradorPlan();

    public GeneradorPlanService(
        PlanSemanalRepository planSemanalRepository,
        ComidaPlanRepository comidaPlanRepository,
        HogarRepository hogarRepository,
        UsuarioActualService usuarioActualService,
        ContextoPlanFactory contextos,
        CatalogoParaPlan catalogo,
        ListaComprasService listaComprasService) {
        this.planSemanalRepository = planSemanalRepository;
        this.comidaPlanRepository = comidaPlanRepository;
        this.hogarRepository = hogarRepository;
        this.usuarioActualService = usuarioActualService;
        this.contextos = contextos;
        this.catalogo = catalogo;
        this.listaComprasService = listaComprasService;
    }

    /** Llena los huecos vacíos del almuerzo y la cena (o las comidas pedidas). */
    @Transactional
    public PlanGeneradoResponse generar(Long planId, GenerarPlanRequest request) {
        PlanSemanal plan = planDeLaUsuaria(planId);
        List<String> comidas = comidasPedidas(request);
        List<ComidaPlan> actuales = comidasDe(plan);
        Contexto contexto = contexto(plan, actuales);

        List<Hueco> huecos = new ArrayList<>();
        for (int dia = 1; dia <= 7; dia++) {
            for (String comida : comidas) {
                huecos.add(new Hueco(dia, comida));
            }
        }
        Map<Hueco, Long> nuevas = generador.completar(contexto.plan(), huecos);

        List<CambioPlanResponse> cambios = new ArrayList<>();
        nuevas.forEach((hueco, receta) -> {
            comidaPlanRepository.save(new ComidaPlan(plan, hueco.dia(), hueco.tipoComida(), receta, null));
            cambios.add(new CambioPlanResponse(hueco.dia(), hueco.tipoComida(), receta, null));
        });
        log.info("Plan generado: planId={} comidasNuevas={}", planId, nuevas.size());
        return terminar(plan, contexto, cambios);
    }

    /** Cambia la receta de una comida por la siguiente mejor opción. */
    @Transactional
    public ComidaPlanResponse otraOpcion(Long planId, Long comidaId, OtraOpcionRequest request) {
        PlanSemanal plan = planDeLaUsuaria(planId);
        List<ComidaPlan> actuales = comidasDe(plan);
        ComidaPlan comida = actuales.stream()
            .filter(c -> c.getId().equals(comidaId))
            .findFirst()
            .orElseThrow(() -> new RecursoNoEncontradoException(NO_ENCONTRADO, "Comida no encontrada."));
        if (comida.isPreparada()) {
            throw new ConflictoException("PLAN_COMIDA_PREPARADA", "Esta comida ya está preparada; no se puede cambiar.");
        }

        Set<Long> excluir = new HashSet<>();
        if (request != null && request.excluir() != null) {
            request.excluir().stream().filter(Objects::nonNull).forEach(excluir::add);
        }
        Contexto contexto = contexto(plan, actuales);
        Long nueva = generador
            .otraOpcion(contexto.plan(), new Hueco(comida.getDia(), comida.getTipoComida()), excluir)
            .orElseThrow(() -> new ConflictoException(
                "PLAN_SIN_ALTERNATIVAS", "Ya viste todas las recetas que encajan en esta comida."));

        comida.setRecetaId(nueva);
        comida.setIdCambioCliente(null);
        ComidaPlan guardada = comidaPlanRepository.save(comida);
        listaComprasService.generarListaCompras(plan.getId());
        return aRespuesta(guardada);
    }

    /** Cambia las comidas no preparadas más caras por otras más baratas hasta caber en el presupuesto. */
    @Transactional
    public PlanGeneradoResponse ajustarAlPresupuesto(Long planId) {
        PlanSemanal plan = planDeLaUsuaria(planId);
        List<ComidaPlan> actuales = comidasDe(plan);
        Contexto contexto = contexto(plan, actuales);

        Map<Hueco, ComidaPlan> modificables = new HashMap<>();
        actuales.stream()
            .filter(c -> !c.isPreparada())
            .forEach(c -> modificables.put(new Hueco(c.getDia(), c.getTipoComida()), c));

        Map<Hueco, Long> cambiosPropuestos = generador.ajustarAlPresupuesto(contexto.plan(), modificables.keySet());

        List<CambioPlanResponse> cambios = new ArrayList<>();
        cambiosPropuestos.entrySet().stream()
            .sorted(Map.Entry.comparingByKey(Hueco.ORDEN))
            .forEach(cambio -> {
                ComidaPlan comida = modificables.get(cambio.getKey());
                Long anterior = comida.getRecetaId();
                comida.setRecetaId(cambio.getValue());
                comida.setIdCambioCliente(null);
                comidaPlanRepository.save(comida);
                cambios.add(new CambioPlanResponse(comida.getDia(), comida.getTipoComida(), cambio.getValue(), anterior));
            });
        log.info("Plan ajustado al presupuesto: planId={} cambios={}", planId, cambios.size());
        return terminar(plan, contexto, cambios);
    }

    /** Cómo adaptar cada receta a las alergias de las personas del hogar de la usuaria. */
    @Transactional(readOnly = true)
    public List<AdaptacionResponse> adaptaciones(List<Long> recetaIds) {
        Hogar hogar = hogarDeLaUsuaria();
        ContextoPlan contexto = contexto(hogar, List.of()).plan();
        Map<Long, String> productos = catalogo.nombresProducto();
        Map<Long, String> recetas = catalogo.nombresReceta();
        return recetaIds.stream()
            .filter(id -> contexto.recetas().containsKey(id))
            .map(id -> aRespuesta(id, generador.adaptar(contexto, id), productos, recetas))
            .toList();
    }

    // ------------------------------------------------------------------

    private static AdaptacionResponse aRespuesta(
        Long recetaId, AdaptacionReceta adaptacion, Map<Long, String> nombres, Map<Long, String> recetas) {
        return new AdaptacionResponse(
            recetaId,
            adaptacion.estado().name(),
            adaptacion.cambios().stream()
                .map(c -> new AdaptacionResponse.Cambio(
                    c.tipo().name(),
                    c.persona(),
                    c.alergias().stream().sorted().toList(),
                    c.productoId(),
                    c.productoId() == null ? null : nombres.get(c.productoId()),
                    c.reemplazoId(),
                    c.reemplazoId() == null ? null : nombres.get(c.reemplazoId()),
                    c.enDespensa(),
                    c.recetaAparteId(),
                    c.recetaAparteId() == null ? null : recetas.get(c.recetaAparteId())))
                .toList());
    }

    private Contexto contexto(PlanSemanal plan, List<ComidaPlan> actuales) {
        return contextos.construir(plan.getHogar(), actuales);
    }

    private Contexto contexto(Hogar hogar, List<ComidaPlan> actuales) {
        return contextos.construir(hogar, actuales);
    }

    private PlanGeneradoResponse terminar(PlanSemanal plan, Contexto contexto, List<CambioPlanResponse> cambios) {
        if (!cambios.isEmpty()) {
            listaComprasService.generarListaCompras(plan.getId());
        }
        List<ComidaPlan> finales = comidasDe(plan);
        Map<Hueco, Long> comoQuedo = new HashMap<>();
        finales.forEach(c -> comoQuedo.put(new Hueco(c.getDia(), c.getTipoComida()), c.getRecetaId()));

        BigDecimal costo = generador.costo(contexto.plan(), comoQuedo).setScale(2, RoundingMode.HALF_UP);
        BigDecimal presupuesto = contexto.plan().presupuestoSemanal();
        Set<String> aprovecha = new LinkedHashSet<>();
        generador.aprovechados(contexto.plan(), comoQuedo.values())
            .forEach(id -> aprovecha.add(contexto.porVencer().get(id)));
        aprovecha.remove(null);

        PlanSemanalResponse respuesta = new PlanSemanalResponse(
            plan.getId(), plan.getSemanaInicio(), plan.getCreadoEn(),
            finales.stream().map(this::aRespuesta).toList());
        return new PlanGeneradoResponse(
            respuesta,
            cambios,
            costo,
            presupuesto,
            presupuesto.signum() > 0 && costo.compareTo(presupuesto) > 0,
            List.copyOf(aprovecha),
            contexto.plan().hayNinos() ? (int) generador.paraNinos(contexto.plan(), comoQuedo.values()) : 0,
            (int) comoQuedo.values().stream()
                .map(id -> generador.adaptar(contexto.plan(), id).estado())
                .filter(e -> e == AdaptacionReceta.Estado.ADAPTADA || e == AdaptacionReceta.Estado.PLATO_APARTE)
                .count());
    }

    private Hogar hogarDeLaUsuaria() {
        Long usuarioId = usuarioActualService.obtener().id();
        return hogarRepository.findByUsuario_Id(usuarioId)
            .orElseThrow(() -> new ConflictoException(
                "PLAN_SIN_PERFIL_HOGAR", "Completa el perfil de tu hogar antes de planear la semana."));
    }

    private PlanSemanal planDeLaUsuaria(Long planId) {
        Hogar hogar = hogarDeLaUsuaria();
        PlanSemanal plan = planSemanalRepository.findById(planId)
            .orElseThrow(() -> new RecursoNoEncontradoException(NO_ENCONTRADO, "Plan semanal no encontrado."));
        if (!plan.getHogar().getId().equals(hogar.getId())) {
            // Plan de otro hogar: mismo 404 que "no existe" (RNF-22)
            throw new RecursoNoEncontradoException(NO_ENCONTRADO, "Plan semanal no encontrado.");
        }
        return plan;
    }

    private List<ComidaPlan> comidasDe(PlanSemanal plan) {
        return comidaPlanRepository.findByPlanSemanal_IdOrderByDiaAscTipoComidaAsc(plan.getId());
    }

    private static List<String> comidasPedidas(GenerarPlanRequest request) {
        List<String> comidas = request == null || request.comidas() == null
            ? List.of()
            : request.comidas().stream().filter(Objects::nonNull).distinct().toList();
        if (comidas.isEmpty()) {
            return GeneradorPlan.COMIDAS_A_LLENAR;
        }
        if (!Hueco.COMIDAS.containsAll(comidas)) {
            throw new DatosInvalidosException("PLAN_DATOS_INVALIDOS", "Tipo de comida inválido.");
        }
        return comidas;
    }

    private ComidaPlanResponse aRespuesta(ComidaPlan c) {
        return new ComidaPlanResponse(
            c.getId(),
            c.getPlanSemanal().getId(),
            c.getDia(),
            c.getTipoComida(),
            c.getRecetaId(),
            c.isPreparada(),
            c.getPreparadaEn(),
            c.getIdCambioCliente());
    }
}
