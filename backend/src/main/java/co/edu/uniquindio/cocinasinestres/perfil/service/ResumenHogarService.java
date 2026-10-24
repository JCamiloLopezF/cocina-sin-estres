package co.edu.uniquindio.cocinasinestres.perfil.service;

import co.edu.uniquindio.cocinasinestres.comun.seguridad.CifradorDatosSensibles;
import co.edu.uniquindio.cocinasinestres.perfil.domain.CalculadoraPresupuesto;
import co.edu.uniquindio.cocinasinestres.perfil.domain.ResumenHogar;
import co.edu.uniquindio.cocinasinestres.perfil.model.Hogar;
import co.edu.uniquindio.cocinasinestres.perfil.repository.PersonaRepository;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * Entrega a otros módulos un resumen del hogar sin exponer cómo se guarda: descifra
 * las restricciones (ADR-20), calcula el presupuesto semanal (ADR-14) y dice si hay
 * niños en casa.
 *
 * <p>Las restricciones de salud solo salen de aquí hacia lógica del servidor (el
 * generador del plan); nunca se registran en logs.</p>
 */
@Service
public class ResumenHogarService {

    private static final TypeReference<List<String>> LISTA_TEXTO = new TypeReference<>() {};
    private static final TypeReference<List<Integer>> LISTA_NUMEROS = new TypeReference<>() {};

    private final CifradorDatosSensibles cifrador;
    private final ObjectMapper objectMapper;
    private final PersonaRepository personaRepository;

    public ResumenHogarService(
        CifradorDatosSensibles cifrador, ObjectMapper objectMapper, PersonaRepository personaRepository) {
        this.cifrador = cifrador;
        this.objectMapper = objectMapper;
        this.personaRepository = personaRepository;
    }

    public ResumenHogar resumir(Hogar hogar) {
        List<String> restricciones = hogar.getRestricciones() == null
            ? List.of()
            : leer(cifrador.descifrar(hogar.getRestricciones()), LISTA_TEXTO);
        List<Integer> dias = leer(hogar.getDiasSinTiempo(), LISTA_NUMEROS);
        boolean hayNinos = hogar.getId() != null && personaRepository.findByHogar_IdOrderByIdAsc(hogar.getId()).stream()
            .anyMatch(p -> p.getEdad() != null && p.getEdad() <= ResumenHogar.EDAD_MAXIMA_NINO);
        return new ResumenHogar(
            hogar.getId(),
            Set.copyOf(restricciones),
            Set.copyOf(dias),
            CalculadoraPresupuesto.presupuestoSemanal(
                hogar.getPresupuestoMonto(), hogar.getPeriodicidadIngreso()),
            hayNinos);
    }

    private <T> List<T> leer(String json, TypeReference<List<T>> tipo) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            List<T> valores = objectMapper.readValue(json, tipo);
            return valores == null ? List.of() : valores.stream().filter(v -> v != null).toList();
        } catch (Exception excepcion) {
            throw new IllegalStateException("No fue posible leer los datos del hogar.", excepcion);
        }
    }
}
