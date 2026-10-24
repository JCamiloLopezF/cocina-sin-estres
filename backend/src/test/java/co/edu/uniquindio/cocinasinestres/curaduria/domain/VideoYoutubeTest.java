package co.edu.uniquindio.cocinasinestres.curaduria.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.edu.uniquindio.cocinasinestres.comun.error.DatosInvalidosException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Video de la receta")
class VideoYoutubeTest {

    @ParameterizedTest
    @ValueSource(strings = {
        "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
        "https://youtube.com/watch?feature=share&v=dQw4w9WgXcQ&t=30",
        "https://m.youtube.com/watch?v=dQw4w9WgXcQ",
        "https://youtu.be/dQw4w9WgXcQ?si=abc",
        "https://www.youtube.com/shorts/dQw4w9WgXcQ",
        "https://www.youtube-nocookie.com/embed/dQw4w9WgXcQ?start=10",
        "  dQw4w9WgXcQ  ",
    })
    @DisplayName("saca el id de los enlaces que se copian del celular o del computador")
    void enlaces(String enlace) {
        assertThat(VideoYoutube.idDesde(enlace)).isEqualTo("dQw4w9WgXcQ");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "https://vimeo.com/123", "https://www.youtube.com/watch?v=corto"})
    @DisplayName("rechaza lo que no es un video de YouTube")
    void noEsYoutube(String enlace) {
        assertThatThrownBy(() -> VideoYoutube.idDesde(enlace)).isInstanceOf(DatosInvalidosException.class);
    }

    @Test
    @DisplayName("el fragmento dura como máximo la mitad del tiempo de la receta")
    void mitadDelTiempo() {
        // Receta de 25 minutos: el video puede durar hasta 12:30.
        VideoYoutube.validarFragmento(0, 750, 25);
        VideoYoutube.validarFragmento(60, 810, 25);
        assertThatThrownBy(() -> VideoYoutube.validarFragmento(0, 751, 25))
            .isInstanceOf(DatosInvalidosException.class)
            .hasMessageContaining("12:30");
        assertThatThrownBy(() -> VideoYoutube.validarFragmento(100, 100, 25)).isInstanceOf(DatosInvalidosException.class);
        assertThat(VideoYoutube.maximoSegundos(25)).isEqualTo(750);
    }
}
