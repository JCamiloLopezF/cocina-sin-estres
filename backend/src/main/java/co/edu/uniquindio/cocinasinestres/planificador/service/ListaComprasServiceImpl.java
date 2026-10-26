package co.edu.uniquindio.cocinasinestres.planificador.service;

import co.edu.uniquindio.cocinasinestres.comun.error.DatosInvalidosException;
import co.edu.uniquindio.cocinasinestres.comun.error.RecursoNoEncontradoException;
import co.edu.uniquindio.cocinasinestres.planificador.domain.MovimientosDespensa;
import co.edu.uniquindio.cocinasinestres.planificador.domain.StockEnDespensa;
import co.edu.uniquindio.cocinasinestres.planificador.dto.CompraResponse;
import co.edu.uniquindio.cocinasinestres.planificador.dto.ItemListaComprasResponse;
import co.edu.uniquindio.cocinasinestres.planificador.dto.ListaComprasResponse;
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
import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Genera la lista de compras agregando las cantidades de ingredientes por producto
 * y descontando lo que el hogar ya tiene en su despensa: solo se compra lo que falta.
 *
 * <p>Con la despensa automática (ADR-24) además: no cuenta lo ya preparado, suma la
 * porción de quien tiene alergia, repone lo que quedó por debajo del mínimo y, al marcar
 * un producto como comprado, lo pasa a la despensa con su vencimiento estimado.</p>
 */
@Service
public class ListaComprasServiceImpl implements ListaComprasService {
    private static final String ERROR_RECURSO_NO_ENCONTRADO = "RECURSO_NO_ENCONTRADO";
    private static final String MOTIVO_PLAN = "PLAN";
    private static final String MOTIVO_REPONER = "REPONER";
    private static final String MOTIVO_ALERGIA = "ALERGIA";
    /** Lo más que cabe en {@code costo_real DECIMAL(12,2)}. */
    private static final BigDecimal COSTO_MAXIMO = new BigDecimal("9999999999.99");

    private final PlanSemanalRepository planSemanalRepository;
    private final ComidaPlanRepository comidaPlanRepository;
    private final ListaComprasRepository listaComprasRepository;
    private final ItemListaComprasRepository itemListaComprasRepository;
    private final JdbcTemplate jdbcTemplate;
    private final StockEnDespensa stockEnDespensa;
    private final MovimientosDespensa movimientosDespensa;
    private final ComprasPorAlergias comprasPorAlergias;
    private final Clock reloj;

    public ListaComprasServiceImpl(
        PlanSemanalRepository planSemanalRepository,
        ComidaPlanRepository comidaPlanRepository,
        ListaComprasRepository listaComprasRepository,
        ItemListaComprasRepository itemListaComprasRepository,
        JdbcTemplate jdbcTemplate,
        StockEnDespensa stockEnDespensa,
        MovimientosDespensa movimientosDespensa,
        ComprasPorAlergias comprasPorAlergias,
        Clock reloj) {
        this.planSemanalRepository = planSemanalRepository;
        this.comidaPlanRepository = comidaPlanRepository;
        this.listaComprasRepository = listaComprasRepository;
        this.itemListaComprasRepository = itemListaComprasRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.stockEnDespensa = stockEnDespensa;
        this.movimientosDespensa = movimientosDespensa;
        this.comprasPorAlergias = comprasPorAlergias;
        this.reloj = reloj;
    }

    /** Un producto que falta, por qué y con qué prioridad (1 = indispensable). */
    private record Faltante(BigDecimal cantidad, String motivo, int prioridad) {
    }

    @Override
    @Transactional
    public void generarListaCompras(Long planSemanalId) {
        PlanSemanal plan = planSemanalRepository.findById(planSemanalId)
            .orElseThrow(() -> new DatosInvalidosException(ERROR_RECURSO_NO_ENCONTRADO, "Plan no encontrado"));
        Long hogarId = plan.getHogar().getId();

        // Lo que ya se preparó no se vuelve a comprar: sus ingredientes ya salieron de la
        // despensa (KAN-16).
        List<ComidaPlan> pendientes = comidaPlanRepository.findByPlanSemanal_IdOrderByDiaAscTipoComidaAsc(planSemanalId)
            .stream()
            .filter(c -> !c.isPreparada())
            .toList();

        Map<Long, BigDecimal> requeridos = new HashMap<>();
        if (!pendientes.isEmpty()) {
            // Ingredientes multiplicados por las veces que está la receta. Los opcionales
            // no se compran (igual que en el costo del plan, KAN-24).
            String sql = """
                SELECT ir.producto_id AS producto_id,
                       SUM(ir.cantidad * occ.cnt) AS cantidad_total
                FROM ingrediente_receta ir
                JOIN (
                    SELECT receta_id, COUNT(*) AS cnt
                    FROM comida_plan
                    WHERE plan_semanal_id = ? AND preparada = FALSE
                    GROUP BY receta_id
                ) occ ON ir.receta_id = occ.receta_id
                WHERE ir.opcional = FALSE
                GROUP BY ir.producto_id
                """;
            for (Map<String, Object> row : jdbcTemplate.queryForList(sql, planSemanalId)) {
                Number prodIdNum = (Number) row.get("producto_id");
                Number cantNum = (Number) row.get("cantidad_total");
                if (prodIdNum != null && cantNum != null) {
                    requeridos.put(prodIdNum.longValue(), new BigDecimal(cantNum.toString()));
                }
            }
        }

        // La porción de reemplazo o el plato aparte de quien tiene alergia (ADR-24).
        Set<Long> soloPorAlergia = new HashSet<>();
        comprasPorAlergias.extras(plan.getHogar(), pendientes).forEach((productoId, cantidad) -> {
            if (!requeridos.containsKey(productoId)) {
                soloPorAlergia.add(productoId);
            }
            requeridos.merge(productoId, cantidad, BigDecimal::add);
        });

        // Cruce con la despensa: lo que el hogar ya tiene (y no está vencido) no se
        // compra. La regla de vencimiento la aplica el puerto StockEnDespensa.
        Map<Long, BigDecimal> stock = stockEnDespensa.disponibles(hogarId, LocalDate.now(reloj));

        Map<Long, Faltante> faltantes = new LinkedHashMap<>();
        requeridos.forEach((productoId, requerido) -> {
            BigDecimal faltante = requerido.subtract(stock.getOrDefault(productoId, BigDecimal.ZERO));
            if (faltante.compareTo(BigDecimal.ZERO) > 0) {
                faltantes.put(productoId, new Faltante(
                    faltante, soloPorAlergia.contains(productoId) ? MOTIVO_ALERGIA : MOTIVO_PLAN, 1));
            }
        });
        // Lo que quedó por debajo de su mínimo se repone solo, con menos prioridad (ADR-24).
        stockEnDespensa.bajoMinimo(hogarId).forEach((productoId, falta) -> {
            if (!faltantes.containsKey(productoId) && falta != null && falta.compareTo(BigDecimal.ZERO) > 0) {
                faltantes.put(productoId, new Faltante(falta, MOTIVO_REPONER, 2));
            }
        });

        Optional<ListaCompras> existente = listaComprasRepository.findByPlanSemanal_Id(planSemanalId);
        List<ItemListaCompras> anteriores = existente
            .map(l -> itemListaComprasRepository.findByListaCompras_Id(l.getId()))
            .orElse(List.of());
        boolean hayComprados = anteriores.stream().anyMatch(ItemListaCompras::isComprado);

        if (faltantes.isEmpty() && !hayComprados) {
            // El hogar ya tiene todo: no hay nada que comprar.
            borrarListaSiExiste(planSemanalId);
            return;
        }

        ListaCompras lista = existente.orElseGet(() -> listaComprasRepository.save(new ListaCompras(plan)));

        // Lo ya comprado se conserva (está en la despensa y se puede deshacer). Si de ese
        // producto vuelve a faltar, la fila se reemplaza por la nueva.
        List<ItemListaCompras> aBorrar = anteriores.stream()
            .filter(i -> !i.isComprado() || faltantes.containsKey(i.getProductoId()))
            .toList();
        if (!aBorrar.isEmpty()) {
            itemListaComprasRepository.deleteAll(aBorrar);
            // forzar el delete antes de insertar, para no chocar con la llave única
            itemListaComprasRepository.flush();
        }
        if (faltantes.isEmpty()) {
            return;
        }

        // Lo que la usuaria corrigió que paga se conserva si el producto sigue pidiendo la
        // misma cantidad; si la cantidad cambió, vuelve a contar el estimado (KAN-26).
        Map<Long, ItemListaCompras> corregidos = new HashMap<>();
        aBorrar.stream()
            .filter(i -> i.getCostoReal() != null)
            .forEach(i -> corregidos.put(i.getProductoId(), i));

        List<ItemListaCompras> items = faltantes.entrySet().stream()
            .map(e -> {
                BigDecimal cantidad = e.getValue().cantidad().setScale(3, RoundingMode.HALF_UP);
                ItemListaCompras item = new ItemListaCompras(
                    lista,
                    e.getKey(),
                    cantidad,
                    costoEstimado(e.getKey(), e.getValue().cantidad()),
                    e.getValue().prioridad(),
                    e.getValue().motivo());
                ItemListaCompras anterior = corregidos.get(e.getKey());
                if (anterior != null && anterior.getCantidad().compareTo(cantidad) == 0) {
                    item.setCostoReal(anterior.getCostoReal());
                }
                return item;
            })
            .collect(Collectors.toList());
        itemListaComprasRepository.saveAll(items);
    }

    @Override
    @Transactional
    public CompraResponse marcarComprado(PlanSemanal plan, Long itemId, boolean comprado) {
        ItemListaCompras item = itemListaComprasRepository.findById(itemId)
            .filter(i -> i.getListaCompras().getPlanSemanal().getId().equals(plan.getId()))
            .orElseThrow(() -> new RecursoNoEncontradoException(
                ERROR_RECURSO_NO_ENCONTRADO, "Ese producto no está en la lista de compras."));
        Long hogarId = plan.getHogar().getId();
        LocalDate hoy = LocalDate.now(reloj);
        LocalDate venceEn = null;

        if (comprado && !item.isComprado()) {
            // Se compra en presentaciones completas: si faltan 300 g de arroz, entra la bolsa de 1 kg.
            MovimientosDespensa.DatosCompra datos = movimientosDespensa.datosCompra(item.getProductoId());
            BigDecimal cantidad = enPresentaciones(item.getCantidad(), datos.presentacion());
            LocalDate venceNuevo = datos.vidaUtilDias() == null ? null : hoy.plusDays(datos.vidaUtilDias());
            venceEn = movimientosDespensa.agregar(hogarId, item.getProductoId(), cantidad, venceNuevo, hoy);
            item.setComprado(true);
            item.setCantidadAgregada(cantidad);
        } else if (!comprado && item.isComprado()) {
            if (item.getCantidadAgregada() != null) {
                movimientosDespensa.quitar(hogarId, item.getProductoId(), item.getCantidadAgregada());
            }
            item.setComprado(false);
            item.setCantidadAgregada(null);
        }
        return new CompraResponse(aItem(itemListaComprasRepository.save(item)), venceEn);
    }

    @Override
    @Transactional
    public ListaComprasResponse cambiarCosto(PlanSemanal plan, Long itemId, BigDecimal costoReal) {
        if (costoReal != null && (costoReal.signum() < 0 || costoReal.compareTo(COSTO_MAXIMO) > 0)) {
            throw new DatosInvalidosException(
                "LISTA_COSTO_INVALIDO", "Escribe cuánto pagaste: un valor en pesos, sin signo negativo.");
        }
        ItemListaCompras item = itemListaComprasRepository.findById(itemId)
            .filter(i -> i.getListaCompras().getPlanSemanal().getId().equals(plan.getId()))
            .orElseThrow(() -> new RecursoNoEncontradoException(
                ERROR_RECURSO_NO_ENCONTRADO, "Ese producto no está en la lista de compras."));
        item.setCostoReal(costoReal == null ? null : costoReal.setScale(2, RoundingMode.HALF_UP));
        itemListaComprasRepository.save(item);
        return consultar(plan.getId());
    }

    /** Redondea hacia arriba a presentaciones completas; sin presentación, la cantidad tal cual. */
    static BigDecimal enPresentaciones(BigDecimal cantidad, BigDecimal presentacion) {
        if (presentacion == null || presentacion.signum() <= 0) {
            return cantidad.setScale(3, RoundingMode.HALF_UP);
        }
        BigDecimal unidades = cantidad.divide(presentacion, 0, RoundingMode.CEILING);
        return presentacion.multiply(unidades.max(BigDecimal.ONE)).setScale(3, RoundingMode.HALF_UP);
    }

    /** Costo de una cantidad con el precio de referencia: {@code cantidad / presentación * precio}. */
    private BigDecimal costoEstimado(Long productoId, BigDecimal cantidad) {
        Map<String, Object> prod;
        try {
            prod = jdbcTemplate.queryForMap(
                "SELECT cantidad_unidad_compra, precio_unidad_compra FROM producto WHERE id = ?", productoId);
        } catch (Exception e) {
            // si no existe el producto o hay error, dejamos costo en 0
            return BigDecimal.ZERO;
        }
        Number cantidadUnidadNum = (Number) prod.get("cantidad_unidad_compra");
        Number precioNum = (Number) prod.get("precio_unidad_compra");
        if (cantidadUnidadNum == null || precioNum == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal cantidadUnidad = new BigDecimal(cantidadUnidadNum.toString());
        BigDecimal precioUnidad = new BigDecimal(precioNum.toString());
        if (cantidadUnidad.compareTo(BigDecimal.ZERO) <= 0 || precioUnidad.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        return cantidad.divide(cantidadUnidad, 6, RoundingMode.HALF_UP)
            .multiply(precioUnidad)
            .setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    @Transactional(readOnly = true)
    public ListaComprasResponse consultar(Long planSemanalId) {
        return listaComprasRepository.findByPlanSemanal_Id(planSemanalId)
            .map(lista -> {
                List<ItemListaComprasResponse> items = itemListaComprasRepository
                    .findByListaCompras_Id(lista.getId())
                    .stream()
                    .sorted(Comparator
                        .comparing(ItemListaCompras::getPrioridad)
                        .thenComparing(ItemListaCompras::getProductoId))
                    .map(ListaComprasServiceImpl::aItem)
                    .toList();

                // Lo que la usuaria corrigió que pagó cuenta en vez del estimado (KAN-26).
                BigDecimal costoTotal = items.stream()
                    .map(i -> i.costoReal() != null ? i.costoReal() : i.costoEstimado())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

                return new ListaComprasResponse(
                    lista.getId(),
                    planSemanalId,
                    lista.getGeneradaEn(),
                    items,
                    costoTotal
                );
            })
            // Plan sin comidas: aún no hay nada que comprar, y eso no es un error.
            .orElseGet(() -> new ListaComprasResponse(
                null,
                planSemanalId,
                null,
                List.of(),
                BigDecimal.ZERO
            ));
    }

    private static ItemListaComprasResponse aItem(ItemListaCompras item) {
        return new ItemListaComprasResponse(
            item.getId(),
            item.getProductoId(),
            item.getCantidad(),
            item.getCostoEstimado(),
            item.getCostoReal(),
            item.getPrioridad(),
            item.isComprado(),
            item.getMotivo(),
            item.getCantidadAgregada()
        );
    }

    /** Borra la lista del plan si existe, con sus ítems. */
    private void borrarListaSiExiste(Long planSemanalId) {
        listaComprasRepository.findByPlanSemanal_Id(planSemanalId).ifPresent(lista -> {
            itemListaComprasRepository.deleteByListaCompras_Id(lista.getId());
            // forzar ejecución del delete antes de borrar la lista
            itemListaComprasRepository.flush();
            listaComprasRepository.delete(lista);
        });
    }
}
