package co.edu.uniquindio.cocinasinestres.admin.domain;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.uniquindio.cocinasinestres.admin.domain.DatosAnalisis.PlanConCosto;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Indicadores del Plan de Gestión de la Calidad (KAN-54)")
class IndicadoresCalidadTest {

    private static DatosAnalisis datos(List<PlanConCosto> planes) {
        return new DatosAnalisis(
            10, 9, 8, 6,
            40, 2.5, 120.0, 38,
            5, 30, 6, 4,
            7, 3.2, 100, 92,
            4, 2.4, planes,
            50, 35, 10, 3, 20, 15,
            List.of(new DatosAnalisis.RecetaUsada("Huevos pericos", 7)), List.of(), List.of("Tomate de árbol en salsa"));
    }

    private static Map<String, Indicador> porNombre(List<Indicador> indicadores) {
        return indicadores.stream().collect(Collectors.toMap(Indicador::nombre, Function.identity()));
    }

    @Test
    @DisplayName("calcula cada indicador y dice si alcanza la meta del plan")
    void calcula() {
        List<PlanConCosto> planes = List.of(
            new PlanConCosto(new BigDecimal("150000"), new BigDecimal("195000"), "SEMANAL"),
            new PlanConCosto(new BigDecimal("250000"), new BigDecimal("195000"), "SEMANAL"));
        Map<String, Indicador> i = porNombre(IndicadoresCalidad.calcular(datos(planes)));

        assertThat(i.get("Completitud de perfil del hogar").valor()).isEqualTo(90.0);
        // "Superior al 90 %": exactamente 90 no la alcanza.
        assertThat(i.get("Completitud de perfil del hogar").cumple()).isFalse();
        assertThat(i.get("Actualidad del inventario").valor()).isEqualTo(3.2);
        assertThat(i.get("Actualidad del inventario").cumple()).isTrue();
        assertThat(i.get("Adopción del catálogo predefinido").valor()).isEqualTo(92.0);
        assertThat(i.get("Tiempo de planificación semanal").cumple()).isTrue();
        assertThat(i.get("Cumplimiento del presupuesto").valor()).isEqualTo(50.0);
        assertThat(i.get("Cumplimiento del presupuesto").cumple()).isFalse();
        assertThat(i.get("Aprovechamiento de la despensa").valor()).isEqualTo(70.0);
        assertThat(i.get("Uso del modo rescate").meta()).isNull();
        assertThat(i.get("Uso del modo rescate").valor()).isEqualTo(30.0);
        assertThat(i.get("Atención de avisos de vencimiento").valor()).isEqualTo(75.0);
        assertThat(i.get("Carga de avisos por sesión").cumple()).isTrue();
        assertThat(i.get("Tiempo de evaluación del motor de reglas").valor()).isEqualTo(120.0);
        assertThat(i.get("Cobertura de reglas evaluadas").valor()).isEqualTo(95.0);
        assertThat(i.get("Cobertura de reglas evaluadas").cumple()).isFalse();
        // Sin inicios de sesión medidos por la web: sin dato, no "cumple".
        assertThat(i.get("Éxito de autenticación").valor()).isNull();
        assertThat(i.get("Éxito de autenticación").cumple()).isNull();
    }

    @Test
    @DisplayName("el inicio de sesión se mide con lo que anota la web: éxito y milisegundos")
    void inicioDeSesion() {
        Map<String, Indicador> i = porNombre(IndicadoresCalidad.calcular(datos(List.of()), new DatosLogin(19, 1, 820.4)));

        // 19 de 20 intentos: 95 % justo no supera la meta "> 95 %".
        assertThat(i.get("Éxito de autenticación").valor()).isEqualTo(95.0);
        assertThat(i.get("Éxito de autenticación").cumple()).isFalse();
        assertThat(i.get("Éxito de autenticación").muestra()).isEqualTo(20);
        assertThat(i.get("Tiempo de respuesta de login").valor()).isEqualTo(820.4);
        assertThat(i.get("Tiempo de respuesta de login").cumple()).isTrue();
    }

    @Test
    @DisplayName("sin datos no inventa un valor ni dice que se cumple")
    void sinDatos() {
        DatosAnalisis vacio = new DatosAnalisis(
            0, 0, 0, 0,
            0, null, null, 0,
            0, 0, 0, 0,
            0, null, 0, 0,
            0, null, List.of(),
            0, 0, 0, 0, 0, 0,
            List.of(), List.of(), List.of());

        assertThat(IndicadoresCalidad.calcular(vacio)).allSatisfy(indicador -> {
            assertThat(indicador.valor()).isNull();
            assertThat(indicador.cumple()).isNull();
        });
    }

    @Test
    @DisplayName("el presupuesto mensual se prorratea a la semana antes de comparar")
    void presupuestoMensual() {
        // $800.000 al mes son unos $184.000 a la semana (ADR-14): un plan de $150.000 cabe.
        Indicador presupuesto = IndicadoresCalidad.cumplimientoPresupuesto(List.of(
            new PlanConCosto(new BigDecimal("150000"), new BigDecimal("800000"), "MENSUAL")));

        assertThat(presupuesto.valor()).isEqualTo(100.0);
        assertThat(presupuesto.cumple()).isTrue();
    }
}
