package co.edu.uniquindio.cocinasinestres.recetario.service;

import co.edu.uniquindio.cocinasinestres.comun.error.RecursoNoEncontradoException;
import co.edu.uniquindio.cocinasinestres.recetario.domain.RestriccionesDeProductos;
import co.edu.uniquindio.cocinasinestres.recetario.dto.IngredienteRecetaResponse;
import co.edu.uniquindio.cocinasinestres.recetario.dto.RecetaDetalleResponse;
import co.edu.uniquindio.cocinasinestres.recetario.dto.RecetaResponse;
import co.edu.uniquindio.cocinasinestres.recetario.dto.VideoRecetaResponse;
import co.edu.uniquindio.cocinasinestres.recetario.model.IngredienteReceta;
import co.edu.uniquindio.cocinasinestres.recetario.model.Receta;
import co.edu.uniquindio.cocinasinestres.recetario.repository.RecetaRepository;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Casos de uso de lectura del catálogo de recetas. Las recetas no se borran: solo
 * se desactivan, así que el catálogo y el detalle solo muestran las activas.
 */
@Service
public class RecetaService {

    private final RecetaRepository recetaRepository;
    private final RestriccionesDeProductos restriccionesDeProductos;

    public RecetaService(RecetaRepository recetaRepository, RestriccionesDeProductos restriccionesDeProductos) {
        this.recetaRepository = recetaRepository;
        this.restriccionesDeProductos = restriccionesDeProductos;
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

        Map<Long, List<String>> restricciones = restriccionesDeProductos.codigosPorProducto(
            receta.getIngredientes().stream().map(IngredienteReceta::getProductoId).toList());
        return aDetalle(receta, restricciones);
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

    private static RecetaDetalleResponse aDetalle(Receta receta, Map<Long, List<String>> restricciones) {
        List<IngredienteRecetaResponse> ingredientes = receta.getIngredientes()
            .stream()
            .map(ingrediente -> new IngredienteRecetaResponse(
                ingrediente.getProductoId(),
                ingrediente.getCantidad(),
                ingrediente.isOpcional(),
                restricciones.getOrDefault(ingrediente.getProductoId(), List.of())))
            .toList();

        List<String> deLaReceta = ingredientes.stream()
            .filter(ingrediente -> !ingrediente.opcional())
            .flatMap(ingrediente -> ingrediente.restricciones().stream())
            .distinct()
            .sorted()
            .toList();

        return new RecetaDetalleResponse(
            receta.getId(),
            receta.getNombre(),
            receta.getDescripcion(),
            receta.getTiempoMinutos(),
            receta.getPorciones(),
            receta.getPreparacionPrevia(),
            receta.pasosEnLista(),
            ingredientes,
            deLaReceta,
            receta.getVideoYoutubeId() == null
                ? null
                : new VideoRecetaResponse(
                    receta.getVideoYoutubeId(), receta.getVideoInicioSeg(), receta.getVideoFinSeg())
        );
    }
}
