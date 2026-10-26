package co.edu.uniquindio.cocinasinestres.admin.domain;

import co.edu.uniquindio.cocinasinestres.perfil.domain.CalculadoraPresupuesto;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Calcula los indicadores del Plan de Gestión de la Calidad (secciones 3.3 y 3.4) con las
 * cifras de {@link DatosAnalisis} y dice si cada uno alcanza su meta (KAN-54). Dominio
 * puro: se prueba sin Spring.
 */
public final class IndicadoresCalidad {

    public static final String USUARIO = "Gestión de usuario y autenticación";
    public static final String DESPENSA = "Despensa virtual";
    public static final String PLANIFICADOR = "Planificación semanal con recetario";
    public static final String AVISOS = "Notificaciones y alertas";

    private static final String SIN_DATOS = "Aún no hay datos en el periodo.";
    private static final int REGLAS = 5;

    private IndicadoresCalidad() {
    }

    public static List<Indicador> calcular(DatosAnalisis d) {
        List<Indicador> lista = new ArrayList<>();

        // Proceso 1
        lista.add(porcentajeMayor(USUARIO, "Completitud de perfil del hogar",
            "(Perfiles completos / cuentas de usuaria activas) %", d.perfilesCompletos(), d.usuarias(), 90,
            "Cuentas con rol USUARIA."));
        lista.add(fueraDeLaApp(USUARIO, "Éxito de autenticación", "(Inicios de sesión exitosos / intentos) %",
            "> 95 %", "Lo registra Firebase Authentication: la app no ve los intentos fallidos. Se consulta en la consola de Firebase."));
        lista.add(fueraDeLaApp(USUARIO, "Tiempo de respuesta de login", "Tiempo promedio en milisegundos",
            "< 1500 ms", "Lo mide Firebase Authentication en el navegador; se toma con las herramientas del navegador."));

        // Proceso 2
        lista.add(menor(DESPENSA, "Actualidad del inventario", "Días desde la última actualización de la despensa (promedio por hogar)",
            d.diasDesdeActualizacion(), "días", 7, d.hogaresConDespensa(), "Hogares con algo en la despensa."));
        lista.add(porcentajeMayor(DESPENSA, "Adopción del catálogo predefinido",
            "(Productos desde el catálogo / total registrados) %", d.itemsDelCatalogo(), d.itemsDespensa(), 80,
            "Productos de todas las despensas."));
        lista.add(fueraDeLaApp(DESPENSA, "Exactitud de existencias", "(Descuentos correctos / total) %", "> 98 %",
            "Se verifica con las pruebas automáticas del descuento al preparar (CI)."));

        // Proceso 3
        lista.add(menor(PLANIFICADOR, "Tiempo de planificación semanal",
            "Minutos entre crear el plan y tener almuerzo y cena los 7 días (promedio)",
            d.minutosParaArmar(), "min", 3, d.planesCompletados(), "Planes completados en menos de un día."));
        lista.add(cumplimientoPresupuesto(d.planes()));
        lista.add(porcentajeMayor(PLANIFICADOR, "Aprovechamiento de la despensa",
            "(Productos de la despensa que usa el plan / productos disponibles) %", d.productosUsados(),
            d.productosDisponibles(), 60, "Hogares con plan esta semana."));
        lista.add(seguimiento(PLANIFICADOR, "Uso del modo rescate", "(Semanas con modo rescate / semanas planeadas) %",
            d.semanasConRescate(), d.semanasPlaneadas(), "Métrica de seguimiento (sección 3.3): no tiene meta."));

        // Proceso 4
        lista.add(porcentajeMayor(AVISOS, "Atención de avisos de vencimiento",
            "(Avisos de vencimiento leídos / generados) %", d.avisosVencimientoLeidos(), d.avisosVencimiento(), 70,
            "Cuenta los que la usuaria marcó como leídos."));
        lista.add(menorOIgual(AVISOS, "Carga de avisos por sesión", "Avisos mostrados / aperturas de la app",
            d.avisosPorApertura(), "avisos", 5, d.aperturas()));
        lista.add(menor(AVISOS, "Tiempo de evaluación del motor de reglas", "Milisegundos promedio por apertura (RNF-07)",
            d.msPorApertura(), "ms", 1000, d.aperturas(), "Medido en el servidor."));
        lista.add(porcentajeMinimo(AVISOS, "Cobertura de reglas evaluadas",
            "(Aperturas con las 5 reglas evaluadas / aperturas) % (RNF-04)", d.aperturasCincoReglas(), d.aperturas(),
            "Si una usuaria apaga un tipo de aviso, esa regla no se evalúa."));
        return lista;
    }

    /** % de planes que caben en el presupuesto semanal del hogar (ADR-14). */
    static Indicador cumplimientoPresupuesto(List<DatosAnalisis.PlanConCosto> planes) {
        long dentro = planes.stream()
            .filter(p -> {
                BigDecimal semanal = CalculadoraPresupuesto.presupuestoSemanal(p.presupuestoMonto(), p.periodicidad());
                return semanal.signum() > 0 && p.costo().compareTo(semanal) <= 0;
            })
            .count();
        return porcentajeMayor(PLANIFICADOR, "Cumplimiento del presupuesto",
            "(Planes dentro del presupuesto semanal / total) %", dentro, planes.size(), 85,
            "Planes de las últimas 8 semanas; costo con los precios de referencia.");
    }

    // ------------------------------------------------------------------

    static Double porcentaje(long parte, long total) {
        return total == 0 ? null : redondear(100.0 * parte / total);
    }

    static Double redondear(Double valor) {
        return valor == null ? null : Math.round(valor * 10) / 10.0;
    }

    private static Indicador porcentajeMayor(
        String proceso, String nombre, String formula, long parte, long total, double meta, String nota) {
        Double valor = porcentaje(parte, total);
        return new Indicador(proceso, nombre, formula, valor, "%", "> " + entero(meta) + " %",
            valor == null ? null : valor > meta, total, valor == null ? SIN_DATOS : nota);
    }

    private static Indicador porcentajeMinimo(
        String proceso, String nombre, String formula, long parte, long total, String nota) {
        Double valor = porcentaje(parte, total);
        return new Indicador(proceso, nombre, formula, valor, "%", "100 %",
            valor == null ? null : parte == total, total, valor == null ? SIN_DATOS : nota);
    }

    private static Indicador seguimiento(
        String proceso, String nombre, String formula, long parte, long total, String nota) {
        Double valor = porcentaje(parte, total);
        return new Indicador(proceso, nombre, formula, valor, "%", null, null, total, valor == null ? SIN_DATOS : nota);
    }

    private static Indicador menor(
        String proceso, String nombre, String formula, Double medido, String unidad, double meta, long muestra, String nota) {
        Double valor = muestra == 0 ? null : redondear(medido);
        return new Indicador(proceso, nombre, formula, valor, unidad, "< " + entero(meta) + " " + unidad,
            valor == null ? null : valor < meta, muestra, valor == null ? SIN_DATOS : nota);
    }

    private static Indicador menorOIgual(
        String proceso, String nombre, String formula, Double medido, String unidad, double meta, long muestra) {
        Double valor = muestra == 0 ? null : redondear(medido);
        return new Indicador(proceso, nombre, formula, valor, unidad, "≤ " + entero(meta) + " " + unidad,
            valor == null ? null : valor <= meta, muestra, valor == null ? SIN_DATOS : "Aperturas de la app en el periodo.");
    }

    private static Indicador fueraDeLaApp(String proceso, String nombre, String formula, String meta, String nota) {
        return new Indicador(proceso, nombre, formula, null, null, meta, null, 0, nota);
    }

    private static String entero(double valor) {
        return valor == Math.rint(valor) ? String.valueOf((long) valor) : String.valueOf(valor);
    }
}
