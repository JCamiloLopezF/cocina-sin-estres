package co.edu.uniquindio.cocinasinestres.planificador.service;

import co.edu.uniquindio.cocinasinestres.comun.error.ConflictoException;
import co.edu.uniquindio.cocinasinestres.comun.error.RecursoNoEncontradoException;
import co.edu.uniquindio.cocinasinestres.perfil.domain.CalculadoraPresupuesto;
import co.edu.uniquindio.cocinasinestres.perfil.domain.UsuarioAutenticado;
import co.edu.uniquindio.cocinasinestres.perfil.model.Hogar;
import co.edu.uniquindio.cocinasinestres.perfil.repository.HogarRepository;
import co.edu.uniquindio.cocinasinestres.perfil.service.UsuarioActualService;
import co.edu.uniquindio.cocinasinestres.planificador.dto.PlanCostoResponse;
import co.edu.uniquindio.cocinasinestres.planificador.model.PlanSemanal;
import co.edu.uniquindio.cocinasinestres.planificador.repository.ComidaPlanRepository;
import co.edu.uniquindio.cocinasinestres.planificador.repository.PlanSemanalRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consulta el costo acumulado del plan semanal frente al presupuesto del hogar (ADR-14).
 *
 * <p>El costo por día sale de una sola consulta SQL sobre las comidas del plan y solo
 * cuenta los ingredientes obligatorios; no usa los repositorios de curaduría ni de
 * recetario (RNF-26). Si el total de la semana supera el presupuesto, la respuesta lo
 * marca con {@code excedePresupuesto}.</p>
 *
 * <p>Suma además el reemplazo o el plato aparte de quien tiene alergia (KAN-24): es comida
 * que también hay que pagar.</p>
 */
@Service
public class PlanificadorCostoService {

    /**
     * Costo por día de la semana. La fórmula por ingrediente es
     * {@code cantidad / cantidadUnidadCompra * precioUnidadCompra}, igual que en
     * la lista de compras.
     */
    private static final String SQL_COSTO_POR_DIA = """
        SELECT cp.dia,
               SUM(ir.cantidad / p.cantidad_unidad_compra * p.precio_unidad_compra) AS costo
        FROM comida_plan cp
        JOIN ingrediente_receta ir ON ir.receta_id = cp.receta_id AND ir.opcional = FALSE
        JOIN producto p ON p.id = ir.producto_id
        WHERE cp.plan_semanal_id = ?
        GROUP BY cp.dia
        """;

    private final PlanSemanalRepository planSemanalRepository;
    private final HogarRepository hogarRepository;
    private final UsuarioActualService usuarioActualService;
    private final JdbcTemplate jdbcTemplate;
    private final ComidaPlanRepository comidaPlanRepository;
    private final CostoPorAlergias costoPorAlergias;

    public PlanificadorCostoService(
        PlanSemanalRepository planSemanalRepository,
        HogarRepository hogarRepository,
        UsuarioActualService usuarioActualService,
        JdbcTemplate jdbcTemplate,
        ComidaPlanRepository comidaPlanRepository,
        CostoPorAlergias costoPorAlergias) {
        this.planSemanalRepository = planSemanalRepository;
        this.hogarRepository = hogarRepository;
        this.usuarioActualService = usuarioActualService;
        this.jdbcTemplate = jdbcTemplate;
        this.comidaPlanRepository = comidaPlanRepository;
        this.costoPorAlergias = costoPorAlergias;
    }

    /**
     * Costo acumulado del plan día a día comparado con el presupuesto semanal del hogar.
     *
     * @param planId plan consultado; debe pertenecer a la hogar de la usuaria autenticada
     * @return acumulado por día (índice 0 = lunes ... índice 6 = domingo), presupuesto
     *         semanal prorrateado y si el total de la semana lo excede
     * @throws RecursoNoEncontradoException si el plan no existe o es de otro hogar
     */
    @Transactional(readOnly = true)
    public PlanCostoResponse calcularCosto(Long planId) {
        UsuarioAutenticado usuario = usuarioActualService.obtener();

        PlanSemanal plan = planSemanalRepository.findById(planId)
            .orElseThrow(() -> new RecursoNoEncontradoException("Plan semanal no encontrado."));

        Hogar hogar = hogarRepository.findByUsuario_Id(usuario.id())
            .orElseThrow(() -> new ConflictoException("PLAN_SIN_PERFIL_HOGAR",
                "Completa el perfil de tu hogar antes de planear la semana."));

        if (!plan.getHogar().getId().equals(hogar.getId())) {
            // No confirmar la existencia del plan de otro hogar: devolver 404 igual que "no existe"
            throw new RecursoNoEncontradoException("Plan semanal no encontrado.");
        }

        BigDecimal presupuestoSemanal = CalculadoraPresupuesto.presupuestoSemanal(
            hogar.getPresupuestoMonto(),
            hogar.getPeriodicidadIngreso());

        // costo por día con una sola consulta (índice 0 = lunes ... 6 = domingo)
        BigDecimal[] costoPorDia = new BigDecimal[7];
        Arrays.fill(costoPorDia, BigDecimal.ZERO);
        for (Map<String, Object> fila : jdbcTemplate.queryForList(SQL_COSTO_POR_DIA, planId)) {
            int dia = ((Number) fila.get("dia")).intValue();
            Object costo = fila.get("costo");
            if (dia >= 1 && dia <= 7 && costo != null) {
                costoPorDia[dia - 1] = new BigDecimal(costo.toString());
            }
        }

        // Lo extra por alergias (reemplazo o plato aparte) también cuesta (KAN-24).
        Map<Integer, BigDecimal> alergias = costoPorAlergias.porDia(
            hogar, comidaPlanRepository.findByPlanSemanal_IdOrderByDiaAscTipoComidaAsc(planId));
        BigDecimal costoAlergias = BigDecimal.ZERO;
        for (Map.Entry<Integer, BigDecimal> extra : alergias.entrySet()) {
            int dia = extra.getKey();
            if (dia >= 1 && dia <= 7) {
                costoPorDia[dia - 1] = costoPorDia[dia - 1].add(extra.getValue());
                costoAlergias = costoAlergias.add(extra.getValue());
            }
        }

        // acumulado del lunes en adelante
        List<BigDecimal> acumulado = new ArrayList<>(7);
        BigDecimal acumuladoHastaAhora = BigDecimal.ZERO;
        for (BigDecimal costoDelDia : costoPorDia) {
            acumuladoHastaAhora = acumuladoHastaAhora.add(costoDelDia)
                .setScale(2, RoundingMode.HALF_UP);
            acumulado.add(acumuladoHastaAhora);
        }

        boolean excede = acumuladoHastaAhora.compareTo(presupuestoSemanal) > 0;

        return new PlanCostoResponse(acumulado, presupuestoSemanal, excede, costoAlergias.setScale(2, RoundingMode.HALF_UP));
    }
}
