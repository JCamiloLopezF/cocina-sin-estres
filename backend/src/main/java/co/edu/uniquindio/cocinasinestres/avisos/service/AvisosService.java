package co.edu.uniquindio.cocinasinestres.avisos.service;

import co.edu.uniquindio.cocinasinestres.avisos.domain.AlmacenAvisos;
import co.edu.uniquindio.cocinasinestres.avisos.domain.AlmacenAvisos.AvisoGuardado;
import co.edu.uniquindio.cocinasinestres.avisos.domain.Aprovechamiento;
import co.edu.uniquindio.cocinasinestres.avisos.domain.Aviso;
import co.edu.uniquindio.cocinasinestres.avisos.domain.EstadoSemana;
import co.edu.uniquindio.cocinasinestres.avisos.domain.FuenteAvisos;
import co.edu.uniquindio.cocinasinestres.avisos.domain.ProductoAviso;
import co.edu.uniquindio.cocinasinestres.avisos.domain.ReglasAviso;
import co.edu.uniquindio.cocinasinestres.avisos.domain.TipoAviso;
import co.edu.uniquindio.cocinasinestres.avisos.dto.AvisoResponse;
import co.edu.uniquindio.cocinasinestres.avisos.dto.PreferenciaAvisoResponse;
import co.edu.uniquindio.cocinasinestres.comun.error.ConflictoException;
import co.edu.uniquindio.cocinasinestres.comun.error.DatosInvalidosException;
import co.edu.uniquindio.cocinasinestres.comun.error.RecursoNoEncontradoException;
import co.edu.uniquindio.cocinasinestres.perfil.service.UsuarioActualService;
import co.edu.uniquindio.cocinasinestres.planificador.dto.RescateResponse;
import co.edu.uniquindio.cocinasinestres.planificador.service.BorradorSemanaService;
import co.edu.uniquindio.cocinasinestres.planificador.service.RescateService;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Motor de avisos (ADR-17, KAN-27). Se evalúa cuando la aplicación pide
 * {@code GET /api/avisos} al abrirse: corre las cinco reglas que la usuaria tiene
 * encendidas, guarda los avisos sin repetirlos y devuelve como máximo cinco sin leer.
 *
 * <p>Además, desde el viernes deja armado un borrador de la semana siguiente si llega
 * sin plan (ADR-24, pieza 3).</p>
 *
 * <p>No es transaccional a propósito: cada aviso se guarda por su cuenta, y el borrador y
 * el modo rescate abren su propia transacción. Si uno falla, los demás avisos igual
 * llegan.</p>
 */
@Service
public class AvisosService {

    private static final Logger log = LoggerFactory.getLogger(AvisosService.class);

    /** El centro de avisos muestra lo de los últimos 30 días. */
    private static final int DIAS_CENTRO = 30;
    private static final int MAX_CENTRO = 40;

    private final UsuarioActualService usuarioActualService;
    private final FuenteAvisos fuente;
    private final AlmacenAvisos almacen;
    private final BorradorSemanaService borradores;
    private final RescateService rescateService;
    private final Clock reloj;

    public AvisosService(
        UsuarioActualService usuarioActualService,
        FuenteAvisos fuente,
        AlmacenAvisos almacen,
        BorradorSemanaService borradores,
        RescateService rescateService,
        Clock reloj) {
        this.usuarioActualService = usuarioActualService;
        this.fuente = fuente;
        this.almacen = almacen;
        this.borradores = borradores;
        this.rescateService = rescateService;
        this.reloj = reloj;
    }

    /**
     * Corre las reglas y devuelve los avisos sin leer de hoy, como máximo
     * {@value ReglasAviso#MAX_POR_SESION}. Sin perfil del hogar no hay avisos.
     */
    public List<AvisoResponse> evaluar() {
        long inicio = System.nanoTime();
        Optional<Long> hogar = fuente.hogarDe(usuarioActualService.obtener().id());
        if (hogar.isEmpty()) {
            return List.of();
        }
        Long hogarId = hogar.get();
        LocalDate hoy = LocalDate.now(reloj);
        Set<TipoAviso> apagados = fuente.apagados(hogarId);

        List<Aviso> avisos = new ArrayList<>();
        if (!apagados.contains(TipoAviso.BAJO_MINIMO)) {
            ReglasAviso.bajoMinimo(hogarId, hoy, fuente.bajoMinimo(hogarId)).ifPresent(avisos::add);
        }
        if (!apagados.contains(TipoAviso.POR_VENCER)) {
            ReglasAviso.porVencer(hogarId, hoy,
                fuente.porVencer(hogarId, hoy.minusDays(ReglasAviso.DIAS_VENCIDO),
                    hoy.plusDays(ReglasAviso.DIAS_POR_VENCER))).ifPresent(avisos::add);
        }
        if (!apagados.contains(TipoAviso.PREPARACION_PREVIA)) {
            ReglasAviso.preparacionPrevia(hogarId, hoy, fuente.preparacionesPara(hogarId, hoy.plusDays(1)))
                .ifPresent(avisos::add);
        }
        if (!apagados.contains(TipoAviso.SEMANA_SIN_PLANIFICAR) && ReglasAviso.tocaRevisarSemana(hoy)) {
            ReglasAviso.semanaSinPlanificar(hogarId, hoy, semanaSiguiente(hogarId, hoy)).ifPresent(avisos::add);
        }
        if (!apagados.contains(TipoAviso.APROVECHAR_PRODUCTO)) {
            aprovechamiento().flatMap(a -> ReglasAviso.aprovechar(hogarId, hoy, a)).ifPresent(avisos::add);
        }

        avisos.forEach(aviso -> almacen.guardar(hogarId, aviso));
        List<AvisoResponse> mostrar = almacen.sinLeer(hogarId, avisos.stream().map(Aviso::clave).toList()).stream()
            .limit(ReglasAviso.MAX_POR_SESION)
            .map(AvisosService::aRespuesta)
            .toList();

        // Métricas del Plan de Calidad: avisos por sesión, reglas evaluadas (RNF-04) y tiempo
        // del motor (RNF-07). Quedan en apertura_app para el análisis del administrador (KAN-54).
        long ms = (System.nanoTime() - inicio) / 1_000_000;
        int reglas = TipoAviso.values().length - apagados.size();
        almacen.registrarApertura(hogarId, avisos.size(), mostrar.size(), reglas, ms);
        log.info("Avisos evaluados: hogarId={} vigentes={} mostrados={} reglas={} ms={}",
            hogarId, avisos.size(), mostrar.size(), reglas, ms);
        return mostrar;
    }

    /** Centro de avisos: lo de los últimos {@value #DIAS_CENTRO} días, leído o no. */
    public List<AvisoResponse> recientes() {
        Long hogarId = hogarObligatorio();
        return almacen.desde(hogarId, LocalDateTime.now(reloj).minusDays(DIAS_CENTRO), MAX_CENTRO).stream()
            .map(AvisosService::aRespuesta)
            .toList();
    }

    /** 404 si el aviso no existe o es de otro hogar (RNF-22). */
    public void marcarLeido(Long avisoId) {
        if (!almacen.marcarLeido(hogarObligatorio(), avisoId)) {
            throw new RecursoNoEncontradoException("RECURSO_NO_ENCONTRADO", "Aviso no encontrado.");
        }
    }

    public void marcarTodosLeidos() {
        almacen.marcarTodosLeidos(hogarObligatorio());
    }

    public List<PreferenciaAvisoResponse> preferencias() {
        return preferenciasDe(hogarObligatorio());
    }

    public List<PreferenciaAvisoResponse> cambiarPreferencia(String tipo, Boolean activo) {
        TipoAviso elegido = TipoAviso.desde(tipo);
        if (elegido == null || activo == null) {
            throw new DatosInvalidosException("AVISO_PREFERENCIA_INVALIDA", "Indica el tipo de aviso y si lo quieres recibir.");
        }
        Long hogarId = hogarObligatorio();
        almacen.cambiarPreferencia(hogarId, elegido, activo);
        log.info("Preferencia de aviso: hogarId={} tipo={} activo={}", hogarId, elegido, activo);
        return preferenciasDe(hogarId);
    }

    // ------------------------------------------------------------------

    /** Si la semana siguiente no tiene comidas, intenta dejarle un borrador. */
    private EstadoSemana semanaSiguiente(Long hogarId, LocalDate hoy) {
        LocalDate lunes = ReglasAviso.lunesSiguiente(hoy);
        EstadoSemana estado = fuente.semana(hogarId, lunes);
        if (!estado.sinComidas()) {
            return estado;
        }
        try {
            if (borradores.armar(lunes).isPresent()) {
                return fuente.semana(hogarId, lunes);
            }
        } catch (RuntimeException e) {
            // Sin borrador el aviso igual sale, invitando a armar la semana.
            log.warn("No se pudo armar el borrador: hogarId={} lunes={} motivo={}", hogarId, lunes, e.getMessage());
        }
        return estado;
    }

    /** La primera opción del modo rescate que salva algo que vence en los próximos días. */
    private Optional<Aprovechamiento> aprovechamiento() {
        try {
            RescateResponse rescate = rescateService.opciones(null);
            return rescate.opciones().stream()
                .filter(o -> o.aprovecha().stream().anyMatch(p -> p.dias() <= ReglasAviso.DIAS_POR_VENCER))
                .findFirst()
                .map(o -> new Aprovechamiento(o.recetaId(), o.nombre(), o.aprovecha().stream()
                    .filter(p -> p.dias() <= ReglasAviso.DIAS_POR_VENCER)
                    .map(p -> new ProductoAviso(p.productoId(), p.nombre(), p.venceEn()))
                    .toList()));
        } catch (RuntimeException e) {
            log.warn("No se pudo calcular el aviso de aprovechar: motivo={}", e.getMessage());
            return Optional.empty();
        }
    }

    private List<PreferenciaAvisoResponse> preferenciasDe(Long hogarId) {
        Set<TipoAviso> apagados = fuente.apagados(hogarId);
        return Arrays.stream(TipoAviso.values())
            .map(t -> new PreferenciaAvisoResponse(t.name(), t.titulo(), t.descripcion(), !apagados.contains(t)))
            .toList();
    }

    private Long hogarObligatorio() {
        return fuente.hogarDe(usuarioActualService.obtener().id())
            .orElseThrow(() -> new ConflictoException(
                "AVISOS_SIN_PERFIL_HOGAR", "Completa el perfil de tu hogar para recibir avisos."));
    }

    private static AvisoResponse aRespuesta(AvisoGuardado a) {
        return new AvisoResponse(
            a.id(), a.tipo().name(), a.titulo(), a.mensaje(), a.enlace(), a.accion(), a.leido(), a.creadoEn());
    }
}
