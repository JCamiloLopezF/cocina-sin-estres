package co.edu.uniquindio.cocinasinestres.admin.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.uniquindio.cocinasinestres.admin.dto.MedicionInicioSesionRequest;
import co.edu.uniquindio.cocinasinestres.admin.dto.MedicionPlanificacionRequest;
import co.edu.uniquindio.cocinasinestres.admin.repository.RegistroMetricasJdbc;
import co.edu.uniquindio.cocinasinestres.comun.error.DatosInvalidosException;
import co.edu.uniquindio.cocinasinestres.perfil.domain.Rol;
import co.edu.uniquindio.cocinasinestres.perfil.domain.UsuarioAutenticado;
import co.edu.uniquindio.cocinasinestres.perfil.service.UsuarioActualService;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Lo que la web manda para el Plan de Calidad (KAN-54), con dobles. */
@DisplayName("MetricasService")
class MetricasServiceTest {

    private static final LocalDate LUNES = LocalDate.of(2026, 10, 26);

    private RegistroMetricasJdbc registro;
    private MetricasService servicio;

    @BeforeEach
    void preparar() {
        registro = mock(RegistroMetricasJdbc.class);
        UsuarioActualService usuarioActualService = mock(UsuarioActualService.class);
        when(usuarioActualService.obtener()).thenReturn(new UsuarioAutenticado(7L, "uid", "a@b.co", Rol.USUARIA, true));
        servicio = new MetricasService(registro, usuarioActualService);
    }

    @Test
    void anotaElInicioDeSesionDeLaCuentaActual() {
        servicio.inicioSesion(new MedicionInicioSesionRequest(850, 2));

        verify(registro).inicioSesion(7L, 850, 2);
    }

    @Test
    void sinFallidosCuentaCeroYDemasiadosSeTopan() {
        servicio.inicioSesion(new MedicionInicioSesionRequest(500, null));
        servicio.inicioSesion(new MedicionInicioSesionRequest(500, 300));

        verify(registro).inicioSesion(7L, 500, 0);
        verify(registro).inicioSesion(7L, 500, MetricasService.FALLIDOS_MAXIMO);
    }

    @Test
    void rechazaUnInicioDeSesionImposible() {
        assertThatThrownBy(() -> servicio.inicioSesion(new MedicionInicioSesionRequest(-1, 0)))
            .isInstanceOf(DatosInvalidosException.class);
        assertThatThrownBy(() -> servicio.inicioSesion(new MedicionInicioSesionRequest(MetricasService.MS_MAXIMO + 1, 0)))
            .isInstanceOf(DatosInvalidosException.class);
        assertThatThrownBy(() -> servicio.inicioSesion(new MedicionInicioSesionRequest(null, 0)))
            .isInstanceOf(DatosInvalidosException.class);
        verify(registro, never()).inicioSesion(anyLong(), anyInt(), anyInt());
    }

    @Test
    void anotaElTiempoDePlanificacion() {
        servicio.planificacion(new MedicionPlanificacionRequest(LUNES, 140));

        verify(registro).planificacion(7L, LUNES, 140);
    }

    @Test
    void unPlanDeMasDeUnDiaNoMideElTiempoDeArmarloYSeIgnora() {
        servicio.planificacion(new MedicionPlanificacionRequest(LUNES, MetricasService.SEGUNDOS_MAXIMO + 1));

        verify(registro, never()).planificacion(anyLong(), org.mockito.ArgumentMatchers.any(), anyInt());
    }

    @Test
    void rechazaUnaPlanificacionSinSemanaOSinTiempo() {
        assertThatThrownBy(() -> servicio.planificacion(new MedicionPlanificacionRequest(null, 60)))
            .isInstanceOf(DatosInvalidosException.class);
        assertThatThrownBy(() -> servicio.planificacion(new MedicionPlanificacionRequest(LUNES, 0)))
            .isInstanceOf(DatosInvalidosException.class);
    }
}
