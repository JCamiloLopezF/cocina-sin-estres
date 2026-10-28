package co.edu.uniquindio.cocinasinestres.admin.service;

import co.edu.uniquindio.cocinasinestres.admin.dto.MedicionInicioSesionRequest;
import co.edu.uniquindio.cocinasinestres.admin.dto.MedicionPlanificacionRequest;
import co.edu.uniquindio.cocinasinestres.admin.repository.RegistroMetricasJdbc;
import co.edu.uniquindio.cocinasinestres.comun.error.DatosInvalidosException;
import co.edu.uniquindio.cocinasinestres.perfil.service.UsuarioActualService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Recibe las dos métricas del Plan de Calidad que solo la web puede medir (KAN-54): el
 * inicio de sesión (éxito y tiempo) y el tiempo para dejar la semana completa.
 */
@Service
public class MetricasService {

    public static final String CODIGO_INVALIDA = "METRICA_INVALIDA";

    /** Más que esto no es un inicio de sesión: es una pestaña que quedó abierta. */
    static final int MS_MAXIMO = 120_000;
    static final int FALLIDOS_MAXIMO = 20;
    /** Un plan que se completa en más de un día no mide el tiempo de armarlo. */
    static final int SEGUNDOS_MAXIMO = 86_400;

    private final RegistroMetricasJdbc registro;
    private final UsuarioActualService usuarioActualService;

    public MetricasService(RegistroMetricasJdbc registro, UsuarioActualService usuarioActualService) {
        this.registro = registro;
        this.usuarioActualService = usuarioActualService;
    }

    @Transactional
    public void inicioSesion(MedicionInicioSesionRequest request) {
        if (request == null || request.duracionMs() == null
            || request.duracionMs() < 0 || request.duracionMs() > MS_MAXIMO
            || (request.fallidosPrevios() != null && request.fallidosPrevios() < 0)) {
            throw new DatosInvalidosException(CODIGO_INVALIDA, "La medición del inicio de sesión no es válida.");
        }
        int fallidos = request.fallidosPrevios() == null ? 0 : Math.min(request.fallidosPrevios(), FALLIDOS_MAXIMO);
        registro.inicioSesion(usuarioActualService.obtener().id(), request.duracionMs(), fallidos);
    }

    @Transactional
    public void planificacion(MedicionPlanificacionRequest request) {
        if (request == null || request.semanaInicio() == null || request.segundos() == null || request.segundos() < 1) {
            throw new DatosInvalidosException(CODIGO_INVALIDA, "La medición de la planificación no es válida.");
        }
        if (request.segundos() > SEGUNDOS_MAXIMO) {
            return;
        }
        registro.planificacion(usuarioActualService.obtener().id(), request.semanaInicio(), request.segundos());
    }
}
