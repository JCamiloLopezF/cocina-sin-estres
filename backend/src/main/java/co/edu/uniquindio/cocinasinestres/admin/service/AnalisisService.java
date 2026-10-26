package co.edu.uniquindio.cocinasinestres.admin.service;

import co.edu.uniquindio.cocinasinestres.admin.domain.DatosAnalisis;
import co.edu.uniquindio.cocinasinestres.admin.domain.FuenteAnalisis;
import co.edu.uniquindio.cocinasinestres.admin.domain.IndicadoresCalidad;
import co.edu.uniquindio.cocinasinestres.admin.dto.AnalisisResponse;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Análisis del administrador (KAN-54): uso de la app y los indicadores del Plan de Gestión
 * de la Calidad, calculados con los datos reales de la base.
 */
@Service
public class AnalisisService {

    /** Periodo de los eventos: los últimos 30 días (las métricas del plan son mensuales o semanales). */
    public static final int DIAS = 30;
    /** Semanas de planes que cuentan, incluida la actual. */
    public static final int SEMANAS = 8;

    private final FuenteAnalisis fuente;
    private final Clock reloj;

    public AnalisisService(FuenteAnalisis fuente, Clock reloj) {
        this.fuente = fuente;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public AnalisisResponse analizar() {
        LocalDate hoy = LocalDate.now(reloj);
        LocalDate lunes = hoy.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate desdeLunes = lunes.minusWeeks(SEMANAS - 1L);
        DatosAnalisis d = fuente.leer(DIAS, lunes, desdeLunes);
        return new AnalisisResponse(
            hoy.minusDays(DIAS),
            hoy,
            desdeLunes,
            new AnalisisResponse.Uso(
                d.usuarias(), d.hogares(), d.activas7Dias(), d.aperturas(), d.planesEstaSemana(),
                d.preparaciones(), d.preparacionesFuera(), d.usosRescate()),
            IndicadoresCalidad.calcular(d),
            d.masPreparadas(),
            d.masPlaneadas(),
            d.sinUsar());
    }
}
