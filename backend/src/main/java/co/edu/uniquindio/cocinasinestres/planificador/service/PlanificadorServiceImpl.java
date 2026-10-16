package co.edu.uniquindio.cocinasinestres.planificador.service;

import co.edu.uniquindio.cocinasinestres.comun.error.DatosInvalidosException;
import co.edu.uniquindio.cocinasinestres.comun.error.NoAutenticadoException;
import co.edu.uniquindio.cocinasinestres.perfil.domain.UsuarioAutenticado;
import co.edu.uniquindio.cocinasinestres.perfil.model.Hogar;
import co.edu.uniquindio.cocinasinestres.perfil.repository.HogarRepository;
import co.edu.uniquindio.cocinasinestres.perfil.service.UsuarioActualService;
import co.edu.uniquindio.cocinasinestres.planificador.dto.ComidaPlanAssignRequest;
import co.edu.uniquindio.cocinasinestres.planificador.dto.ComidaPlanResponse;
import co.edu.uniquindio.cocinasinestres.planificador.dto.PlanSemanalResponse;
import co.edu.uniquindio.cocinasinestres.planificador.model.ComidaPlan;
import co.edu.uniquindio.cocinasinestres.planificador.model.PlanSemanal;
import co.edu.uniquindio.cocinasinestres.planificador.repository.ComidaPlanRepository;
import co.edu.uniquindio.cocinasinestres.planificador.repository.PlanSemanalRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación del servicio de planificador con reglas definidas en V1.
 */
@Service
public class PlanificadorServiceImpl implements co.edu.uniquindio.cocinasinestres.planificador.service.PlanificadorService {

    private static final Logger log = LoggerFactory.getLogger(PlanificadorServiceImpl.class);

    private static final String ERROR_PLAN_DATOS_INVALIDOS = "PLAN_DATOS_INVALIDOS";
    private static final String ERROR_RECURSO_NO_ENCONTRADO = "RECURSO_NO_ENCONTRADO";

    private final PlanSemanalRepository planSemanalRepository;
    private final ComidaPlanRepository comidaPlanRepository;
    private final UsuarioActualService usuarioActualService;
    private final HogarRepository hogarRepository;
    private final JdbcTemplate jdbcTemplate;
    private final ListaComprasService listaComprasService;

    public PlanificadorServiceImpl(
        PlanSemanalRepository planSemanalRepository,
        ComidaPlanRepository comidaPlanRepository,
        UsuarioActualService usuarioActualService,
        HogarRepository hogarRepository,
        JdbcTemplate jdbcTemplate,
        ListaComprasService listaComprasService) {
        this.planSemanalRepository = planSemanalRepository;
        this.comidaPlanRepository = comidaPlanRepository;
        this.usuarioActualService = usuarioActualService;
        this.hogarRepository = hogarRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.listaComprasService = listaComprasService;
    }

    @Override
    @Transactional
    public PlanSemanalResponse crearPlanSiNoExiste(LocalDate semanaInicio) {
        UsuarioAutenticado usuario = usuarioActualService.obtener();
        Hogar hogar = hogarRepository.findByUsuario_Id(usuario.id())
            .orElseThrow(() -> new NoAutenticadoException("No existe hogar para la usuaria."));

        PlanSemanal plan = planSemanalRepository.findByHogar_IdAndSemanaInicio(hogar.getId(), semanaInicio)
            .orElseGet(() -> planSemanalRepository.save(new PlanSemanal(hogar, semanaInicio)));

        return construirRespuesta(plan);
    }

    @Override
    @Transactional(readOnly = true)
    public PlanSemanalResponse obtenerPorSemana(LocalDate semanaInicio) {
        UsuarioAutenticado usuario = usuarioActualService.obtener();
        Hogar hogar = hogarRepository.findByUsuario_Id(usuario.id())
            .orElseThrow(() -> new NoAutenticadoException("No existe hogar para la usuaria."));

        PlanSemanal plan = planSemanalRepository.findByHogar_IdAndSemanaInicio(hogar.getId(), semanaInicio)
            .orElseThrow(() -> new DatosInvalidosException(ERROR_RECURSO_NO_ENCONTRADO, "No existe un plan para esa semana."));

        return construirRespuesta(plan);
    }

    @Override
    @Transactional
    public ComidaPlanResponse asignarComida(Long planId, ComidaPlanAssignRequest request) {
        validarRequestAsignacion(request);

        UsuarioAutenticado usuario = usuarioActualService.obtener();
        // verificar que el plan pertenece al hogar de la usuaria
        PlanSemanal plan = planSemanalRepository.findById(planId)
            .orElseThrow(() -> new DatosInvalidosException(ERROR_RECURSO_NO_ENCONTRADO, "Plan semanal no encontrado."));

        Hogar hogar = hogarRepository.findByUsuario_Id(usuario.id())
            .orElseThrow(() -> new NoAutenticadoException("No existe hogar para la usuaria."));

        if (!plan.getHogar().getId().equals(hogar.getId())) {
            throw new DatosInvalidosException("PLAN_NO_PERMITIDO", "No puedes operar sobre el plan de otro hogar.");
        }

        // validar receta existe y está activa (consulta directa a la tabla receta)
        if (!recetaActiva(request.recetaId())) {
            throw new DatosInvalidosException("RECETA_NO_DISPONIBLE", "La receta no existe o no está activa.");
        }

        // idempotencia por idCambioCliente
        String idCambio = request.idCambioCliente();
        if (idCambio != null && !idCambio.isBlank()) {
            Optional<ComidaPlan> porCambio = comidaPlanRepository.findByIdCambioCliente(idCambio);
            if (porCambio.isPresent()) {
                ComidaPlan existente = porCambio.get();
                // si pertenece a otro plan: devolver error
                if (!existente.getPlanSemanal().getId().equals(planId)) {
                    throw new DatosInvalidosException("CONFLICTO_ASIGNACION", "El idCambioCliente ya existe en otro plan.");
                }
                // actualizar si cambió algo
                existente.setRecetaId(request.recetaId());
                existente.setPreparada(false);
                existente.setPreparadaEn(null);
                existente.setDia(request.dia());
                existente.setTipoComida(request.tipoComida());
                ComidaPlan guardada = comidaPlanRepository.save(existente);
                // Generar lista de compras sincronamente
                listaComprasService.generarListaCompras(plan.getId());
                return aComidaPlanResponse(guardada);
            }
        }

        // upsert por plan+dia+tipo
        try {
            ComidaPlan comida = comidaPlanRepository
                .findByPlanSemanal_IdAndDiaAndTipoComida(planId, request.dia(), request.tipoComida())
                .map(existing -> {
                    existing.setRecetaId(request.recetaId());
                    existing.setPreparada(false);
                    existing.setPreparadaEn(null);
                    if (idCambio != null && !idCambio.isBlank()) {
                        existing.setIdCambioCliente(idCambio);
                    }
                    return comidaPlanRepository.save(existing);
                })
                .orElseGet(() -> {
                    ComidaPlan nueva = new ComidaPlan(plan, request.dia(), request.tipoComida(), request.recetaId(), idCambio);
                    return comidaPlanRepository.save(nueva);
                });

            // Generación síncrona de lista de compras (se ejecuta antes de responder)
            listaComprasService.generarListaCompras(plan.getId());

            log.info("Comida asignada: planId={} dia={} tipo={} receta={}", planId, request.dia(), request.tipoComida(), request.recetaId());

            return aComidaPlanResponse(comida);
        } catch (DataIntegrityViolationException dive) {
            // Posible choque por id_cambio_cliente u otra constraint: intentar recuperar por idCambioCliente si fue el caso.
            if (idCambio != null && !idCambio.isBlank()) {
                return comidaPlanRepository.findByIdCambioCliente(idCambio)
                    .map(this::aComidaPlanResponse)
                    .orElseThrow(() -> new DatosInvalidosException("CONFLICTO_ASIGNACION",
                        "No fue posible completar la asignación por conflicto."));
            }
            throw new DatosInvalidosException("CONFLICTO_ASIGNACION", "No fue posible completar la asignación por conflicto.");
        }
    }

    @Override
    @Transactional
    public void eliminarComida(Long planId, Long comidaId) {
        UsuarioAutenticado usuario = usuarioActualService.obtener();
        Hogar hogar = hogarRepository.findByUsuario_Id(usuario.id())
            .orElseThrow(() -> new NoAutenticadoException("No existe hogar para la usuaria."));

        ComidaPlan comida = comidaPlanRepository.findById(comidaId)
            .orElseThrow(() -> new DatosInvalidosException(ERROR_RECURSO_NO_ENCONTRADO, "Comida no encontrada."));

        if (!comida.getPlanSemanal().getId().equals(planId)) {
            throw new DatosInvalidosException("PLAN_NO_PERMITIDO", "La comida no pertenece al plan indicado.");
        }

        if (!comida.getPlanSemanal().getHogar().getId().equals(hogar.getId())) {
            throw new DatosInvalidosException("PLAN_NO_PERMITIDO", "No puedes operar sobre el plan de otro hogar.");
        }

        comidaPlanRepository.delete(comida);

        // regenerar lista de compras al eliminar
        listaComprasService.generarListaCompras(planId);
    }

    @Override
    @Transactional
    public ComidaPlanResponse marcarPreparada(Long planId, Long comidaId) {
        UsuarioAutenticado usuario = usuarioActualService.obtener();
        Hogar hogar = hogarRepository.findByUsuario_Id(usuario.id())
            .orElseThrow(() -> new NoAutenticadoException("No existe hogar para la usuaria."));

        ComidaPlan comida = comidaPlanRepository.findById(comidaId)
            .orElseThrow(() -> new DatosInvalidosException(ERROR_RECURSO_NO_ENCONTRADO, "Comida no encontrada."));

        if (!comida.getPlanSemanal().getId().equals(planId)) {
            throw new DatosInvalidosException("PLAN_NO_PERMITIDO", "La comida no pertenece al plan indicado.");
        }

        if (!comida.getPlanSemanal().getHogar().getId().equals(hogar.getId())) {
            throw new DatosInvalidosException("PLAN_NO_PERMITIDO", "No puedes operar sobre el plan de otro hogar.");
        }

        comida.setPreparada(true);
        comida.setPreparadaEn(LocalDateTime.now(ZoneOffset.UTC));
        ComidaPlan guardada = comidaPlanRepository.save(comida);

        // generar lista si es necesario (por ejemplo si marcar preparada afecta lista)
        listaComprasService.generarListaCompras(planId);

        return aComidaPlanResponse(guardada);
    }

    private void validarRequestAsignacion(ComidaPlanAssignRequest r) {
        if (r == null) {
            throw new DatosInvalidosException(ERROR_PLAN_DATOS_INVALIDOS, "Request vacío.");
        }
        if (r.dia() == null || r.dia() < 1 || r.dia() > 7) {
            throw new DatosInvalidosException(ERROR_PLAN_DATOS_INVALIDOS, "El día debe estar entre 1 y 7.");
        }
        if (r.tipoComida() == null || r.tipoComida().isBlank()
            || !(r.tipoComida().equals("DESAYUNO") || r.tipoComida().equals("ALMUERZO") || r.tipoComida().equals("CENA"))) {
            throw new DatosInvalidosException(ERROR_PLAN_DATOS_INVALIDOS, "Tipo de comida inválido.");
        }
        if (r.recetaId() == null) {
            throw new DatosInvalidosException(ERROR_PLAN_DATOS_INVALIDOS, "RecetaId es obligatorio.");
        }
        if (r.idCambioCliente() != null && r.idCambioCliente().length() > 64) {
            throw new DatosInvalidosException(ERROR_PLAN_DATOS_INVALIDOS, "idCambioCliente demasiado largo.");
        }
    }

    private boolean recetaActiva(Long recetaId) {
        if (recetaId == null) return false;
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM receta WHERE id = ? AND activa = TRUE",
            Integer.class, recetaId);
        return count != null && count > 0;
    }

    private PlanSemanalResponse construirRespuesta(PlanSemanal plan) {
        List<ComidaPlanResponse> comidas = comidaPlanRepository
            .findByPlanSemanal_IdOrderByDiaAscTipoComidaAsc(plan.getId())
            .stream()
            .map(this::aComidaPlanResponse)
            .collect(Collectors.toList());

        return new PlanSemanalResponse(
            plan.getId(),
            plan.getSemanaInicio(),
            plan.getCreadoEn(),
            comidas);
    }

    private ComidaPlanResponse aComidaPlanResponse(ComidaPlan c) {
        return new ComidaPlanResponse(
            c.getId(),
            c.getPlanSemanal().getId(),
            c.getDia(),
            c.getTipoComida(),
            c.getRecetaId(),
            c.isPreparada(),
            c.getPreparadaEn(),
            c.getIdCambioCliente()
        );
    }
}
