package co.edu.uniquindio.cocinasinestres.curaduria.service;

import co.edu.uniquindio.cocinasinestres.comun.error.ConflictoException;
import co.edu.uniquindio.cocinasinestres.comun.error.DatosInvalidosException;
import co.edu.uniquindio.cocinasinestres.comun.error.RecursoNoEncontradoException;
import co.edu.uniquindio.cocinasinestres.curaduria.domain.AccionAuditoria;
import co.edu.uniquindio.cocinasinestres.curaduria.domain.CategoriaProducto;
import co.edu.uniquindio.cocinasinestres.curaduria.domain.EntidadCatalogo;
import co.edu.uniquindio.cocinasinestres.curaduria.domain.ReglaAlmuerzo;
import co.edu.uniquindio.cocinasinestres.curaduria.domain.ValidadorReceta;
import co.edu.uniquindio.cocinasinestres.curaduria.dto.IngredienteCuradoRequest;
import co.edu.uniquindio.cocinasinestres.curaduria.dto.IngredienteCuradoResponse;
import co.edu.uniquindio.cocinasinestres.curaduria.dto.RecetaCuradaRequest;
import co.edu.uniquindio.cocinasinestres.curaduria.dto.RecetaCuradaResponse;
import co.edu.uniquindio.cocinasinestres.curaduria.dto.RecetaCuradaResumen;
import co.edu.uniquindio.cocinasinestres.curaduria.model.AuditoriaCatalogo;
import co.edu.uniquindio.cocinasinestres.curaduria.repository.AuditoriaCatalogoRepository;
import co.edu.uniquindio.cocinasinestres.perfil.service.UsuarioActualService;
import co.edu.uniquindio.cocinasinestres.recetario.domain.PasosReceta;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * El curador crea y edita recetas sin tocar código (RNF-28): nombre, tiempo, porciones,
 * pasos, ingredientes con sus cantidades y en qué comidas va. Antes una receta nueva
 * exigía una migración.
 *
 * <p>Reglas: los pasos siguen la regla del Modo cocinar (un tiempo por paso), los
 * ingredientes son productos activos del catálogo global, un almuerzo es un plato completo
 * ({@link ReglaAlmuerzo}) y el nombre no se repite. Cada cambio queda en la auditoría del
 * catálogo a nombre del curador (ADR-23, RNF-24).</p>
 *
 * <p>Las recetas no se borran: se desactivan, porque los planes siguen apuntando a ellas.</p>
 */
@Service
public class RecetaCuradaService {

    private static final String NO_EXISTE = "La receta solicitada no existe.";

    private final JdbcClient jdbc;
    private final AuditoriaCatalogoRepository auditoriaRepository;
    private final UsuarioActualService usuarioActualService;

    public RecetaCuradaService(
        JdbcClient jdbc,
        AuditoriaCatalogoRepository auditoriaRepository,
        UsuarioActualService usuarioActualService) {
        this.jdbc = jdbc;
        this.auditoriaRepository = auditoriaRepository;
        this.usuarioActualService = usuarioActualService;
    }

    /** Todas las recetas, activas primero y por nombre, con su costo de referencia. */
    @Transactional(readOnly = true)
    public List<RecetaCuradaResumen> listar() {
        return jdbc.sql("""
                SELECT r.id, r.nombre, r.tiempo_minutos, r.porciones, r.para_desayuno, r.para_almuerzo,
                       r.para_cena, r.es_plato_fuerte, r.activa, r.video_youtube_id,
                       COUNT(ir.id) AS ingredientes,
                       COALESCE(SUM(CASE WHEN ir.opcional = FALSE
                                         THEN ir.cantidad / p.cantidad_unidad_compra * p.precio_unidad_compra END), 0) AS costo
                FROM receta r
                LEFT JOIN ingrediente_receta ir ON ir.receta_id = r.id
                LEFT JOIN producto p ON p.id = ir.producto_id
                GROUP BY r.id, r.nombre, r.tiempo_minutos, r.porciones, r.para_desayuno, r.para_almuerzo,
                         r.para_cena, r.es_plato_fuerte, r.activa, r.video_youtube_id
                ORDER BY r.activa DESC, r.nombre
                """)
            .query((rs, n) -> new RecetaCuradaResumen(
                rs.getLong("id"),
                rs.getString("nombre"),
                rs.getInt("tiempo_minutos"),
                rs.getInt("porciones"),
                momentos(rs.getBoolean("para_desayuno"), rs.getBoolean("para_almuerzo"), rs.getBoolean("para_cena")),
                rs.getBoolean("es_plato_fuerte"),
                rs.getBoolean("activa"),
                rs.getInt("ingredientes"),
                pesos(rs.getBigDecimal("costo")),
                rs.getString("video_youtube_id") != null))
            .list();
    }

    @Transactional(readOnly = true)
    public RecetaCuradaResponse obtener(Long id) {
        return buscar(id);
    }

    @Transactional
    public RecetaCuradaResponse crear(RecetaCuradaRequest request) {
        Datos datos = validar(request, null);
        Long curador = usuarioActualService.obtener().id();

        KeyHolder llave = new GeneratedKeyHolder();
        jdbc.sql("""
                INSERT INTO receta (nombre, descripcion, tiempo_minutos, porciones, pasos, preparacion_previa,
                                    activa, modificado_por, es_plato_fuerte, gusta_a_ninos,
                                    para_desayuno, para_almuerzo, para_cena)
                VALUES (:nombre, :descripcion, :tiempo, :porciones, :pasos, :previa,
                        TRUE, :curador, :fuerte, :ninos, :desayuno, :almuerzo, :cena)
                """)
            .paramSource(parametros(datos, curador))
            .update(llave);
        Long id = Objects.requireNonNull(llave.getKey(), "id de la receta").longValue();
        guardarIngredientes(id, datos.ingredientes());

        RecetaCuradaResponse creada = buscar(id);
        Map<String, Object> cambios = new LinkedHashMap<>();
        cambios.put("origen", "pantalla del curador");
        cambios.put("despues", resumen(creada));
        auditoriaRepository.save(new AuditoriaCatalogo(EntidadCatalogo.RECETA, id, AccionAuditoria.CREAR, curador, cambios));
        return creada;
    }

    @Transactional
    public RecetaCuradaResponse actualizar(Long id, RecetaCuradaRequest request) {
        RecetaCuradaResponse antes = buscar(id);
        Datos datos = validar(request, id);
        Map<String, Integer> video = jdbc.sql("SELECT video_inicio_seg, video_fin_seg FROM receta WHERE id = :id")
            .param("id", id)
            .query((rs, n) -> {
                Map<String, Integer> fragmento = new HashMap<>();
                fragmento.put("inicio", (Integer) rs.getObject("video_inicio_seg", Integer.class));
                fragmento.put("fin", (Integer) rs.getObject("video_fin_seg", Integer.class));
                return fragmento;
            })
            .single();
        ValidadorReceta.videoCabe(video.get("inicio"), video.get("fin"), datos.tiempo());

        Long curador = usuarioActualService.obtener().id();
        Map<String, Object> parametros = new HashMap<>(parametros(datos, curador).getValues());
        parametros.put("id", id);
        jdbc.sql("""
                UPDATE receta
                SET nombre = :nombre, descripcion = :descripcion, tiempo_minutos = :tiempo, porciones = :porciones,
                    pasos = :pasos, preparacion_previa = :previa, modificado_por = :curador,
                    es_plato_fuerte = :fuerte, gusta_a_ninos = :ninos,
                    para_desayuno = :desayuno, para_almuerzo = :almuerzo, para_cena = :cena
                WHERE id = :id
                """)
            .params(parametros)
            .update();
        jdbc.sql("DELETE FROM ingrediente_receta WHERE receta_id = :id").param("id", id).update();
        guardarIngredientes(id, datos.ingredientes());

        RecetaCuradaResponse despues = buscar(id);
        Map<String, Object> cambios = diferencias(resumen(antes), resumen(despues));
        if (!cambios.isEmpty()) {
            auditoriaRepository.save(new AuditoriaCatalogo(EntidadCatalogo.RECETA, id, AccionAuditoria.EDITAR, curador, cambios));
        }
        return despues;
    }

    /** Deja de ofrecerse en el recetario y en el plan. Idempotente. */
    @Transactional
    public RecetaCuradaResponse desactivar(Long id) {
        return cambiarActiva(id, false);
    }

    /** Vuelve a ofrecerse. Idempotente. */
    @Transactional
    public RecetaCuradaResponse reactivar(Long id) {
        return cambiarActiva(id, true);
    }

    // ------------------------------------------------------------------

    private RecetaCuradaResponse cambiarActiva(Long id, boolean activa) {
        RecetaCuradaResponse antes = buscar(id);
        if (antes.activa() == activa) {
            return antes;
        }
        Long curador = usuarioActualService.obtener().id();
        jdbc.sql("UPDATE receta SET activa = :activa, modificado_por = :curador WHERE id = :id")
            .param("activa", activa)
            .param("curador", curador)
            .param("id", id)
            .update();
        Map<String, Object> detalle = new LinkedHashMap<>();
        detalle.put("antes", !activa);
        detalle.put("despues", activa);
        Map<String, Object> cambios = new LinkedHashMap<>();
        cambios.put("activa", detalle);
        auditoriaRepository.save(new AuditoriaCatalogo(
            EntidadCatalogo.RECETA, id, activa ? AccionAuditoria.EDITAR : AccionAuditoria.DESACTIVAR, curador, cambios));
        return buscar(id);
    }

    /** Lo que se guarda, ya validado y limpio. */
    private record Datos(
        String nombre,
        String descripcion,
        int tiempo,
        int porciones,
        List<String> pasos,
        String previa,
        List<IngredienteCuradoRequest> ingredientes,
        Set<String> momentos,
        boolean platoFuerte,
        boolean ninos) {
    }

    private record ProductoDelCatalogo(Long id, String nombre, String categoria, boolean global, boolean activo) {
    }

    private Datos validar(RecetaCuradaRequest request, Long id) {
        if (request == null) {
            throw new DatosInvalidosException("RECETA_DATOS_REQUERIDOS", "Envía los datos de la receta.");
        }
        String nombre = ValidadorReceta.nombre(request.nombre());
        String descripcion = ValidadorReceta.opcional(
            request.descripcion(), ValidadorReceta.LARGO_DESCRIPCION, "RECETA_DESCRIPCION_LARGA", "La descripción");
        int tiempo = ValidadorReceta.tiempo(request.tiempoMinutos());
        int porciones = ValidadorReceta.porciones(request.porciones());
        List<String> pasos = ValidadorReceta.pasos(request.pasos());
        String previa = ValidadorReceta.opcional(
            request.preparacionPrevia(), ValidadorReceta.LARGO_PREPARACION, "RECETA_PREPARACION_LARGA", "La preparación previa");
        Set<String> momentos = ValidadorReceta.momentos(request.momentos());
        boolean platoFuerte = request.platoFuerte() == null || request.platoFuerte();
        boolean ninos = Boolean.TRUE.equals(request.gustaANinos());

        List<IngredienteCuradoRequest> ingredientes = ingredientes(request.ingredientes());
        Map<Long, ProductoDelCatalogo> productos = productos(ingredientes.stream().map(IngredienteCuradoRequest::productoId).toList());
        List<String> categoriasObligatorias = new ArrayList<>();
        List<IngredienteCuradoRequest> limpios = new ArrayList<>();
        for (IngredienteCuradoRequest ingrediente : ingredientes) {
            ProductoDelCatalogo producto = productos.get(ingrediente.productoId());
            if (producto == null || !producto.global() || !producto.activo()) {
                throw new DatosInvalidosException("RECETA_INGREDIENTE_INVALIDO",
                    "Uno de los ingredientes no está en el catálogo o fue desactivado. Elígelo de nuevo de la lista.");
            }
            boolean opcional = Boolean.TRUE.equals(ingrediente.opcional());
            limpios.add(new IngredienteCuradoRequest(
                producto.id(),
                ValidadorReceta.cantidad(ingrediente.cantidad(), producto.nombre()),
                opcional,
                Boolean.TRUE.equals(ingrediente.principal())));
            if (!opcional) {
                categoriasObligatorias.add(producto.categoria());
            }
        }
        if (categoriasObligatorias.isEmpty()) {
            throw new DatosInvalidosException("RECETA_INGREDIENTES_REQUERIDOS",
                "Al menos un ingrediente tiene que ser obligatorio.");
        }
        if (platoFuerte && momentos.contains("ALMUERZO") && !ReglaAlmuerzo.esCompleto(categoriasObligatorias)) {
            throw new DatosInvalidosException("RECETA_ALMUERZO_INCOMPLETO", ReglaAlmuerzo.MENSAJE);
        }
        validarNoRepetida(nombre, id);
        return new Datos(nombre, descripcion, tiempo, porciones, pasos, previa, limpios, momentos, platoFuerte, ninos);
    }

    private static List<IngredienteCuradoRequest> ingredientes(List<IngredienteCuradoRequest> ingredientes) {
        if (ingredientes == null || ingredientes.isEmpty()) {
            throw new DatosInvalidosException("RECETA_INGREDIENTES_REQUERIDOS", "Agrega al menos un ingrediente.");
        }
        if (ingredientes.size() > ValidadorReceta.MAXIMO_INGREDIENTES) {
            throw new DatosInvalidosException("RECETA_INGREDIENTES_MUCHOS",
                "Una receta puede tener hasta " + ValidadorReceta.MAXIMO_INGREDIENTES + " ingredientes.");
        }
        Set<Long> vistos = new HashSet<>();
        for (IngredienteCuradoRequest ingrediente : ingredientes) {
            if (ingrediente == null || ingrediente.productoId() == null) {
                throw new DatosInvalidosException("RECETA_INGREDIENTE_INVALIDO", "Elige el producto de cada ingrediente.");
            }
            if (!vistos.add(ingrediente.productoId())) {
                throw new DatosInvalidosException("RECETA_INGREDIENTE_REPETIDO",
                    "Un producto está dos veces en la receta. Suma las cantidades en un solo ingrediente.");
            }
        }
        return ingredientes;
    }

    private Map<Long, ProductoDelCatalogo> productos(List<Long> ids) {
        Map<Long, ProductoDelCatalogo> productos = new HashMap<>();
        jdbc.sql("SELECT id, nombre, categoria, hogar_id, activo FROM producto WHERE id IN (:ids)")
            .param("ids", ids)
            .query(rs -> {
                productos.put(rs.getLong("id"), new ProductoDelCatalogo(
                    rs.getLong("id"),
                    rs.getString("nombre"),
                    rs.getString("categoria"),
                    rs.getObject("hogar_id") == null,
                    rs.getBoolean("activo")));
            });
        return productos;
    }

    /** Dos recetas no se llaman igual, sin importar mayúsculas, tildes ni espacios. */
    private void validarNoRepetida(String nombre, Long id) {
        String clave = CategoriaProducto.claveNombre(nombre);
        boolean repetida = jdbc.sql("SELECT id, nombre FROM receta")
            .query((rs, n) -> Map.entry(rs.getLong("id"), rs.getString("nombre")))
            .list()
            .stream()
            .anyMatch(e -> !e.getKey().equals(id) && CategoriaProducto.claveNombre(e.getValue()).equals(clave));
        if (repetida) {
            throw new ConflictoException("RECETA_DUPLICADA",
                "Ya hay una receta con ese nombre. Edítala o ponle un nombre distinto.");
        }
    }

    private static MapSqlParameterSource parametros(Datos datos, Long curador) {
        return new MapSqlParameterSource()
            .addValue("nombre", datos.nombre())
            .addValue("descripcion", datos.descripcion())
            .addValue("tiempo", datos.tiempo())
            .addValue("porciones", datos.porciones())
            .addValue("pasos", PasosReceta.unir(datos.pasos()))
            .addValue("previa", datos.previa())
            .addValue("curador", curador)
            .addValue("fuerte", datos.platoFuerte())
            .addValue("ninos", datos.ninos())
            .addValue("desayuno", datos.momentos().contains("DESAYUNO"))
            .addValue("almuerzo", datos.momentos().contains("ALMUERZO"))
            .addValue("cena", datos.momentos().contains("CENA"));
    }

    private void guardarIngredientes(Long recetaId, List<IngredienteCuradoRequest> ingredientes) {
        for (IngredienteCuradoRequest ingrediente : ingredientes) {
            jdbc.sql("""
                    INSERT INTO ingrediente_receta (receta_id, producto_id, cantidad, opcional, es_principal)
                    VALUES (:receta, :producto, :cantidad, :opcional, :principal)
                    """)
                .param("receta", recetaId)
                .param("producto", ingrediente.productoId())
                .param("cantidad", ingrediente.cantidad())
                .param("opcional", Boolean.TRUE.equals(ingrediente.opcional()))
                .param("principal", Boolean.TRUE.equals(ingrediente.principal()))
                .update();
        }
    }

    private RecetaCuradaResponse buscar(Long id) {
        record Fila(Long id, String nombre, String descripcion, int tiempo, int porciones, String pasos, String previa,
                    List<String> momentos, boolean fuerte, boolean ninos, boolean activa, boolean video) {
        }
        Fila fila = jdbc.sql("""
                SELECT id, nombre, descripcion, tiempo_minutos, porciones, pasos, preparacion_previa,
                       para_desayuno, para_almuerzo, para_cena, es_plato_fuerte, gusta_a_ninos, activa, video_youtube_id
                FROM receta
                WHERE id = :id
                """)
            .param("id", id)
            .query((rs, n) -> new Fila(
                rs.getLong("id"),
                rs.getString("nombre"),
                rs.getString("descripcion"),
                rs.getInt("tiempo_minutos"),
                rs.getInt("porciones"),
                rs.getString("pasos"),
                rs.getString("preparacion_previa"),
                momentos(rs.getBoolean("para_desayuno"), rs.getBoolean("para_almuerzo"), rs.getBoolean("para_cena")),
                rs.getBoolean("es_plato_fuerte"),
                rs.getBoolean("gusta_a_ninos"),
                rs.getBoolean("activa"),
                rs.getString("video_youtube_id") != null))
            .optional()
            .orElseThrow(() -> new RecursoNoEncontradoException(NO_EXISTE));

        List<IngredienteCuradoResponse> ingredientes = new ArrayList<>();
        BigDecimal[] costo = {BigDecimal.ZERO};
        jdbc.sql("""
                SELECT ir.producto_id, p.nombre, p.categoria, p.unidad_base, ir.cantidad, ir.opcional, ir.es_principal,
                       p.activo, p.cantidad_unidad_compra, p.precio_unidad_compra
                FROM ingrediente_receta ir
                JOIN producto p ON p.id = ir.producto_id
                WHERE ir.receta_id = :id
                ORDER BY ir.id
                """)
            .param("id", id)
            .query(rs -> {
                boolean opcional = rs.getBoolean("opcional");
                BigDecimal cantidad = rs.getBigDecimal("cantidad");
                ingredientes.add(new IngredienteCuradoResponse(
                    rs.getLong("producto_id"),
                    rs.getString("nombre"),
                    rs.getString("categoria"),
                    rs.getString("unidad_base"),
                    cantidad,
                    opcional,
                    rs.getBoolean("es_principal"),
                    rs.getBoolean("activo")));
                BigDecimal presentacion = rs.getBigDecimal("cantidad_unidad_compra");
                BigDecimal precio = rs.getBigDecimal("precio_unidad_compra");
                if (!opcional && presentacion != null && precio != null && presentacion.signum() > 0) {
                    costo[0] = costo[0].add(cantidad.multiply(precio).divide(presentacion, 6, RoundingMode.HALF_UP));
                }
            });
        return new RecetaCuradaResponse(
            fila.id(), fila.nombre(), fila.descripcion(), fila.tiempo(), fila.porciones(), PasosReceta.dividir(fila.pasos()),
            fila.previa(), ingredientes, fila.momentos(), fila.fuerte(), fila.ninos(), fila.activa(), pesos(costo[0]),
            fila.video());
    }

    private static BigDecimal pesos(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor.setScale(0, RoundingMode.HALF_UP);
    }

    static List<String> momentos(boolean desayuno, boolean almuerzo, boolean cena) {
        List<String> momentos = new ArrayList<>();
        if (desayuno) {
            momentos.add("DESAYUNO");
        }
        if (almuerzo) {
            momentos.add("ALMUERZO");
        }
        if (cena) {
            momentos.add("CENA");
        }
        return momentos;
    }

    /** Lo que se compara entre antes y después para la auditoría. */
    private static Map<String, Object> resumen(RecetaCuradaResponse receta) {
        Map<String, Object> campos = new LinkedHashMap<>();
        campos.put("nombre", receta.nombre());
        campos.put("descripcion", receta.descripcion());
        campos.put("tiempo_minutos", receta.tiempoMinutos());
        campos.put("porciones", receta.porciones());
        campos.put("pasos", receta.pasos());
        campos.put("preparacion_previa", receta.preparacionPrevia());
        campos.put("momentos", receta.momentos());
        campos.put("es_plato_fuerte", receta.platoFuerte());
        campos.put("gusta_a_ninos", receta.gustaANinos());
        List<String> ingredientes = new ArrayList<>();
        for (IngredienteCuradoResponse i : receta.ingredientes()) {
            ingredientes.add(i.nombre() + " " + i.cantidad().stripTrailingZeros().toPlainString() + " " + i.unidadBase()
                + (i.opcional() ? " (opcional)" : "") + (i.principal() ? " (principal)" : ""));
        }
        campos.put("ingredientes", ingredientes);
        return campos;
    }

    private static Map<String, Object> diferencias(Map<String, Object> antes, Map<String, Object> despues) {
        Map<String, Object> cambios = new LinkedHashMap<>();
        Set<String> campos = new LinkedHashSet<>(antes.keySet());
        campos.addAll(despues.keySet());
        for (String campo : campos) {
            Object a = antes.get(campo);
            Object d = despues.get(campo);
            if (!Objects.equals(a, d)) {
                Map<String, Object> detalle = new LinkedHashMap<>();
                detalle.put("antes", a);
                detalle.put("despues", d);
                cambios.put(campo, detalle);
            }
        }
        return cambios;
    }
}
