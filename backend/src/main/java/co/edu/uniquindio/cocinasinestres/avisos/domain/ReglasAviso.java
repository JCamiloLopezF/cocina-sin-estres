package co.edu.uniquindio.cocinasinestres.avisos.domain;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Las cinco reglas del motor de avisos (ADR-17) y sus textos.
 *
 * <p>Cada regla recibe los datos ya leídos y devuelve como mucho <strong>un</strong>
 * aviso: varios productos en el mismo caso se agrupan ("3 productos vencen pronto").
 * La clave de idempotencia es tipo + hogar + día (o semana), así el mismo aviso no se
 * repite en el día; si la situación cambia antes de que la usuaria lo lea, se actualiza
 * el texto.</p>
 *
 * <p>Los textos van en tono de apoyo, no de alarma (RNF-14): dicen qué pasa y qué se
 * puede hacer, sin signos de admiración ni palabras como "urgente" o "alerta".</p>
 *
 * <p>Sin Spring ni base de datos (ADR-09): se prueba con JUnit (RNF-29).</p>
 */
public final class ReglasAviso {

    /** Un producto "por vencer" para el aviso: vence hoy o en los próximos 3 días (ADR-17). */
    public static final int DIAS_POR_VENCER = 3;

    /** Desde este día se revisa si la semana siguiente tiene plan (ADR-17). */
    public static final DayOfWeek DIA_REVISAR_SEMANA = DayOfWeek.FRIDAY;

    /** Avisos que se muestran como máximo al abrir la aplicación (ADR-17). */
    public static final int MAX_POR_SESION = 5;

    /** Cuántos nombres se escriben antes de decir "y N más". */
    private static final int MAX_NOMBRES = 4;

    private static final Locale ES = Locale.forLanguageTag("es-CO");

    private ReglasAviso() {
    }

    // --- Las cinco reglas ------------------------------------------------

    /** Productos que llegaron a su mínimo (o quedaron por debajo). */
    public static Optional<Aviso> bajoMinimo(Long hogarId, LocalDate hoy, List<ProductoAviso> productos) {
        List<String> nombres = productos.stream().map(ProductoAviso::nombre).filter(Objects::nonNull).distinct().toList();
        if (nombres.isEmpty()) {
            return Optional.empty();
        }
        String titulo;
        String mensaje;
        if (nombres.size() == 1) {
            String nombre = nombres.get(0);
            boolean plural = esPlural(nombre);
            titulo = "Ya casi no " + (plural ? "quedan " : "queda ") + minuscula(nombre);
            mensaje = (plural ? "Llegaron al mínimo que definiste. Tenlos" : "Llegó al mínimo que definiste. Tenlo")
                + " en cuenta en tu próxima compra.";
        } else {
            titulo = nombres.size() + " productos llegaron al mínimo";
            mensaje = mayuscula(enumerar(nombres.stream().map(ReglasAviso::minuscula).toList()))
                + ". Tenlos en cuenta en tu próxima compra.";
        }
        return Optional.of(new Aviso(TipoAviso.BAJO_MINIMO, clave(TipoAviso.BAJO_MINIMO, hogarId, hoy),
            titulo, mensaje, "/despensa?filtro=BAJO_MINIMO", "Ver en la despensa"));
    }

    /** Productos que vencen hoy o en los próximos {@value #DIAS_POR_VENCER} días. */
    public static Optional<Aviso> porVencer(Long hogarId, LocalDate hoy, List<ProductoAviso> productos) {
        List<ProductoAviso> pronto = productos.stream()
            .filter(p -> p.nombre() != null && p.venceEn() != null)
            .filter(p -> !p.venceEn().isBefore(hoy) && !p.venceEn().isAfter(hoy.plusDays(DIAS_POR_VENCER)))
            .sorted(Comparator.comparing(ProductoAviso::venceEn).thenComparing(ProductoAviso::nombre))
            .toList();
        if (pronto.isEmpty()) {
            return Optional.empty();
        }
        String titulo;
        String mensaje;
        if (pronto.size() == 1) {
            ProductoAviso producto = pronto.get(0);
            boolean plural = esPlural(producto.nombre());
            titulo = mayuscula(minuscula(producto.nombre())) + (plural ? " vencen " : " vence ") + cuando(producto.venceEn(), hoy);
            mensaje = (plural ? "Úsalos" : "Úsalo") + " primero en tus comidas para que no se pierda"
                + (plural ? "n." : ".");
        } else {
            titulo = pronto.size() + " productos vencen pronto";
            mensaje = mayuscula(enumerar(pronto.stream()
                    .map(p -> minuscula(p.nombre()) + " (" + cuando(p.venceEn(), hoy) + ")")
                    .toList()))
                + ". Úsalos primero en tus comidas para que no se pierdan.";
        }
        return Optional.of(new Aviso(TipoAviso.POR_VENCER, clave(TipoAviso.POR_VENCER, hogarId, hoy),
            titulo, mensaje, "/despensa?filtro=POR_VENCER", "Ver en la despensa"));
    }

    /**
     * Comidas de mañana que piden alistar algo desde hoy, como remojar los fríjoles.
     *
     * @param pendientes las de mañana que no están preparadas
     */
    public static Optional<Aviso> preparacionPrevia(Long hogarId, LocalDate hoy, List<PreparacionPendiente> pendientes) {
        List<PreparacionPendiente> conIndicacion = pendientes.stream()
            .filter(p -> p.indicacion() != null && !p.indicacion().isBlank())
            .toList();
        if (conIndicacion.isEmpty()) {
            return Optional.empty();
        }
        LocalDate manana = hoy.plusDays(1);
        String titulo;
        String mensaje;
        if (conIndicacion.size() == 1) {
            PreparacionPendiente p = conIndicacion.get(0);
            titulo = "Para mañana: " + p.receta();
            mensaje = conPunto(p.indicacion().trim());
        } else {
            titulo = "Mañana hay " + conIndicacion.size() + " comidas que se alistan desde hoy";
            mensaje = String.join(" ", conIndicacion.stream()
                .map(p -> p.receta() + ": " + conPunto(minusculaInicial(p.indicacion().trim())))
                .toList());
        }
        int dia = manana.getDayOfWeek().getValue();
        String enlace = "/plan/dia/" + dia + (manana.getDayOfWeek() == DayOfWeek.MONDAY ? "?semana=proxima" : "");
        return Optional.of(new Aviso(TipoAviso.PREPARACION_PREVIA, clave(TipoAviso.PREPARACION_PREVIA, hogarId, manana),
            titulo, mensaje, enlace, "Ver el día"));
    }

    /** Desde el viernes se mira si la semana siguiente tiene plan. */
    public static boolean tocaRevisarSemana(LocalDate hoy) {
        return hoy.getDayOfWeek().getValue() >= DIA_REVISAR_SEMANA.getValue();
    }

    /** El lunes de la semana siguiente a {@code hoy}. */
    public static LocalDate lunesSiguiente(LocalDate hoy) {
        return hoy.with(TemporalAdjusters.next(DayOfWeek.MONDAY));
    }

    /**
     * La semana siguiente sin plan, o con el borrador que la app dejó armado (ADR-24).
     *
     * @param estado cómo está la semana siguiente, después de intentar el borrador
     */
    public static Optional<Aviso> semanaSinPlanificar(Long hogarId, LocalDate hoy, EstadoSemana estado) {
        if (!tocaRevisarSemana(hoy) || estado == null || estado.situacion() == EstadoSemana.Situacion.PLANEADA) {
            return Optional.empty();
        }
        String titulo;
        String mensaje;
        String accion;
        if (estado.situacion() == EstadoSemana.Situacion.BORRADOR && estado.comidas() > 0) {
            titulo = "Te armamos la próxima semana";
            mensaje = "Es un borrador con " + estado.comidas() + (estado.comidas() == 1 ? " comida" : " comidas")
                + " que cuida las alergias y el presupuesto de tu hogar. Revísalo y cámbialo a tu gusto.";
            accion = "Revisar la semana";
        } else {
            titulo = "La próxima semana todavía no tiene plan";
            mensaje = "Con «Armar mi semana» queda lista en un minuto, y la puedes cambiar como quieras.";
            accion = "Armar la semana";
        }
        return Optional.of(new Aviso(TipoAviso.SEMANA_SIN_PLANIFICAR,
            clave(TipoAviso.SEMANA_SIN_PLANIFICAR, hogarId, lunesSiguiente(hoy)),
            titulo, mensaje, "/plan?semana=proxima", accion));
    }

    /** Una receta que salva lo que está por vencer, sin comprar nada. */
    public static Optional<Aviso> aprovechar(Long hogarId, LocalDate hoy, Aprovechamiento sugerencia) {
        if (sugerencia == null || sugerencia.productos().isEmpty()) {
            return Optional.empty();
        }
        List<ProductoAviso> productos = sugerencia.productos();
        String mensaje;
        if (productos.size() == 1) {
            ProductoAviso p = productos.get(0);
            boolean plural = esPlural(p.nombre());
            mensaje = "Usa " + minuscula(p.nombre())
                + (p.venceEn() == null ? "" : ", que " + (plural ? "vencen " : "vence ") + cuando(p.venceEn(), hoy) + ",")
                + " y no tienes que comprar nada.";
        } else {
            mensaje = "Usa " + enumerar(productos.stream().map(p -> minuscula(p.nombre())).toList())
                + " antes de que se dañen. No tienes que comprar nada.";
        }
        return Optional.of(new Aviso(TipoAviso.APROVECHAR_PRODUCTO, clave(TipoAviso.APROVECHAR_PRODUCTO, hogarId, hoy),
            "Hoy puedes hacer " + sugerencia.receta(), mensaje, "/recetas/" + sugerencia.recetaId(), "Ver la receta"));
    }

    /** Los avisos que se muestran: en el orden de ADR-17 y como máximo {@value #MAX_POR_SESION}. */
    public static List<Aviso> seleccionar(Collection<Aviso> avisos) {
        return avisos.stream()
            .sorted(Comparator.comparing(Aviso::tipo))
            .limit(MAX_POR_SESION)
            .toList();
    }

    // --- Textos ----------------------------------------------------------

    /** "TIPO:hogar:fecha". Cabe en {@code aviso.clave_idempotencia} (120). */
    static String clave(TipoAviso tipo, Long hogarId, Object referencia) {
        return tipo.name() + ":" + hogarId + ":" + referencia;
    }

    /** "hoy", "mañana" o "en 3 días". */
    static String cuando(LocalDate fecha, LocalDate hoy) {
        long dias = ChronoUnit.DAYS.between(hoy, fecha);
        if (dias <= 0) {
            return "hoy";
        }
        return dias == 1 ? "mañana" : "en " + dias + " días";
    }

    /** "a", "a y b", "a, b y c"; desde el quinto, "a, b, c, d y 2 más". */
    static String enumerar(List<String> nombres) {
        if (nombres.isEmpty()) {
            return "";
        }
        if (nombres.size() > MAX_NOMBRES) {
            int resto = nombres.size() - MAX_NOMBRES;
            return String.join(", ", nombres.subList(0, MAX_NOMBRES)) + " y " + resto + " más";
        }
        if (nombres.size() == 1) {
            return nombres.get(0);
        }
        return String.join(", ", nombres.subList(0, nombres.size() - 1)) + " y " + nombres.get(nombres.size() - 1);
    }

    /** El verbo va en plural si la primera palabra termina en s ("Huevos", "Fríjoles"). */
    static boolean esPlural(String nombre) {
        String primera = nombre.trim().split("\\s+")[0].toLowerCase(ES);
        return primera.length() > 3 && primera.endsWith("s");
    }

    private static String minuscula(String texto) {
        return texto.trim().toLowerCase(ES);
    }

    private static String minusculaInicial(String texto) {
        return texto.isEmpty() ? texto : texto.substring(0, 1).toLowerCase(ES) + texto.substring(1);
    }

    private static String mayuscula(String texto) {
        return texto.isEmpty() ? texto : texto.substring(0, 1).toUpperCase(ES) + texto.substring(1);
    }

    private static String conPunto(String texto) {
        return texto.endsWith(".") ? texto : texto + ".";
    }
}
