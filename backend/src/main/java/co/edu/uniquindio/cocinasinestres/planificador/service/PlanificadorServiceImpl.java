package co.edu.uniquindio.cocinasinestres.planificador.service;

import co.edu.uniquindio.cocinasinestres.comun.error.ConflictoException;
import co.edu.uniquindio.cocinasinestres.comun.error.DatosInvalidosException;
import co.edu.uniquindio.cocinasinestres.comun.error.RecursoNoEncontradoException;
import co.edu.uniquindio.cocinasinestres.perfil.domain.UsuarioAutenticado;
import co.edu.uniquindio.cocinasinestres.perfil.model.Hogar;
import co.edu.uniquindio.cocinasinestres.perfil.repository.HogarRepository;
import co.edu.uniquindio.cocinasinestres.perfil.service.UsuarioActualService;
import co.edu.uniquindio.cocinasinestres.planificador.dto.ComidaPlanAssignRequest;
import co.edu.uniquindio.cocinasinestres.planificador.domain.MovimientosDespensa;
import co.edu.uniquindio.cocinasinestres.planificador.dto.ComidaPlanResponse;
import co.edu.uniquindio.cocinasinestres.planificador.dto.CompraResponse;
import co.edu.uniquindio.cocinasinestres.planificador.dto.ListaComprasResponse;
import co.edu.uniquindio.cocinasinestres.planificador.dto.PlanSemanalResponse;
import co.edu.uniquindio.cocinasinestres.planificador.model.ComidaPlan;
import co.edu.uniquindio.cocinasinestres.planificador.model.PlanSemanal;
import co.edu.uniquindio.cocinasinestres.planificador.repository.ComidaPlanRepository;
import co.edu.uniquindio.cocinasinestres.planificador.repository.PlanSemanalRepository;
import java.math.BigDecimal;
import java.time.DayOfWeek;
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
    private final MovimientosDespensa movimientosDespensa;

    public PlanificadorServiceImpl(
        PlanSemanalRepository planSemanalRepository,
        ComidaPlanRepository comidaPlanRepository,
        UsuarioActualService usuarioActualService,
        HogarRepository hogarRepository,
        JdbcTemplate jdbcTemplate,
        ListaComprasService listaComprasService,
        MovimientosDespensa movimientosDespensa) {
        this.planSemanalRepository = planSemanalRepository;
        this.comidaPlanRepository = comidaPlanRepository;
        this.usuarioActualService = usuarioActualService;
        this.hogarRepository = hogarRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.listaComprasService = listaComprasService;
        this.movimientosDespensa = movimientosDespensa;
    }

    @Override
    @Transactional
    public PlanSemanalResponse crearPlanSiNoExiste(LocalDate semanaInicio) {
        // validar semanaInicio obligatorio y que sea lunes
        if (semanaInicio == null || semanaInicio.getDayOfWeek() != DayOfWeek.MONDAY) {
            throw new DatosInvalidosException("PLAN_SEMANA_INVALIDA", "La semana debe empezar en lunes.");
        }

        UsuarioAutenticado usuario = usuarioActualService.obtener();
        // Si la usuaria no tiene hogar, devolver conflicto específico (no 401)
        Hogar hogar = hogarRepository.findByUsuario_Id(usuario.id())
            .orElseThrow(() -> new ConflictoException("PLAN_SIN_PERFIL_HOGAR", "Completa el perfil de tu hogar antes de planear la semana."));

        PlanSemanal plan = planSemanalRepository.findByHogar_IdAndSemanaInicio(hogar.getId(), semanaInicio)
            .orElseGet(() -> planSemanalRepository.save(new PlanSemanal(hogar, semanaInicio)));

        return construirRespuesta(plan);
    }

    @Override
    @Transactional(readOnly = true)
    public PlanSemanalResponse obtenerPorSemana(LocalDate semanaInicio) {
        // validar semanaInicio obligatorio y que sea lunes
        if (semanaInicio == null || semanaInicio.getDayOfWeek() != DayOfWeek.MONDAY) {
            throw new DatosInvalidosException("PLAN_SEMANA_INVALIDA", "La semana debe empezar en lunes.");
        }

        UsuarioAutenticado usuario = usuarioActualService.obtener();
        Hogar hogar = hogarRepository.findByUsuario_Id(usuario.id())
            .orElseThrow(() -> new ConflictoException("PLAN_SIN_PERFIL_HOGAR", "Completa el perfil de tu hogar antes de planear la semana."));

        PlanSemanal plan = planSemanalRepository.findByHogar_IdAndSemanaInicio(hogar.getId(), semanaInicio)
            .orElseThrow(() -> new RecursoNoEncontradoException(ERROR_RECURSO_NO_ENCONTRADO, "No existe un plan para esa semana."));

        return construirRespuesta(plan);
    }

    @Override
    @Transactional
    public ComidaPlanResponse asignarComida(Long planId, ComidaPlanAssignRequest request) {
        validarRequestAsignacion(request);

        UsuarioAutenticado usuario = usuarioActualService.obtener();

        PlanSemanal plan = planSemanalRepository.findById(planId)
            .orElseThrow(() -> new RecursoNoEncontradoException("Plan semanal no encontrado."));

        Hogar hogar = hogarRepository.findByUsuario_Id(usuario.id())
            .orElseThrow(() -> new ConflictoException("PLAN_SIN_PERFIL_HOGAR", "Completa el perfil de tu hogar antes de planear la semana."));

        if (!plan.getHogar().getId().equals(hogar.getId())) {
            // No confirmar la existencia del plan de otro hogar: devolver 404 igual que "no existe"
            throw new RecursoNoEncontradoException("Plan semanal no encontrado.");
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
                // si pertenece a otro plan: devolver conflicto (409)
                if (!existente.getPlanSemanal().getId().equals(planId)) {
                    throw new ConflictoException("CONFLICTO_ASIGNACION", "El idCambioCliente ya existe en otro plan.");
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
            // Conflicto: devolver 409 con ConflictoException
            if (idCambio != null && !idCambio.isBlank()) {
                return comidaPlanRepository.findByIdCambioCliente(idCambio)
                    .map(this::aComidaPlanResponse)
                    .orElseThrow(() -> new ConflictoException("CONFLICTO_ASIGNACION", "No fue posible completar la asignación por conflicto."));
            }
            throw new ConflictoException("CONFLICTO_ASIGNACION", "No fue posible completar la asignación por conflicto.");
        }
    }

    @Override
    @Transactional
    public void eliminarComida(Long planId, Long comidaId) {
        UsuarioAutenticado usuario = usuarioActualService.obtener();
        Hogar hogar = hogarRepository.findByUsuario_Id(usuario.id())
            .orElseThrow(() -> new ConflictoException("PLAN_SIN_PERFIL_HOGAR", "Completa el perfil de tu hogar antes de planear la semana."));

        ComidaPlan comida = comidaPlanRepository.findById(comidaId)
            .orElseThrow(() -> new RecursoNoEncontradoException("Comida no encontrada."));

        if (!comida.getPlanSemanal().getId().equals(planId)) {
            // No revelar existencia del recurso en otro plan: 404 como "no encontrada"
            throw new RecursoNoEncontradoException("Comida no encontrada.");
        }
        if (!comida.getPlanSemanal().getHogar().getId().equals(hogar.getId())) {
            // Igual: 404
            throw new RecursoNoEncontradoException("Comida no encontrada.");
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
            .orElseThrow(() -> new ConflictoException("PLAN_SIN_PERFIL_HOGAR", "Completa el perfil de tu hogar antes de planear la semana."));

        ComidaPlan comida = comidaPlanRepository.findById(comidaId)
            .orElseThrow(() -> new RecursoNoEncontradoException("Comida no encontrada."));

        if (!comida.getPlanSemanal().getId().equals(planId)) {
            throw new RecursoNoEncontradoException("Comida no encontrada.");
        }
        if (!comida.getPlanSemanal().getHogar().getId().equals(hogar.getId())) {
            throw new RecursoNoEncontradoException("Comida no encontrada.");
        }

        if (!comida.isPreparada()) {
            // Lo que se cocinó sale de la despensa (KAN-16, ADR-24). Solo la primera vez:
            // marcarla otra vez no descuenta de nuevo.
            int descontados = movimientosDespensa.descontarReceta(hogar.getId(), comida.getRecetaId());
            log.info("Comida preparada: planId={} comidaId={} productosDescontados={}", planId, comidaId, descontados);
        }
        comida.setPreparada(true);
        comida.setPreparadaEn(LocalDateTime.now(ZoneOffset.UTC));
        ComidaPlan guardada = comidaPlanRepository.save(comida);
        // generar lista si es necesario (por ejemplo si marcar preparada afecta lista)
        listaComprasService.generarListaCompras(planId);
        return aComidaPlanResponse(guardada);
    }

    @Override
    @Transactional(readOnly = true)
    public ListaComprasResponse consultarListaCompras(Long planId) {
        UsuarioAutenticado usuario = usuarioActualService.obtener();
        Hogar hogar = hogarRepository.findByUsuario_Id(usuario.id())
            .orElseThrow(() -> new ConflictoException("PLAN_SIN_PERFIL_HOGAR", "Completa el perfil de tu hogar antes de planear la semana."));

        PlanSemanal plan = planSemanalRepository.findById(planId)
            .orElseThrow(() -> new RecursoNoEncontradoException(ERROR_RECURSO_NO_ENCONTRADO, "Plan semanal no encontrado."));

        if (!plan.getHogar().getId().equals(hogar.getId())) {
            // Plan de otro hogar: mismo 404 que "no existe" (RNF-22)
            throw new RecursoNoEncontradoException(ERROR_RECURSO_NO_ENCONTRADO, "Plan semanal no encontrado.");
        }

        return listaComprasService.consultar(planId);
    }

    @Override
    @Transactional
    public CompraResponse marcarComprado(Long planId, Long itemId, boolean comprado) {
        UsuarioAutenticado usuario = usuarioActualService.obtener();
        Hogar hogar = hogarRepository.findByUsuario_Id(usuario.id())
            .orElseThrow(() -> new ConflictoException("PLAN_SIN_PERFIL_HOGAR", "Completa el perfil de tu hogar antes de planear la semana."));
        PlanSemanal plan = planSemanalRepository.findById(planId)
            .orElseThrow(() -> new RecursoNoEncontradoException(ERROR_RECURSO_NO_ENCONTRADO, "Plan semanal no encontrado."));
        if (!plan.getHogar().getId().equals(hogar.getId())) {
            // Plan de otro hogar: mismo 404 que "no existe" (RNF-22)
            throw new RecursoNoEncontradoException(ERROR_RECURSO_NO_ENCONTRADO, "Plan semanal no encontrado.");
        }
        return listaComprasService.marcarComprado(plan, itemId, comprado);
    }

    @Override
    @Transactional
    public ListaComprasResponse cambiarCostoItem(Long planId, Long itemId, BigDecimal costoReal) {
        UsuarioAutenticado usuario = usuarioActualService.obtener();
        Hogar hogar = hogarRepository.findByUsuario_Id(usuario.id())
            .orElseThrow(() -> new ConflictoException("PLAN_SIN_PERFIL_HOGAR", "Completa el perfil de tu hogar antes de planear la semana."));
        PlanSemanal plan = planSemanalRepository.findById(planId)
            .orElseThrow(() -> new RecursoNoEncontradoException(ERROR_RECURSO_NO_ENCONTRADO, "Plan semanal no encontrado."));
        if (!plan.getHogar().getId().equals(hogar.getId())) {
            // Plan de otro hogar: mismo 404 que "no existe" (RNF-22)
            throw new RecursoNoEncontradoException(ERROR_RECURSO_NO_ENCONTRADO, "Plan semanal no encontrado.");
        }
        return listaComprasService.cambiarCosto(plan, itemId, costoReal);
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

    @Override
    @Transactional
    public PlanSemanalResponse confirmarBorrador(Long planId) {
        UsuarioAutenticado usuario = usuarioActualService.obtener();
        Hogar hogar = hogarRepository.findByUsuario_Id(usuario.id())
            .orElseThrow(() -> new ConflictoException("PLAN_SIN_PERFIL_HOGAR", "Completa el perfil de tu hogar antes de planear la semana."));
        PlanSemanal plan = planSemanalRepository.findById(planId)
            .filter(p -> p.getHogar().getId().equals(hogar.getId()))
            .orElseThrow(() -> new RecursoNoEncontradoException(ERROR_RECURSO_NO_ENCONTRADO, "Plan semanal no encontrado."));
        if (plan.isBorrador()) {
            plan.setBorrador(false);
            planSemanalRepository.save(plan);
            log.info("Borrador confirmado: planId={}", planId);
        }
        return construirRespuesta(plan);
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
            comidas,
            plan.isBorrador());
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
