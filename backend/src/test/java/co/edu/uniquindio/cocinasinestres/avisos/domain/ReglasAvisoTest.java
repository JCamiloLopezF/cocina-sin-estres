package co.edu.uniquindio.cocinasinestres.avisos.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Reglas del motor de avisos (ADR-17)")
class ReglasAvisoTest {

    private static final Long HOGAR = 10L;
    /** Viernes. */
    private static final LocalDate VIERNES = LocalDate.of(2026, 10, 23);
    private static final LocalDate JUEVES = VIERNES.minusDays(1);
    private static final LocalDate DOMINGO = VIERNES.plusDays(2);

    private static ProductoAviso producto(String nombre) {
        return new ProductoAviso(1L, nombre, null);
    }

    private static ProductoAviso vence(String nombre, LocalDate fecha) {
        return new ProductoAviso((long) nombre.hashCode(), nombre, fecha);
    }

    @Nested
    @DisplayName("Bajo el mínimo")
    class BajoMinimo {

        @Test
        @DisplayName("sin productos en el mínimo no hay aviso")
        void sinProductos() {
            assertThat(ReglasAviso.bajoMinimo(HOGAR, VIERNES, List.of())).isEmpty();
        }

        @Test
        @DisplayName("un producto: frase corta con el verbo en singular o plural")
        void unProducto() {
            Aviso arroz = ReglasAviso.bajoMinimo(HOGAR, VIERNES, List.of(producto("Arroz"))).orElseThrow();
            assertThat(arroz.titulo()).isEqualTo("Ya casi no queda arroz");
            assertThat(arroz.mensaje()).isEqualTo("Llegó al mínimo que definiste. Tenlo en cuenta en tu próxima compra.");
            assertThat(arroz.clave()).isEqualTo("BAJO_MINIMO:10:2026-10-23");
            assertThat(arroz.enlace()).isEqualTo("/despensa?filtro=BAJO_MINIMO");

            Aviso huevos = ReglasAviso.bajoMinimo(HOGAR, VIERNES, List.of(producto("Huevos"))).orElseThrow();
            assertThat(huevos.titulo()).isEqualTo("Ya casi no quedan huevos");
            assertThat(huevos.mensaje()).startsWith("Llegaron al mínimo").contains("Tenlos");
        }

        @Test
        @DisplayName("varios productos se agrupan en un solo aviso")
        void varios() {
            Aviso aviso = ReglasAviso.bajoMinimo(HOGAR, VIERNES,
                List.of(producto("Arroz"), producto("Leche"), producto("Huevos"))).orElseThrow();
            assertThat(aviso.titulo()).isEqualTo("3 productos llegaron al mínimo");
            assertThat(aviso.mensaje()).isEqualTo("Arroz, leche y huevos. Tenlos en cuenta en tu próxima compra.");
        }
    }

    @Nested
    @DisplayName("Por vencer")
    class PorVencer {

        @Test
        @DisplayName("solo cuenta lo vencido hace hasta 7 días y lo que vence en los próximos 3")
        void ventana() {
            List<ProductoAviso> productos = List.of(
                vence("Queso", VIERNES.minusDays(8)),
                vence("Yogur", VIERNES.plusDays(4)));
            assertThat(ReglasAviso.porVencer(HOGAR, VIERNES, productos)).isEmpty();
        }

        @Test
        @DisplayName("lo que ya venció avisa: revísalo antes de usarlo")
        void vencido() {
            Aviso huevos = ReglasAviso.porVencer(HOGAR, VIERNES, List.of(vence("Huevos", VIERNES.minusDays(1)))).orElseThrow();
            assertThat(huevos.titulo()).isEqualTo("Huevos vencieron ayer");
            assertThat(huevos.mensaje()).isEqualTo("Revísalos antes de usarlos: si huelen o se ven distinto, mejor no los uses.");

            Aviso queso = ReglasAviso.porVencer(HOGAR, VIERNES, List.of(vence("Queso", VIERNES.minusDays(3)))).orElseThrow();
            assertThat(queso.titulo()).isEqualTo("Queso venció hace 3 días");
            assertThat(queso.mensaje()).isEqualTo("Revísalo antes de usarlo: si huele o se ve distinto, mejor no lo uses.");
        }

        @Test
        @DisplayName("vencidos y por vencer juntos: primero lo vencido")
        void vencidosYPorVencer() {
            Aviso aviso = ReglasAviso.porVencer(HOGAR, VIERNES, List.of(
                vence("Leche", VIERNES.plusDays(1)),
                vence("Pollo", VIERNES.minusDays(2)))).orElseThrow();
            assertThat(aviso.titulo()).isEqualTo("2 productos vencidos o por vencer");
            assertThat(aviso.mensaje()).isEqualTo("Pollo (venció hace 2 días) y leche (mañana). Revisa primero lo vencido y usa pronto lo demás.");
        }

        @Test
        @DisplayName("un producto dice cuándo vence")
        void unProducto() {
            Aviso aviso = ReglasAviso.porVencer(HOGAR, VIERNES, List.of(vence("Leche", VIERNES.plusDays(1)))).orElseThrow();
            assertThat(aviso.titulo()).isEqualTo("Leche vence mañana");
            assertThat(aviso.mensaje()).isEqualTo("Úsalo primero en tus comidas para que no se pierda.");

            Aviso huevos = ReglasAviso.porVencer(HOGAR, VIERNES, List.of(vence("Huevos", VIERNES))).orElseThrow();
            assertThat(huevos.titulo()).isEqualTo("Huevos vencen hoy");
            assertThat(huevos.mensaje()).isEqualTo("Úsalos primero en tus comidas para que no se pierdan.");
        }

        @Test
        @DisplayName("varios van del que vence primero al último")
        void varios() {
            Aviso aviso = ReglasAviso.porVencer(HOGAR, VIERNES, List.of(
                vence("Leche", VIERNES.plusDays(1)),
                vence("Pollo", VIERNES),
                vence("Queso", VIERNES.plusDays(3)))).orElseThrow();
            assertThat(aviso.titulo()).isEqualTo("3 productos vencen pronto");
            assertThat(aviso.mensaje()).startsWith("Pollo (hoy), leche (mañana) y queso (en 3 días).");
            assertThat(aviso.clave()).isEqualTo("POR_VENCER:10:2026-10-23");
        }
    }

    @Nested
    @DisplayName("Preparación previa")
    class PreparacionPrevia {

        @Test
        @DisplayName("una comida de mañana con remojo")
        void una() {
            Aviso aviso = ReglasAviso.preparacionPrevia(HOGAR, VIERNES, List.of(
                new PreparacionPendiente(6, "ALMUERZO", "Fríjoles con arroz", "Remoja los fríjoles la noche anterior"))).orElseThrow();
            assertThat(aviso.titulo()).isEqualTo("Para mañana: Fríjoles con arroz");
            assertThat(aviso.mensaje()).isEqualTo("Remoja los fríjoles la noche anterior.");
            assertThat(aviso.enlace()).isEqualTo("/plan/dia/6");
            assertThat(aviso.clave()).isEqualTo("PREPARACION_PREVIA:10:2026-10-24");
        }

        @Test
        @DisplayName("el domingo avisa del lunes, que está en la semana siguiente")
        void domingo() {
            Aviso aviso = ReglasAviso.preparacionPrevia(HOGAR, DOMINGO, List.of(
                new PreparacionPendiente(1, "ALMUERZO", "Sopa de lentejas", "Remoja la lenteja 30 minutos."),
                new PreparacionPendiente(1, "CENA", "Arroz con leche", "Deja el arroz en remojo."))).orElseThrow();
            assertThat(aviso.titulo()).isEqualTo("Mañana hay 2 comidas que se alistan desde hoy");
            assertThat(aviso.mensaje())
                .isEqualTo("Sopa de lentejas: remoja la lenteja 30 minutos. Arroz con leche: deja el arroz en remojo.");
            assertThat(aviso.enlace()).isEqualTo("/plan/dia/1?semana=proxima");
        }

        @Test
        @DisplayName("sin indicación no hay aviso")
        void sinIndicacion() {
            assertThat(ReglasAviso.preparacionPrevia(HOGAR, VIERNES, List.of(
                new PreparacionPendiente(6, "CENA", "Arepa", "  ")))).isEmpty();
        }
    }

    @Nested
    @DisplayName("Semana sin planificar")
    class Semana {

        @Test
        @DisplayName("antes del viernes no se revisa")
        void antesDelViernes() {
            assertThat(ReglasAviso.tocaRevisarSemana(JUEVES)).isFalse();
            assertThat(ReglasAviso.tocaRevisarSemana(VIERNES)).isTrue();
            assertThat(ReglasAviso.tocaRevisarSemana(DOMINGO)).isTrue();
            assertThat(ReglasAviso.semanaSinPlanificar(HOGAR, JUEVES, EstadoSemana.SIN_PLAN)).isEmpty();
        }

        @Test
        @DisplayName("sin plan invita a armarla; con borrador invita a revisarlo")
        void viernes() {
            assertThat(ReglasAviso.lunesSiguiente(VIERNES)).isEqualTo(LocalDate.of(2026, 10, 26));
            assertThat(ReglasAviso.lunesSiguiente(DOMINGO)).isEqualTo(LocalDate.of(2026, 10, 26));

            Aviso sinPlan = ReglasAviso.semanaSinPlanificar(HOGAR, VIERNES, EstadoSemana.SIN_PLAN).orElseThrow();
            assertThat(sinPlan.titulo()).isEqualTo("La próxima semana todavía no tiene plan");
            assertThat(sinPlan.accion()).isEqualTo("Armar la semana");
            assertThat(sinPlan.enlace()).isEqualTo("/plan?semana=proxima");
            assertThat(sinPlan.clave()).isEqualTo("SEMANA_SIN_PLANIFICAR:10:2026-10-26");

            Aviso borrador = ReglasAviso.semanaSinPlanificar(HOGAR, VIERNES,
                new EstadoSemana(EstadoSemana.Situacion.BORRADOR, 14)).orElseThrow();
            assertThat(borrador.titulo()).isEqualTo("Te armamos la próxima semana");
            assertThat(borrador.mensaje()).startsWith("Es un borrador con 14 comidas");
            assertThat(borrador.clave()).isEqualTo(sinPlan.clave());
        }

        @Test
        @DisplayName("una semana ya planeada no avisa")
        void planeada() {
            assertThat(ReglasAviso.semanaSinPlanificar(HOGAR, VIERNES,
                new EstadoSemana(EstadoSemana.Situacion.PLANEADA, 10))).isEmpty();
            assertThat(new EstadoSemana(EstadoSemana.Situacion.VACIA, 0).sinComidas()).isTrue();
            assertThat(new EstadoSemana(EstadoSemana.Situacion.BORRADOR, 3).sinComidas()).isFalse();
        }
    }

    @Nested
    @DisplayName("Aprovechar un producto")
    class Aprovechar {

        @Test
        @DisplayName("propone la receta que salva lo que vence")
        void unProducto() {
            Aviso aviso = ReglasAviso.aprovechar(HOGAR, VIERNES, new Aprovechamiento(7L, "Arroz con leche",
                List.of(vence("Leche", VIERNES.plusDays(1))))).orElseThrow();
            assertThat(aviso.titulo()).isEqualTo("Hoy puedes hacer Arroz con leche");
            assertThat(aviso.mensaje()).isEqualTo("Usa leche, que vence mañana, y no tienes que comprar nada.");
            assertThat(aviso.enlace()).isEqualTo("/recetas/7");
        }

        @Test
        @DisplayName("con varios productos los nombra todos")
        void varios() {
            Aviso aviso = ReglasAviso.aprovechar(HOGAR, VIERNES, new Aprovechamiento(7L, "Arroz con pollo",
                List.of(vence("Pollo", VIERNES), vence("Tomate", VIERNES.plusDays(2))))).orElseThrow();
            assertThat(aviso.mensaje()).isEqualTo("Usa pollo y tomate antes de que se dañen. No tienes que comprar nada.");
        }

        @Test
        @DisplayName("sin productos por vencer no hay aviso")
        void sinProductos() {
            assertThat(ReglasAviso.aprovechar(HOGAR, VIERNES, new Aprovechamiento(7L, "Arepa", List.of()))).isEmpty();
            assertThat(ReglasAviso.aprovechar(HOGAR, VIERNES, null)).isEmpty();
        }
    }

    @Test
    @DisplayName("se muestran como máximo 5, en el orden de ADR-17")
    void seleccionar() {
        List<Aviso> avisos = new ArrayList<>();
        ReglasAviso.aprovechar(HOGAR, VIERNES, new Aprovechamiento(1L, "Arepa", List.of(vence("Queso", VIERNES))))
            .ifPresent(avisos::add);
        ReglasAviso.semanaSinPlanificar(HOGAR, VIERNES, EstadoSemana.SIN_PLAN).ifPresent(avisos::add);
        ReglasAviso.porVencer(HOGAR, VIERNES, List.of(vence("Queso", VIERNES))).ifPresent(avisos::add);
        ReglasAviso.bajoMinimo(HOGAR, VIERNES, List.of(producto("Sal"))).ifPresent(avisos::add);
        ReglasAviso.preparacionPrevia(HOGAR, VIERNES,
            List.of(new PreparacionPendiente(6, "CENA", "Fríjoles", "Remoja."))).ifPresent(avisos::add);
        avisos.add(new Aviso(TipoAviso.POR_VENCER, "extra", "Otro", "Otro", null, null));

        List<Aviso> elegidos = ReglasAviso.seleccionar(avisos);
        assertThat(elegidos).hasSize(5);
        assertThat(elegidos).extracting(Aviso::tipo).containsExactly(
            TipoAviso.BAJO_MINIMO, TipoAviso.POR_VENCER, TipoAviso.POR_VENCER,
            TipoAviso.PREPARACION_PREVIA, TipoAviso.SEMANA_SIN_PLANIFICAR);
    }

    @Test
    @DisplayName("los textos van en tono de apoyo: sin admiraciones ni palabras de alarma (RNF-14)")
    void tono() {
        List<Aviso> todos = Stream.of(
                ReglasAviso.bajoMinimo(HOGAR, VIERNES, List.of(producto("Arroz"), producto("Sal"))),
                ReglasAviso.porVencer(HOGAR, VIERNES, List.of(vence("Leche", VIERNES))),
                ReglasAviso.preparacionPrevia(HOGAR, VIERNES, List.of(new PreparacionPendiente(6, "CENA", "Fríjoles", "Remoja"))),
                ReglasAviso.semanaSinPlanificar(HOGAR, VIERNES, EstadoSemana.SIN_PLAN),
                ReglasAviso.aprovechar(HOGAR, VIERNES, new Aprovechamiento(1L, "Arepa", List.of(vence("Queso", VIERNES)))))
            .flatMap(java.util.Optional::stream)
            .toList();
        assertThat(todos).hasSize(5).allSatisfy(a -> {
            String texto = (a.titulo() + " " + a.mensaje()).toLowerCase();
            assertThat(texto).doesNotContain("!", "urgente", "alerta", "cuidado", "peligro");
        });
    }

    @Test
    @DisplayName("textos de apoyo: listas, fechas y plurales")
    void textos() {
        assertThat(ReglasAviso.enumerar(List.of("a"))).isEqualTo("a");
        assertThat(ReglasAviso.enumerar(List.of("a", "b"))).isEqualTo("a y b");
        assertThat(ReglasAviso.enumerar(List.of("a", "b", "c", "d", "e", "f"))).isEqualTo("a, b, c, d y 2 más");
        assertThat(ReglasAviso.cuando(VIERNES.plusDays(2), VIERNES)).isEqualTo("en 2 días");
        assertThat(ReglasAviso.esPlural("Fríjoles rojos")).isTrue();
        assertThat(ReglasAviso.esPlural("Tomate de árbol")).isFalse();
        assertThat(ReglasAviso.esPlural("Gas")).isFalse();
        assertThat(new Aviso(TipoAviso.POR_VENCER, "k", "t", "x".repeat(300), null, null).mensaje()).hasSize(255);
        assertThat(TipoAviso.desde("por_vencer")).isEqualTo(TipoAviso.POR_VENCER);
        assertThat(TipoAviso.desde("OTRO")).isNull();
    }
}
