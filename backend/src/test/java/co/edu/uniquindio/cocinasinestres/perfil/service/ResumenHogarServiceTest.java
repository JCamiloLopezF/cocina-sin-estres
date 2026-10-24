package co.edu.uniquindio.cocinasinestres.perfil.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import co.edu.uniquindio.cocinasinestres.comun.config.PropiedadesCocina;
import co.edu.uniquindio.cocinasinestres.comun.seguridad.CifradorDatosSensibles;
import co.edu.uniquindio.cocinasinestres.perfil.domain.ResumenHogar;
import co.edu.uniquindio.cocinasinestres.perfil.model.Hogar;
import co.edu.uniquindio.cocinasinestres.perfil.model.Persona;
import co.edu.uniquindio.cocinasinestres.perfil.repository.PersonaRepository;
import java.math.BigDecimal;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

@DisplayName("Resumen del hogar para el planificador")
class ResumenHogarServiceTest {

    private final CifradorDatosSensibles cifrador = new CifradorDatosSensibles(new PropiedadesCocina(
        null, new PropiedadesCocina.Cifrado(Base64.getEncoder().encodeToString(new byte[32])), null, null));
    private final PersonaRepository personaRepository = mock(PersonaRepository.class);
    private final ResumenHogarService servicio =
        new ResumenHogarService(cifrador, JsonMapper.builder().build(), personaRepository);

    private static Persona deEdad(int edad) {
        Persona persona = mock(Persona.class);
        when(persona.getEdad()).thenReturn(edad);
        return persona;
    }

    private Persona persona(String nombre, int edad, String restricciones, String graves) {
        Persona persona = mock(Persona.class);
        when(persona.getNombre()).thenReturn(nombre);
        when(persona.getEdad()).thenReturn(edad);
        when(persona.getRestricciones()).thenReturn(restricciones == null ? null : cifrador.cifrar(restricciones));
        when(persona.getAlergiasGraves()).thenReturn(graves == null ? null : cifrador.cifrar(graves));
        return persona;
    }

    @Test
    @DisplayName("descifra las restricciones y prorratea el presupuesto a la semana")
    void resume() {
        Hogar hogar = mock(Hogar.class);
        when(hogar.getId()).thenReturn(5L);
        when(hogar.getRestricciones()).thenReturn(cifrador.cifrar("[\"lactosa\",\"diabetes\"]"));
        when(hogar.getDiasSinTiempo()).thenReturn("[3,5]");
        when(hogar.getPresupuestoMonto()).thenReturn(new BigDecimal("700000"));
        when(hogar.getPeriodicidadIngreso()).thenReturn("SEMANAL");
        List<Persona> personas = List.of(deEdad(34), deEdad(9));
        when(personaRepository.findByHogar_IdOrderByIdAsc(5L)).thenReturn(personas);

        ResumenHogar resumen = servicio.resumir(hogar);

        assertThat(resumen.hogarId()).isEqualTo(5L);
        assertThat(resumen.restricciones()).containsExactlyInAnyOrder("lactosa", "diabetes");
        assertThat(resumen.diasSinTiempo()).containsExactlyInAnyOrder(3, 5);
        assertThat(resumen.presupuestoSemanal()).isEqualByComparingTo("700000");
        assertThat(resumen.hayNinos()).isTrue();
    }

    @Test
    @DisplayName("un adolescente de 13 años ya no cuenta como niño")
    void sinNinos() {
        Hogar hogar = mock(Hogar.class);
        when(hogar.getId()).thenReturn(6L);
        when(hogar.getPresupuestoMonto()).thenReturn(new BigDecimal("300000"));
        when(hogar.getPeriodicidadIngreso()).thenReturn("SEMANAL");
        List<Persona> personas = List.of(deEdad(40), deEdad(13));
        when(personaRepository.findByHogar_IdOrderByIdAsc(6L)).thenReturn(personas);

        assertThat(servicio.resumir(hogar).hayNinos()).isFalse();
    }

    @Test
    @DisplayName("sin restricciones ni días sin tiempo devuelve conjuntos vacíos")
    void vacio() {
        Hogar hogar = mock(Hogar.class);
        when(hogar.getPresupuestoMonto()).thenReturn(new BigDecimal("300000"));
        when(hogar.getPeriodicidadIngreso()).thenReturn("MENSUAL");

        ResumenHogar resumen = servicio.resumir(hogar);

        assertThat(resumen.restricciones()).isEmpty();
        assertThat(resumen.diasSinTiempo()).isEmpty();
        assertThat(resumen.presupuestoSemanal()).isPositive();
    }

    @Test
    @DisplayName("entrega cada persona con sus alergias descifradas y un nombre para los avisos")
    void personas() {
        Hogar hogar = mock(Hogar.class);
        when(hogar.getId()).thenReturn(7L);
        when(hogar.getPresupuestoMonto()).thenReturn(new BigDecimal("300000"));
        when(hogar.getPeriodicidadIngreso()).thenReturn("SEMANAL");
        List<Persona> personas = List.of(
            persona(null, 34, null, null),
            persona("Samuel", 9, "[\"huevo\",\"mani\"]", "[\"mani\"]"),
            persona(" ", 60, "[\"diabetes\"]", null));
        when(personaRepository.findByHogar_IdOrderByIdAsc(7L)).thenReturn(personas);

        var resumen = servicio.resumir(hogar).personas();

        assertThat(resumen).extracting(p -> p.nombre()).containsExactly("ti", "Samuel", "la persona 3");
        assertThat(resumen.get(1).restricciones()).containsExactlyInAnyOrder("huevo", "mani");
        assertThat(resumen.get(1).alergiasGraves()).containsExactly("mani");
        assertThat(resumen.get(2).alergiasGraves()).isEmpty();
    }
}
