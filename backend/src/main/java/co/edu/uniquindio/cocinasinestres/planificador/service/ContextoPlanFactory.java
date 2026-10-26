package co.edu.uniquindio.cocinasinestres.planificador.service;

import co.edu.uniquindio.cocinasinestres.perfil.domain.ResumenHogar;
import co.edu.uniquindio.cocinasinestres.perfil.model.Hogar;
import co.edu.uniquindio.cocinasinestres.perfil.service.ResumenHogarService;
import co.edu.uniquindio.cocinasinestres.planificador.domain.CatalogoParaPlan;
import co.edu.uniquindio.cocinasinestres.planificador.domain.CicloIngreso;
import co.edu.uniquindio.cocinasinestres.planificador.domain.Comensal;
import co.edu.uniquindio.cocinasinestres.planificador.domain.ContextoPlan;
import co.edu.uniquindio.cocinasinestres.planificador.domain.GeneradorPlan;
import co.edu.uniquindio.cocinasinestres.planificador.domain.Hueco;
import co.edu.uniquindio.cocinasinestres.planificador.domain.ReglasAlergia;
import co.edu.uniquindio.cocinasinestres.planificador.domain.StockEnDespensa;
import co.edu.uniquindio.cocinasinestres.planificador.model.ComidaPlan;
import java.time.Clock;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Junta lo que el generador y el adaptador de alergias necesitan saber del hogar
 * (ADR-24): catálogo, despensa, perfil y lo que ya tiene el plan. Lo usan el generador
 * del plan y la lista de compras, para que los dos vean lo mismo.
 */
@Component
public class ContextoPlanFactory {

    /**
     * @param plan      el contexto para el dominio
     * @param porVencer nombre de cada producto que vence pronto (para contárselo a la usuaria)
     */
    public record Contexto(ContextoPlan plan, Map<Long, String> porVencer) {
    }

    private final ResumenHogarService resumenHogarService;
    private final CatalogoParaPlan catalogo;
    private final StockEnDespensa stockEnDespensa;
    private final Clock reloj;

    public ContextoPlanFactory(
        ResumenHogarService resumenHogarService,
        CatalogoParaPlan catalogo,
        StockEnDespensa stockEnDespensa,
        Clock reloj) {
        this.resumenHogarService = resumenHogarService;
        this.catalogo = catalogo;
        this.stockEnDespensa = stockEnDespensa;
        this.reloj = reloj;
    }

    /**
     * El contexto para planear la semana que empieza en {@code lunes}: además de lo de
     * siempre, sabe cuántos días van desde el pago en cada día, para dejar las comidas
     * más costosas después del pago.
     */
    public Contexto construir(Hogar entidad, List<ComidaPlan> actuales, LocalDate lunes) {
        Contexto contexto = construir(entidad, actuales);
        if (lunes == null || entidad.getPeriodicidadIngreso() == null || entidad.getDiaPago1() == null) {
            return contexto;
        }
        CicloIngreso ciclo = new CicloIngreso(
            entidad.getPeriodicidadIngreso(), entidad.getDiaPago1(), entidad.getDiaPago2());
        return new Contexto(contexto.plan().conIngreso(ciclo.diasDesdeElPagoEnLaSemana(lunes)), contexto.porVencer());
    }

    public Contexto construir(Hogar entidad, List<ComidaPlan> actuales) {
        ResumenHogar hogar = resumenHogarService.resumir(entidad);
        LocalDate hoy = LocalDate.now(reloj);
        Map<Long, String> porVencer = stockEnDespensa.porVencer(
            hogar.hogarId(), hoy, hoy.plusDays(GeneradorPlan.DIAS_POR_VENCER));

        Map<Hueco, Long> fijas = new HashMap<>();
        actuales.forEach(c -> fijas.put(new Hueco(c.getDia(), c.getTipoComida()), c.getRecetaId()));

        ContextoPlan contexto = new ContextoPlan(
            catalogo.recetas(),
            hogar.restricciones(),
            hogar.diasSinTiempo(),
            hogar.presupuestoSemanal(),
            stockEnDespensa.disponibles(hogar.hogarId(), hoy),
            porVencer.keySet(),
            fijas,
            hogar.hayNinos(),
            new ReglasAlergia(
                hogar.personas().stream()
                    .map(p -> new Comensal(p.nombre(), p.restricciones(), p.alergiasGraves()))
                    .toList(),
                catalogo.alergenos(),
                catalogo.reemplazos()));
        return new Contexto(contexto, porVencer);
    }
}
