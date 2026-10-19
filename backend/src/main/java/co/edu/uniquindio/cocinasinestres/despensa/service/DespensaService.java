package co.edu.uniquindio.cocinasinestres.despensa.service;

import co.edu.uniquindio.cocinasinestres.comun.error.ConflictoException;
import co.edu.uniquindio.cocinasinestres.curaduria.domain.TipoCantidad;
import co.edu.uniquindio.cocinasinestres.curaduria.domain.UnidadBase;
import co.edu.uniquindio.cocinasinestres.comun.error.DatosInvalidosException;
import co.edu.uniquindio.cocinasinestres.comun.error.RecursoNoEncontradoException;
import co.edu.uniquindio.cocinasinestres.despensa.domain.BusquedaTexto;
import co.edu.uniquindio.cocinasinestres.despensa.domain.CatalogoProductos;
import co.edu.uniquindio.cocinasinestres.despensa.domain.CodigoDespensa;
import co.edu.uniquindio.cocinasinestres.despensa.domain.DatosItemDespensa;
import co.edu.uniquindio.cocinasinestres.despensa.domain.FiltroDespensa;
import co.edu.uniquindio.cocinasinestres.despensa.domain.HogarDeUsuaria;
import co.edu.uniquindio.cocinasinestres.despensa.domain.ProductoCatalogo;
import co.edu.uniquindio.cocinasinestres.despensa.domain.ReglasDespensa;
import co.edu.uniquindio.cocinasinestres.despensa.domain.ValidadorItemDespensa;
import co.edu.uniquindio.cocinasinestres.despensa.dto.EdicionItemDespensaRequest;
import co.edu.uniquindio.cocinasinestres.despensa.dto.ItemDespensaRequest;
import co.edu.uniquindio.cocinasinestres.despensa.dto.ProductoAdicionalRequest;
import co.edu.uniquindio.cocinasinestres.despensa.dto.ItemDespensaResponse;
import co.edu.uniquindio.cocinasinestres.despensa.dto.ProductoDisponibleResponse;
import co.edu.uniquindio.cocinasinestres.despensa.dto.ResumenDespensaResponse;
import co.edu.uniquindio.cocinasinestres.despensa.model.ItemDespensa;
import co.edu.uniquindio.cocinasinestres.despensa.repository.ItemDespensaRepository;
import co.edu.uniquindio.cocinasinestres.perfil.service.UsuarioActualService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.Collator;
import java.text.Normalizer;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Casos de uso de la despensa virtual: agregar, consultar, editar y quitar productos del
 * hogar de la usuaria que hace la petición.
 *
 * <p>Toda operación se limita al hogar de la usuaria. Un registro de otro hogar responde
 * como si no existiera (404), para no confirmar que existe (RNF-22).</p>
 */
@Service
public class DespensaService {

    private static final Collator COLATOR_ES = Collator.getInstance(Locale.forLanguageTag("es-CO"));

    /** Categoría de los productos adicionales cuando la usuaria no elige una. */
    static final String CATEGORIA_ADICIONAL = "OTROS";

    private static final int LARGO_MAXIMO_NOMBRE = 120;
    private static final int LARGO_MAXIMO_CATEGORIA = 60;
    private static final BigDecimal CAPACIDAD_ADICIONAL_GRANEL = new BigDecimal("1000");

    private final ItemDespensaRepository itemRepository;
    private final CatalogoProductos catalogo;
    private final HogarDeUsuaria hogarDeUsuaria;
    private final UsuarioActualService usuarioActualService;
    private final Clock reloj;

    public DespensaService(
        ItemDespensaRepository itemRepository,
        CatalogoProductos catalogo,
        HogarDeUsuaria hogarDeUsuaria,
        UsuarioActualService usuarioActualService,
        Clock reloj) {

        this.itemRepository = itemRepository;
        this.catalogo = catalogo;
        this.hogarDeUsuaria = hogarDeUsuaria;
        this.usuarioActualService = usuarioActualService;
        this.reloj = reloj;
    }

    /**
     * Lista la despensa del hogar.
     *
     * @param filtro pestaña: TODO (por defecto), POR_VENCER o BAJO_MINIMO
     * @param buscar texto que debe contener el nombre del producto; sin distinguir
     *               mayúsculas ni tildes. Vacío o ausente no filtra
     * @throws DatosInvalidosException si el filtro no existe
     */
    @Transactional(readOnly = true)
    public List<ItemDespensaResponse> listar(String filtro, String buscar) {

        FiltroDespensa filtroElegido = FiltroDespensa.desde(filtro);

        return construirRespuestas(hogarIdActual()).stream()
            .filter(item -> BusquedaTexto.coincide(item.nombre(), buscar))
            .filter(item -> ReglasDespensa.cumpleFiltro(
                filtroElegido, item.estadoVencimiento(), item.bajoMinimo()))
            .sorted(ordenPara(filtroElegido))
            .toList();
    }

    /** Cuenta los productos del hogar para la tarjeta de la pantalla de inicio. */
    @Transactional(readOnly = true)
    public ResumenDespensaResponse resumen() {

        List<ItemDespensaResponse> items = construirRespuestas(hogarIdActual());

        int porVencer = (int) items.stream()
            .filter(item -> ReglasDespensa.cumpleFiltro(
                FiltroDespensa.POR_VENCER, item.estadoVencimiento(), item.bajoMinimo()))
            .count();

        int bajoMinimo = (int) items.stream()
            .filter(ItemDespensaResponse::bajoMinimo)
            .count();

        return new ResumenDespensaResponse(items.size(), porVencer, bajoMinimo);
    }

    /**
     * Productos del catálogo que la usuaria puede agregar (pantalla 16), ordenados por
     * nombre. Cada uno dice si ya está en la despensa, para editarlo en vez de repetirlo.
     *
     * @param buscar texto que debe contener el nombre; sin distinguir mayúsculas ni tildes.
     *               Vacío o ausente devuelve todo el catálogo disponible
     */
    @Transactional(readOnly = true)
    public List<ProductoDisponibleResponse> catalogo(String buscar) {

        Long hogarId = hogarIdActual();

        Map<Long, Long> itemPorProducto = itemRepository.findByHogarId(hogarId).stream()
            .collect(Collectors.toMap(ItemDespensa::getProductoId, ItemDespensa::getId, (a, b) -> a));

        return catalogo.listarDisponibles(hogarId).stream()
            .filter(producto -> BusquedaTexto.coincide(producto.nombre(), buscar))
            .sorted(Comparator.comparing(ProductoCatalogo::nombre, COLATOR_ES))
            .map(producto -> new ProductoDisponibleResponse(
                producto.id(),
                producto.nombre(),
                producto.categoria(),
                producto.tipoCantidad(),
                producto.unidadBase(),
                producto.unidadCompra(),
                producto.cantidadUnidadCompra(),
                producto.precioUnidadCompra(),
                itemPorProducto.get(producto.id())))
            .toList();
    }

    /**
     * @throws RecursoNoEncontradoException si no existe o es de otro hogar
     */
    @Transactional(readOnly = true)
    public ItemDespensaResponse obtenerPorId(Long id) {

        ItemDespensa item = buscarOExcepcion(id, hogarIdActual());

        return aResponse(item, productoDe(item));
    }

    /**
     * Agrega un producto del catálogo a la despensa.
     *
     * @throws DatosInvalidosException      si faltan datos o incumplen la regla de cantidad mixta
     * @throws RecursoNoEncontradoException si el producto no existe o no está disponible
     * @throws ConflictoException           si el producto ya está en la despensa o la usuaria
     *                                      aún no tiene hogar
     */
    @Transactional
    public ItemDespensaResponse agregar(ItemDespensaRequest request) {

        Long hogarId = hogarIdActual();

        if (request.productoId() == null) {
            throw new DatosInvalidosException(
                CodigoDespensa.PRODUCTO_REQUERIDO,
                "Elige el producto que quieres agregar."
            );
        }

        ProductoCatalogo producto = catalogo.buscarDisponible(request.productoId(), hogarId)
            .orElseThrow(() ->
                new RecursoNoEncontradoException("No encontramos ese producto en el catálogo."));

        DatosItemDespensa datos = ValidadorItemDespensa.validar(
            producto,
            request.cantidad(),
            request.nivel(),
            request.cantidadMinima(),
            request.nivelMinimo(),
            request.fechaVencimiento(),
            request.ubicacion()
        );

        if (itemRepository.existsByHogarIdAndProductoId(hogarId, producto.id())) {
            throw productoRepetido();
        }

        ItemDespensa guardado;

        try {
            guardado = itemRepository.saveAndFlush(new ItemDespensa(hogarId, producto.id(), datos));
        } catch (DataIntegrityViolationException e) {
            // Dos peticiones a la vez pasaron la comprobación de arriba: gana la primera
            // y la base rechaza la segunda con uk_item_despensa_hogar_id_producto_id.
            throw productoRepetido();
        }

        return aResponse(guardado, producto);
    }

    /**
     * Registra un producto que no está en el catálogo (solo para este hogar, ADR-18) y lo
     * agrega a la despensa en la misma transacción: si los datos del ítem no sirven, no
     * queda un producto suelto.
     *
     * @throws DatosInvalidosException si falta el nombre o el tipo, la unidad no cuadra con
     *                                 el tipo o los datos del ítem incumplen la cantidad mixta
     * @throws ConflictoException      si ya existe un producto con ese nombre para el hogar
     *                                 o la usuaria aún no tiene hogar
     */
    @Transactional
    public ItemDespensaResponse agregarAdicional(ProductoAdicionalRequest request) {

        Long hogarId = hogarIdActual();

        String nombre = request.nombre() == null ? "" : request.nombre().trim().replaceAll("\\s+", " ");
        if (nombre.isEmpty()) {
            throw new DatosInvalidosException(CodigoDespensa.NOMBRE_REQUERIDO, "Escribe el nombre del producto.");
        }
        if (nombre.length() > LARGO_MAXIMO_NOMBRE) {
            throw new DatosInvalidosException(CodigoDespensa.NOMBRE_LARGO,
                "El nombre puede tener hasta " + LARGO_MAXIMO_NOMBRE + " caracteres.");
        }
        if (request.tipoCantidad() == null) {
            throw new DatosInvalidosException(CodigoDespensa.TIPO_REQUERIDO,
                "Indica si el producto se cuenta por unidades o va a granel.");
        }

        boolean granel = request.tipoCantidad() == TipoCantidad.GRANEL;
        UnidadBase unidad = request.unidadBase() != null ? request.unidadBase() : (granel ? UnidadBase.g : UnidadBase.unidad);
        if (granel == (unidad == UnidadBase.unidad)) {
            throw new DatosInvalidosException(CodigoDespensa.UNIDAD_INVALIDA,
                "Lo que se cuenta va en unidades; lo que va a granel, en gramos o mililitros.");
        }

        String categoria = request.categoria() == null || request.categoria().isBlank()
            ? CATEGORIA_ADICIONAL
            : request.categoria().trim().toUpperCase(Locale.ROOT);
        if (categoria.length() > LARGO_MAXIMO_CATEGORIA) {
            categoria = CATEGORIA_ADICIONAL;
        }

        String clave = claveNombre(nombre);
        boolean repetido = catalogo.listarDisponibles(hogarId).stream()
            .anyMatch(producto -> claveNombre(producto.nombre()).equals(clave));
        if (repetido) {
            throw new ConflictoException(CodigoDespensa.PRODUCTO_EXISTE,
                "Ya hay un producto llamado así. Búscalo en el catálogo y agrégalo desde ahí.");
        }

        // A granel, "Lleno" equivale a un kilo o un litro: es solo la referencia para estimar.
        BigDecimal capacidad = granel ? CAPACIDAD_ADICIONAL_GRANEL : null;

        // Se valida antes de guardar el producto, con los mismos datos que tendrá.
        DatosItemDespensa datos = ValidadorItemDespensa.validar(
            new ProductoCatalogo(0L, nombre, categoria, request.tipoCantidad(), unidad, capacidad, null, null, null),
            request.cantidad(),
            request.nivel(),
            request.cantidadMinima(),
            request.nivelMinimo(),
            request.fechaVencimiento(),
            request.ubicacion()
        );

        ProductoCatalogo producto = catalogo.registrarAdicional(
            hogarId, nombre, categoria, request.tipoCantidad(), unidad, capacidad);
        ItemDespensa guardado = itemRepository.saveAndFlush(new ItemDespensa(hogarId, producto.id(), datos));
        return aResponse(guardado, producto);
    }

    /** Nombre comparable: sin tildes, sin mayúsculas y con espacios simples. */
    private static String claveNombre(String nombre) {
        return Normalizer.normalize(nombre.trim(), Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .replaceAll("\\s+", " ")
            .toLowerCase(Locale.ROOT);
    }

    /**
     * Edita la cantidad o nivel, el mínimo, el vencimiento y la ubicación de un producto.
     *
     * @throws RecursoNoEncontradoException si no existe o es de otro hogar
     * @throws DatosInvalidosException      si incumple la regla de cantidad mixta
     */
    @Transactional
    public ItemDespensaResponse actualizar(Long id, EdicionItemDespensaRequest request) {

        ItemDespensa item = buscarOExcepcion(id, hogarIdActual());
        ProductoCatalogo producto = productoDe(item);

        DatosItemDespensa datos = ValidadorItemDespensa.validar(
            producto,
            request.cantidad(),
            request.nivel(),
            request.cantidadMinima(),
            request.nivelMinimo(),
            request.fechaVencimiento(),
            request.ubicacion()
        );

        item.actualizar(datos);

        return aResponse(item, producto);
    }

    /**
     * Quita un producto de la despensa.
     *
     * @throws RecursoNoEncontradoException si no existe o es de otro hogar
     */
    @Transactional
    public void eliminar(Long id) {

        ItemDespensa item = buscarOExcepcion(id, hogarIdActual());

        itemRepository.delete(item);
    }

    private List<ItemDespensaResponse> construirRespuestas(Long hogarId) {

        List<ItemDespensa> items = itemRepository.findByHogarId(hogarId);

        if (items.isEmpty()) {
            return List.of();
        }

        Map<Long, ProductoCatalogo> productos = catalogo.buscarPorIds(
            items.stream().map(ItemDespensa::getProductoId).toList()
        );

        LocalDate hoy = LocalDate.now(reloj);

        return items.stream()
            .map(item -> ItemDespensaMapper.aResponse(item, productoDe(item, productos), hoy))
            .toList();
    }

    private ItemDespensaResponse aResponse(ItemDespensa item, ProductoCatalogo producto) {
        return ItemDespensaMapper.aResponse(item, producto, LocalDate.now(reloj));
    }

    private ProductoCatalogo productoDe(ItemDespensa item) {
        return productoDe(item, catalogo.buscarPorIds(List.of(item.getProductoId())));
    }

    private ProductoCatalogo productoDe(ItemDespensa item, Map<Long, ProductoCatalogo> productos) {
        ProductoCatalogo producto = productos.get(item.getProductoId());

        if (producto == null) {
            // El catálogo nunca borra productos (solo los desactiva), así que esto es un
            // dato corrupto y no un error de la usuaria.
            throw new IllegalStateException(
                "El producto " + item.getProductoId() + " de la despensa no existe en el catálogo."
            );
        }

        return producto;
    }

    private ItemDespensa buscarOExcepcion(Long id, Long hogarId) {
        return itemRepository.findByIdAndHogarId(id, hogarId)
            .orElseThrow(() ->
                new RecursoNoEncontradoException("Ese producto no está en tu despensa."));
    }

    private Long hogarIdActual() {
        Long usuarioId = usuarioActualService.obtener().id();

        return hogarDeUsuaria.hogarIdDe(usuarioId)
            .orElseThrow(() ->
                new ConflictoException(
                    CodigoDespensa.HOGAR_REQUERIDO,
                    "Completa el perfil de tu hogar para empezar a usar tu despensa."
                ));
    }

    private static ConflictoException productoRepetido() {
        return new ConflictoException(
            CodigoDespensa.PRODUCTO_REPETIDO,
            "Ya tienes este producto en tu despensa. Puedes editar su cantidad."
        );
    }

    /**
     * Orden de cada pestaña: por vencer, el que vence primero (los ya vencidos arriba);
     * las demás, por categoría y nombre.
     */
    private static Comparator<ItemDespensaResponse> ordenPara(FiltroDespensa filtro) {

        Comparator<ItemDespensaResponse> porNombre =
            Comparator.comparing(ItemDespensaResponse::nombre, COLATOR_ES);

        return switch (filtro) {
            case POR_VENCER -> Comparator
                .comparing(ItemDespensaResponse::fechaVencimiento)
                .thenComparing(porNombre);
            case BAJO_MINIMO -> porNombre;
            case TODO -> Comparator
                .comparing(ItemDespensaResponse::categoria, COLATOR_ES)
                .thenComparing(porNombre);
        };
    }
}
