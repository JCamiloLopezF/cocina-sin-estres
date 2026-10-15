package co.edu.uniquindio.cocinasinestres.perfil.dto;

import co.edu.uniquindio.cocinasinestres.perfil.domain.PeriodicidadIngreso;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PerfilHogarResponse(
    Long id,
    BigDecimal presupuestoMonto,
    PeriodicidadIngreso periodicidadIngreso,
    Integer diaPago1,
    Integer diaPago2,
    List<Integer> diasSinTiempo,
    List<String> restricciones,
    boolean sinRestricciones,
    String otrasCondiciones,
    LocalDateTime consentimientoDatosEn,
    List<PersonaResponse> personas
) {
}
