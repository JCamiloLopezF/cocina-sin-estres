package co.edu.uniquindio.cocinasinestres.planificador.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Número de WhatsApp de una tienda (KAN-53)")
class NumeroWhatsappTest {

    @Test
    @DisplayName("acepta el celular como lo escribe la usuaria y le pone el indicativo")
    void normaliza() {
        assertThat(NumeroWhatsapp.normalizar("300 123 4567")).contains("573001234567");
        assertThat(NumeroWhatsapp.normalizar("+57 300-123-4567")).contains("573001234567");
        assertThat(NumeroWhatsapp.normalizar("573001234567")).contains("573001234567");
        assertThat(NumeroWhatsapp.normalizar("(310) 555 0000")).contains("573105550000");
    }

    @Test
    @DisplayName("rechaza lo que no es un celular colombiano")
    void rechaza() {
        assertThat(NumeroWhatsapp.normalizar(null)).isEmpty();
        assertThat(NumeroWhatsapp.normalizar("")).isEmpty();
        assertThat(NumeroWhatsapp.normalizar("7451234")).isEmpty();
        assertThat(NumeroWhatsapp.normalizar("6017451234")).isEmpty();
        assertThat(NumeroWhatsapp.normalizar("30012345")).isEmpty();
        assertThat(NumeroWhatsapp.normalizar("15551234567")).isEmpty();
    }
}
