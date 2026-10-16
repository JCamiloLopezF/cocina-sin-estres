package co.edu.uniquindio.cocinasinestres.planificador.service;

import co.edu.uniquindio.cocinasinestres.comun.error.DatosInvalidosException;
import co.edu.uniquindio.cocinasinestres.planificador.model.ComidaPlan;
import co.edu.uniquindio.cocinasinestres.planificador.model.ItemListaCompras;
import co.edu.uniquindio.cocinasinestres.planificador.model.ListaCompras;
import co.edu.uniquindio.cocinasinestres.planificador.model.PlanSemanal;
import co.edu.uniquindio.cocinasinestres.planificador.repository.ComidaPlanRepository;
import co.edu.uniquindio.cocinasinestres.planificador.repository.ItemListaComprasRepository;
import co.edu.uniquindio.cocinasinestres.planificador.repository.ListaComprasRepository;
import co.edu.uniquindio.cocinasinestres.planificador.repository.PlanSemanalRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Genera la lista de compras agregando las cantidades de ingredientes por producto.
 *
 * Algoritmo:
 *  - Agrega por receta las cantidades de ingrediente (según ingrediente_receta.cantidad)
 *    multiplicadas por la cantidad de veces que aparece la receta en el plan.
 *  - Agrupa por producto_id y calcula costo estimado si hay cantidad_unidad_compra y precio_unidad_compra.
 */
@Service
public class ListaComprasServiceImpl implements ListaComprasService {

    private static final String ERROR_RECURSO_NO_ENCONTRADO = "RECURSO_NO_ENCONTRADO";

    private final PlanSemanalRepository planSemanalRepository;
    private final ComidaPlanRepository comidaPlanRepository;
    private final ListaComprasRepository listaComprasRepository;
    private final ItemListaComprasRepository itemListaComprasRepository;
    private final JdbcTemplate jdbcTemplate;

    public ListaComprasServiceImpl(
        PlanSemanalRepository planSemanalRepository,
        ComidaPlanRepository comidaPlanRepository,
        ListaComprasRepository listaComprasRepository,
        ItemListaComprasRepository itemListaComprasRepository,
        JdbcTemplate jdbcTemplate) {
        this.planSemanalRepository = planSemanalRepository;
        this.comidaPlanRepository = comidaPlanRepository;
        this.listaComprasRepository = listaComprasRepository;
        this.itemListaComprasRepository = itemListaComprasRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void generarListaCompras(Long planSemanalId) {
        PlanSemanal plan = planSemanalRepository.findById(planSemanalId)
            .orElseThrow(() -> new DatosInvalidosException(ERROR_RECURSO_NO_ENCONTRADO, "Plan no encontrado"));

        // Obtener conteo de recetas en el plan (receta_id -> ocurrencias)
        Map<Long, Integer> recetaCounts = comidaPlanRepository.findByPlanSemanal_IdOrderByDiaAscTipoComidaAsc(planSemanalId)
            .stream()
            .map(ComidaPlan::getRecetaId)
            .collect(Collectors.toMap(r -> r, r -> 1, Integer::sum));

        if (recetaCounts.isEmpty()) {
            // Si no hay recetas, borramos lista existente si la hubiera
            listaComprasRepository.findByPlanSemanal_Id(planSemanalId).ifPresent(lista -> {
                itemListaComprasRepository.deleteByListaCompras_Id(lista.getId());
                listaComprasRepository.delete(lista);
            });
            return;
        }

        // SQL: agregamos ingredientes multiplicados por ocurrencias
        // Esta query retorna producto_id y cantidad_total (DECIMAL)
        String sql = """
            SELECT ir.producto_id AS producto_id,
                   SUM(ir.cantidad * occ.cnt) AS cantidad_total
            FROM ingrediente_receta ir
            JOIN (
                SELECT receta_id, COUNT(*) AS cnt
                FROM comida_plan
                WHERE plan_semanal_id = ?
                GROUP BY receta_id
            ) occ ON ir.receta_id = occ.receta_id
            GROUP BY ir.producto_id
            """;

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, planSemanalId);

        // Map producto_id -> cantidad_total (BigDecimal)
        Map<Long, BigDecimal> cantidades = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Number prodIdNum = (Number) row.get("producto_id");
            Number cantNum = (Number) row.get("cantidad_total");
            if (prodIdNum == null || cantNum == null) continue;
            Long productoId = prodIdNum.longValue();
            BigDecimal cantidad = new BigDecimal(cantNum.toString());
            cantidades.put(productoId, cantidad);
        }

        if (cantidades.isEmpty()) {
            // Sin ingredientes relevantes: eliminar lista existente y salir.
            listaComprasRepository.findByPlanSemanal_Id(planSemanalId).ifPresent(lista -> {
                itemListaComprasRepository.deleteByListaCompras_Id(lista.getId());
                listaComprasRepository.delete(lista);
            });
            return;
        }

        // Crear o reusar la lista de compras
        ListaCompras lista = listaComprasRepository.findByPlanSemanal_Id(planSemanalId)
            .orElseGet(() -> listaComprasRepository.save(new ListaCompras(plan)));

        // Borrar items previos
        itemListaComprasRepository.deleteByListaCompras_Id(lista.getId());

        // Para calcular costo estimado consultamos producto por producto
        String sqlProducto = "SELECT cantidad_unidad_compra, precio_unidad_compra FROM producto WHERE id = ?";

        List<ItemListaCompras> items = cantidades.entrySet().stream().map(entry -> {
            Long productoId = entry.getKey();
            BigDecimal cantidadTotal = entry.getValue();

            // Default costo = 0
            BigDecimal costoEstimado = BigDecimal.ZERO;

            Map<String, Object> prod = jdbcTemplate.queryForMap(sqlProducto, productoId);
            if (prod != null) {
                Number cantidadUnidadNum = (Number) prod.get("cantidad_unidad_compra");
                Number precioNum = (Number) prod.get("precio_unidad_compra");
                if (cantidadUnidadNum != null && precioNum != null) {
                    try {
                        BigDecimal cantidadUnidad = new BigDecimal(cantidadUnidadNum.toString());
                        BigDecimal precioUnidad = new BigDecimal(precioNum.toString());
                        if (cantidadUnidad.compareTo(BigDecimal.ZERO) > 0 && precioUnidad.compareTo(BigDecimal.ZERO) >= 0) {
                            // cantidadTotal puede tener 3 decimales; costo = (cantidadTotal / cantidadUnidad) * precioUnidad
                            costoEstimado = cantidadTotal.divide(cantidadUnidad, 6, RoundingMode.HALF_UP)
                                .multiply(precioUnidad)
                                .setScale(2, RoundingMode.HALF_UP);
                        }
                    } catch (Exception e) {
                        // cualquier excepción dejamos costo 0
                        costoEstimado = BigDecimal.ZERO;
                    }
                }
            }

            // prioridad por defecto = 1
            return new ItemListaCompras(lista, productoId, cantidadTotal.setScale(3, RoundingMode.HALF_UP), costoEstimado, 1);
        }).collect(Collectors.toList());

        // Guardar items
        itemListaComprasRepository.saveAll(items);
    }
}
