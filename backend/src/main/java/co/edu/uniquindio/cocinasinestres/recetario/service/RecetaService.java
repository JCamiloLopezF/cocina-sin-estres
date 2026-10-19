package co.edu.uniquindio.cocinasinestres.recetario.service;

import co.edu.uniquindio.cocinasinestres.comun.error.RecursoNoEncontradoException;
import co.edu.uniquindio.cocinasinestres.recetario.dto.IngredienteRecetaResponse;
import co.edu.uniquindio.cocinasinestres.recetario.dto.RecetaDetalleResponse;
import co.edu.uniquindio.cocinasinestres.recetario.dto.RecetaResponse;
import co.edu.uniquindio.cocinasinestres.recetario.model.Receta;
import co.edu.uniquindio.cocinasinestres.recetario.repository.RecetaRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Casos de uso de lectura del catálogo de recetas. Las recetas no se borran: solo
 * se desactivan, así que el catálogo y el detalle solo muestran las activas.
 */
@Service
public class RecetaService {

    private final RecetaRepository recetaRepository;

    public RecetaService(RecetaRepository recetaRepository) {
        this.recetaRepository = recetaRepository;
    }

    /** Recetas activas ordenadas por nombre, sin pasos ni ingredientes. */
    @Transactional(readOnly = true)
    public List<RecetaResponse> listar() {
        return recetaRepository.findByActivaTrueOrderByNombreAsc()
            .stream()
            .map(RecetaService::aResponse)
            .toList();
    }

    /**
     * Detalle de una receta activa: pasos separados e ingredientes.
     *
     * @throws RecursoNoEncontradoException 404 si no existe o está desactivada
     */
    @Transactional(readOnly = true)
    public RecetaDetalleResponse obtenerPorId(Long id) {
        Receta receta = recetaRepository.findById(id)
            .filter(Receta::isActiva)
            .orElseThrow(() ->
                new RecursoNoEncontradoException("La receta solicitada no existe."));

        return aDetalle(receta);
    }

    private static RecetaResponse aResponse(Receta receta) {
        return new RecetaResponse(
            receta.getId(),
            receta.getNombre(),
            receta.getDescripcion(),
            receta.getTiempoMinutos(),
            receta.getPorciones()
        );
    }

    private static RecetaDetalleResponse aDetalle(Receta receta) {
        List<IngredienteRecetaResponse> ingredientes = receta.getIngredientes()
            .stream()
            .map(ingrediente -> new IngredienteRecetaResponse(
                ingrediente.getProductoId(),
                ingrediente.getCantidad(),
                ingrediente.isOpcional()))
            .toList();

        return new RecetaDetalleResponse(
            receta.getId(),
            receta.getNombre(),
            receta.getDescripcion(),
            receta.getTiempoMinutos(),
            receta.getPorciones(),
            receta.getPreparacionPrevia(),
            receta.pasosEnLista(),
            ingredientes
        );
    }
}
