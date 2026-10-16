package co.edu.uniquindio.cocinasinestres.perfil.service;

import co.edu.uniquindio.cocinasinestres.comun.error.DatosInvalidosException;
import co.edu.uniquindio.cocinasinestres.comun.seguridad.CifradorDatosSensibles;
import co.edu.uniquindio.cocinasinestres.perfil.domain.PeriodicidadIngreso;
import co.edu.uniquindio.cocinasinestres.perfil.domain.UsuarioAutenticado;
import co.edu.uniquindio.cocinasinestres.perfil.dto.PerfilHogarRequest;
import co.edu.uniquindio.cocinasinestres.perfil.dto.PerfilHogarResponse;
import co.edu.uniquindio.cocinasinestres.perfil.dto.PersonaRequest;
import co.edu.uniquindio.cocinasinestres.perfil.dto.PersonaResponse;
import co.edu.uniquindio.cocinasinestres.perfil.model.Hogar;
import co.edu.uniquindio.cocinasinestres.perfil.model.Persona;
import co.edu.uniquindio.cocinasinestres.perfil.model.Usuario;
import co.edu.uniquindio.cocinasinestres.perfil.repository.HogarRepository;
import co.edu.uniquindio.cocinasinestres.perfil.repository.PersonaRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HogarService {

    private static final String ERROR_DATOS_INVALIDOS =
        "PERFIL_HOGAR_DATOS_INVALIDOS";

    private final HogarRepository hogarRepository;
    private final PersonaRepository personaRepository;
    private final UsuarioActualService usuarioActualService;
    private final CifradorDatosSensibles cifrador;
    private final ObjectMapper objectMapper;
    private final EntityManager entityManager;

    public HogarService(
        HogarRepository hogarRepository,
        PersonaRepository personaRepository,
        UsuarioActualService usuarioActualService,
        CifradorDatosSensibles cifrador,
        ObjectMapper objectMapper,
        EntityManager entityManager) {

        this.hogarRepository = hogarRepository;
        this.personaRepository = personaRepository;
        this.usuarioActualService = usuarioActualService;
        this.cifrador = cifrador;
        this.objectMapper = objectMapper;
        this.entityManager = entityManager;
    }

    /**
     * Crea o actualiza el perfil del hogar de la persona autenticada.
     */
    @Transactional
    public PerfilHogarResponse guardar(PerfilHogarRequest request) {
        Objects.requireNonNull(request, "La solicitud es obligatoria.");

        validar(request);

        UsuarioAutenticado usuarioActual = usuarioActualService.obtener();

        // Obtenemos la referencia del usuario para la relación (evita select completo).
        Usuario usuario = entityManager.getReference(Usuario.class, usuarioActual.id());

        Hogar hogar = hogarRepository
            .findByUsuario_Id(usuarioActual.id())
            .orElseGet(() -> new Hogar(
                usuario,
                request.presupuestoMonto(),
                request.periodicidadIngreso().name(),
                request.diaPago1(),
                request.diaPago2()));

        hogar.setPresupuestoMonto(request.presupuestoMonto());
        hogar.setPeriodicidadIngreso(request.periodicidadIngreso().name());
        hogar.setDiaPago1(request.diaPago1());
        hogar.setDiaPago2(request.diaPago2());

        hogar.setDiasSinTiempo(serializar(request.diasSinTiempo()));

        guardarDatosSensibles(hogar, request);

        hogar = hogarRepository.save(hogar);

        // Reemplazamos las personas del perfil en la misma transacción.
        List<Persona> personasAnteriores =
            personaRepository.findByHogar_IdOrderByIdAsc(hogar.getId());

        personaRepository.deleteAll(personasAnteriores);

        final Hogar hogarGuardado = hogar;

        List<Persona> personasNuevas = request.personas().stream()
            .map(persona -> new Persona(
                hogarGuardado,
                limpiarNombre(persona.nombre()),
                persona.edad()))
            .toList();

        List<Persona> personasGuardadas =
            personaRepository.saveAll(personasNuevas);

        boolean completo = perfilCompleto(request);

        usuarioActualService.actualizarPerfilCompleto(
            usuarioActual.id(), completo);

        return construirRespuesta(hogar, personasGuardadas);
    }

    /**
     * Obtiene exclusivamente el hogar de la persona autenticada.
     */
    @Transactional(readOnly = true)
    public PerfilHogarResponse obtener() {
        UsuarioAutenticado usuarioActual = usuarioActualService.obtener();

        Hogar hogar = hogarRepository
            .findByUsuario_Id(usuarioActual.id())
            .orElseThrow(() -> new DatosInvalidosException(
                ERROR_DATOS_INVALIDOS,
                "Todavía no existe un perfil del hogar."));

        List<Persona> personas =
            personaRepository.findByHogar_IdOrderByIdAsc(hogar.getId());

        return construirRespuesta(hogar, personas);
    }

    private void guardarDatosSensibles(Hogar hogar, PerfilHogarRequest request) {

        if (request.sinRestricciones()
            && request.restricciones() != null
            && !request.restricciones().isEmpty()) {
            throw new DatosInvalidosException(
                ERROR_DATOS_INVALIDOS,
                "No puedes declarar que no hay restricciones y enviar restricciones a la vez.");
        }

        boolean tieneRestricciones = request.restricciones() != null
            && !request.restricciones().isEmpty();

        boolean tieneCondiciones = request.otrasCondiciones() != null
            && !request.otrasCondiciones().isBlank();

        if (!request.consentimientoDatos()
            && (tieneRestricciones || tieneCondiciones)) {
            throw new DatosInvalidosException(
                ERROR_DATOS_INVALIDOS,
                "Debes consentir el tratamiento de los datos sensibles para registrarlos.");
        }

        hogar.setSinRestricciones(request.sinRestricciones());

        if (request.consentimientoDatos()) {
            if (hogar.getConsentimientoDatosEn() == null) {
                hogar.registrarConsentimiento(LocalDateTime.now(ZoneOffset.UTC));
            }

            hogar.setRestricciones(
                request.sinRestricciones() || !tieneRestricciones
                    ? null
                    : cifrador.cifrar(serializar(request.restricciones())));

            hogar.setOtrasCondiciones(
                tieneCondiciones
                    ? cifrador.cifrar(request.otrasCondiciones().trim())
                    : null);
        } else {
            // Si se revoca el consentimiento, no conservamos esos datos.
            hogar.setRestricciones(null);
            hogar.setOtrasCondiciones(null);
            hogar.revocarConsentimiento();
        }
    }

    private void validar(PerfilHogarRequest request) {
        BigDecimal presupuesto = request.presupuestoMonto();

        if (presupuesto == null || presupuesto.signum() <= 0) {
            throw new DatosInvalidosException(
                ERROR_DATOS_INVALIDOS,
                "El presupuesto debe ser mayor que cero.");
        }

        PeriodicidadIngreso periodicidad = request.periodicidadIngreso();

        if (periodicidad == null) {
            throw new DatosInvalidosException(
                ERROR_DATOS_INVALIDOS,
                "Debes indicar la periodicidad del ingreso.");
        }

        Integer dia1 = request.diaPago1();
        Integer dia2 = request.diaPago2();

        if (dia1 == null) {
            throw new DatosInvalidosException(
                ERROR_DATOS_INVALIDOS,
                "Debes indicar el día de pago.");
        }

        switch (periodicidad) {
            case SEMANAL -> {
                validarRango(dia1, 1, 7, "El día semanal debe estar entre 1 y 7.");
                if (dia2 != null) {
                    throw new DatosInvalidosException(
                        ERROR_DATOS_INVALIDOS,
                        "El ingreso semanal solo tiene un día de pago.");
                }
            }
            case MENSUAL -> {
                validarRango(dia1, 1, 31, "El día mensual debe estar entre 1 y 31.");
                if (dia2 != null) {
                    throw new DatosInvalidosException(
                        ERROR_DATOS_INVALIDOS,
                        "El ingreso mensual solo tiene un día de pago.");
                }
            }
            case QUINCENAL -> {
                validarRango(dia1, 1, 31, "El primer día debe estar entre 1 y 31.");
                validarRango(dia2, 1, 31, "El segundo día debe estar entre 1 y 31.");

                if (dia1 >= dia2) {
                    throw new DatosInvalidosException(
                        ERROR_DATOS_INVALIDOS,
                        "El primer día de pago debe ser menor que el segundo.");
                }
            }
        }

        if (request.personas() == null || request.personas().isEmpty()) {
            throw new DatosInvalidosException(
                ERROR_DATOS_INVALIDOS,
                "Debes registrar al menos una persona en el hogar.");
        }

        for (PersonaRequest persona : request.personas()) {
            if (persona == null || persona.edad() == null
                || persona.edad() < 0 || persona.edad() > 120) {
                throw new DatosInvalidosException(
                    ERROR_DATOS_INVALIDOS,
                    "Cada persona debe tener una edad entre 0 y 120 años.");
            }

            if (persona.nombre() != null
                && persona.nombre().trim().length() > 60) {
                throw new DatosInvalidosException(
                    ERROR_DATOS_INVALIDOS,
                    "El nombre de cada persona admite máximo 60 caracteres.");
            }
        }

        List<Integer> dias = request.diasSinTiempo();
        if (dias != null) {
            if (dias.stream().anyMatch(
                dia -> dia == null || dia < 1 || dia > 7)
                || new HashSet<>(dias).size() != dias.size()) {
                throw new DatosInvalidosException(
                    ERROR_DATOS_INVALIDOS,
                    "Los días sin tiempo deben ser únicos y estar entre 1 y 7.");
            }
        }

        List<String> restricciones = request.restricciones();
        if (restricciones != null) {
            if (restricciones.stream().anyMatch(
                codigo -> codigo == null || codigo.isBlank()
                    || codigo.trim().length() > 40)
                || new HashSet<>(restricciones).size() != restricciones.size()) {
                throw new DatosInvalidosException(
                    ERROR_DATOS_INVALIDOS,
                    "Los códigos de restricción deben ser válidos y no repetirse.");
            }
        }
    }

    private void validarRango(Integer valor, int minimo, int maximo, String mensaje) {
        if (valor == null || valor < minimo || valor > maximo) {
            throw new DatosInvalidosException(ERROR_DATOS_INVALIDOS, mensaje);
        }
    }

    private boolean perfilCompleto(PerfilHogarRequest request) {
        boolean respondioRestricciones = request.sinRestricciones()
            || (request.consentimientoDatos()
            && request.restricciones() != null
            && !request.restricciones().isEmpty());

        return request.presupuestoMonto() != null
            && request.periodicidadIngreso() != null
            && request.diaPago1() != null
            && request.personas() != null
            && !request.personas().isEmpty()
            && respondioRestricciones;
    }

    private PerfilHogarResponse construirRespuesta(Hogar hogar, List<Persona> personas) {

        List<Integer> dias = deserializar(
            hogar.getDiasSinTiempo(),
            new TypeReference<List<Integer>>() {});

        String restriccionesCifradas = hogar.getRestricciones();

        List<String> restricciones =
            restriccionesCifradas == null
                ? List.of()
                : deserializar(
                cifrador.descifrar(restriccionesCifradas),
                new TypeReference<List<String>>() {});

        String condicionesCifradas = hogar.getOtrasCondiciones();

        String condiciones = condicionesCifradas == null
            ? null
            : cifrador.descifrar(condicionesCifradas);

        List<PersonaResponse> personasRespuesta = personas.stream()
            .map(persona -> new PersonaResponse(
                persona.getId(),
                persona.getNombre(),
                persona.getEdad()))
            .toList();

        return new PerfilHogarResponse(
            hogar.getId(),
            hogar.getPresupuestoMonto(),
            PeriodicidadIngreso.valueOf(hogar.getPeriodicidadIngreso()),
            hogar.getDiaPago1(),
            hogar.getDiaPago2(),
            dias,
            restricciones,
            hogar.isSinRestricciones(),
            condiciones,
            hogar.getConsentimientoDatosEn(),
            personasRespuesta);
    }

    private String limpiarNombre(String nombre) {
        return nombre == null || nombre.isBlank()
            ? null
            : nombre.trim();
    }

    private String serializar(Object valor) {
        if (valor == null) {
            return null;
        }

        try {
            return objectMapper.writeValueAsString(valor);
        } catch (Exception excepcion) {
            throw new IllegalStateException(
                "No fue posible convertir los datos del perfil.", excepcion);
        }
    }

    private <T> T deserializar(String json, TypeReference<T> tipo) {
        if (json == null || json.isBlank()) {
            return null;
        }

        try {
            return objectMapper.readValue(json, tipo);
        } catch (Exception excepcion) {
            throw new IllegalStateException(
                "No fue posible leer los datos almacenados del perfil.", excepcion);
        }
    }
}
