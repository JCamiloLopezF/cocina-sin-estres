package co.edu.uniquindio.cocinasinestres.perfil.dto;

import co.edu.uniquindio.cocinasinestres.perfil.domain.PeriodicidadIngreso;

import java.math.BigDecimal;
import java.util.List;

/**
 * Request para crear/actualizar perfil del hogar.
 */
public record PerfilHogarRequest(
    BigDecimal presupuestoMonto,
    PeriodicidadIngreso periodicidadIngreso,
    Integer diaPago1,
    Integer diaPago2,
    List<Integer> diasSinTiempo,
    List<String> restricciones,
    boolean sinRestricciones,
    String otrasCondiciones,
    boolean consentimientoDatos,
    List<PersonaRequest> personas
) {
}
