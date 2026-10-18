package co.edu.uniquindio.cocinasinestres.planificador.service;

import co.edu.uniquindio.cocinasinestres.avisos.model.Aviso;
import co.edu.uniquindio.cocinasinestres.avisos.repository.AvisoRepository;
import co.edu.uniquindio.cocinasinestres.curaduria.model.Producto;
import co.edu.uniquindio.cocinasinestres.curaduria.repository.ProductoRepository;
import co.edu.uniquindio.cocinasinestres.planificador.dto.PlanCostoResponse;
import co.edu.uniquindio.cocinasinestres.planificador.model.ComidaPlan;
import co.edu.uniquindio.cocinasinestres.planificador.model.PlanSemanal;
import co.edu.uniquindio.cocinasinestres.planificador.repository.ComidaPlanRepository;
import co.edu.uniquindio.cocinasinestres.planificador.repository.PlanSemanalRepository;
import co.edu.uniquindio.cocinasinestres.perfil.domain.CalculadoraPresupuesto;
import co.edu.uniquindio.cocinasinestres.perfil.model.Hogar;
import co.edu.uniquindio.cocinasinestres.perfil.repository.HogarRepository;
import co.edu.uniquindio.cocinasinestres.recetario.model.IngredienteReceta;
import co.edu.uniquindio.cocinasinestres.recetario.repository.IngredienteRecetaRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio que calcula el costo acumulado del plan semanal y genera un aviso
 * idempotente si el costo total supera el presupuesto semanal.
 *
 * Supuestos:
 * - Los repositorios inyectados existen en el proyecto.
 * - El precio y la cantidad de compra están en Producto (getCantidadUnidadCompra, getPrecioUnidadCompra).
 */
@Service
public class PlanificadorCostoService {

    private final PlanSemanalRepository planSemanalRepository;
    private final ComidaPlanRepository comidaPlanRepository;
    private final IngredienteRecetaRepository ingredienteRepo;
    private final ProductoRepository productoRepo;
    private final HogarRepository hogarRepository;
    private final AvisoRepository avisoRepository;

    public PlanificadorCostoService(
        PlanSemanalRepository planSemanalRepository,
        ComidaPlanRepository comidaPlanRepository,
        IngredienteRecetaRepository ingredienteRepo,
        ProductoRepository productoRepo,
        HogarRepository hogarRepository,
        AvisoRepository avisoRepository
    ) {
        this.planSemanalRepository = planSemanalRepository;
        this.comidaPlanRepository = comidaPlanRepository;
        this.ingredienteRepo = ingredienteRepo;
        this.productoRepo = productoRepo;
        this.hogarRepository = hogarRepository;
        this.avisoRepository = avisoRepository;
    }

    /**
     * Calcula acumulado por día y crea un aviso PRESUPUESTO_EXCEDIDO si el total supera el presupuesto semanal.
     *
     * @param hogarId     id del hogar
     * @param semanaInicio lunes de la semana planificada
     * @return DTO con acumulado por día (7 elementos), presupuesto y flag
     */
    @Transactional
    public PlanCostoResponse calcularCostoAcumulado(Long hogarId, LocalDate semanaInicio) {
        PlanSemanal plan = planSemanalRepository.findByHogar_IdAndSemanaInicio(hogarId, semanaInicio)
            .orElseThrow(() -> new IllegalArgumentException("No existe plan para hogar/semana"));

        Hogar hogar = hogarRepository.findById(hogarId)
            .orElseThrow(() -> new IllegalArgumentException("Hogar no encontrado"));

        BigDecimal presupuestoSemanal = CalculadoraPresupuesto.presupuestoSemanal(
            hogar.getPresupuestoMonto(),
            hogar.getPeriodicidadIngreso()
        );

        // costo por día (0=lunes .. 6=domingo)
        BigDecimal[] costoPorDia = new BigDecimal[7];
        Arrays.fill(costoPorDia, BigDecimal.ZERO);

        List<ComidaPlan> comidas = comidaPlanRepository.findByPlanSemanal_IdOrderByDiaAscTipoComidaAsc(plan.getId());
        for (ComidaPlan c : comidas) {
            BigDecimal costoReceta = calcularCostoReceta(c.getRecetaId());
            int idx = c.getDia() - 1;
            if (idx >= 0 && idx < 7) {
                costoPorDia[idx] = costoPorDia[idx].add(costoReceta);
            }
        }

        // acumulado
        List<BigDecimal> acumulado = new ArrayList<>(7);
        BigDecimal running = BigDecimal.ZERO;
        for (int i = 0; i < 7; i++) {
            running = running.add(costoPorDia[i]).setScale(2, RoundingMode.HALF_UP);
            acumulado.add(running);
        }

        boolean excede = running.compareTo(presupuestoSemanal) > 0;

        if (excede && presupuestoSemanal.compareTo(BigDecimal.ZERO) > 0) {
            String clave = String.format("PRESUPUESTO:%d:%s", hogarId, semanaInicio);
            if (!avisoRepository.existsByClaveIdempotencia(clave)) {
                // Usamos la entidad Hogar que ya cargamos (variable hogar)
                Aviso aviso = new Aviso(hogar, "PRESUPUESTO_EXCEDIDO", "El plan semanal supera tu presupuesto.", clave);
                avisoRepository.save(aviso);
            }
        }

        return new PlanCostoResponse(acumulado, presupuestoSemanal, excede);
    }

    /**
     * Calcula el costo de una receta sumando el coste de cada ingrediente.
     * Fórmula: (cantidadIngrediente / producto.cantidadUnidadCompra) * producto.precioUnidadCompra
     *
     * @param recetaId id de la receta
     * @return costo total de la receta (scale 2)
     */
    private BigDecimal calcularCostoReceta(Long recetaId) {
        List<IngredienteReceta> ingredientes = ingredienteRepo.findByReceta_Id(recetaId);
        BigDecimal total = BigDecimal.ZERO;
        for (IngredienteReceta ing : ingredientes) {
            Long productoId = ing.getProductoId();
            Producto producto = productoRepo.findById(productoId)
                .orElseThrow(() -> new IllegalStateException("Producto no encontrado: " + productoId));

            BigDecimal cantidadIngrediente = ing.getCantidad();
            BigDecimal cantidadUnidadCompra = producto.getCantidadUnidadCompra();
            BigDecimal precioUnidadCompra = producto.getPrecioUnidadCompra();

            if (cantidadUnidadCompra == null || precioUnidadCompra == null) {
                throw new IllegalStateException("Producto sin cantidad/precio de compra: " + productoId);
            }

            BigDecimal factor = cantidadIngrediente.divide(cantidadUnidadCompra, 8, RoundingMode.HALF_UP);
            BigDecimal costo = factor.multiply(precioUnidadCompra);
            total = total.add(costo);
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }
}
