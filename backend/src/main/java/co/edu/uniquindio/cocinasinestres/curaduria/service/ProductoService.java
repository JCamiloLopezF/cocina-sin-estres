package co.edu.uniquindio.cocinasinestres.curaduria.service;

import co.edu.uniquindio.cocinasinestres.comun.error.RecursoNoEncontradoException;
import co.edu.uniquindio.cocinasinestres.curaduria.domain.AccionAuditoria;
import co.edu.uniquindio.cocinasinestres.curaduria.domain.ComparadorProducto;
import co.edu.uniquindio.cocinasinestres.curaduria.domain.EntidadCatalogo;
import co.edu.uniquindio.cocinasinestres.curaduria.domain.InstantaneaProducto;
import co.edu.uniquindio.cocinasinestres.curaduria.domain.ValidadorProducto;
import co.edu.uniquindio.cocinasinestres.curaduria.dto.ProductoRequest;
import co.edu.uniquindio.cocinasinestres.curaduria.dto.ProductoResponse;
import co.edu.uniquindio.cocinasinestres.curaduria.model.AuditoriaCatalogo;
import co.edu.uniquindio.cocinasinestres.curaduria.model.Producto;
import co.edu.uniquindio.cocinasinestres.curaduria.repository.AuditoriaCatalogoRepository;
import co.edu.uniquindio.cocinasinestres.curaduria.repository.ProductoRepository;
import co.edu.uniquindio.cocinasinestres.perfil.domain.UsuarioAutenticado;
import co.edu.uniquindio.cocinasinestres.perfil.service.UsuarioActualService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Casos de uso del catálogo de productos. Cada cambio queda registrado en la
 * auditoría dentro de la misma transacción (RNF-24).
 */
@Service
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final AuditoriaCatalogoRepository auditoriaRepository;
    private final UsuarioActualService usuarioActualService;

    public ProductoService(
        ProductoRepository productoRepository,
        AuditoriaCatalogoRepository auditoriaRepository,
        UsuarioActualService usuarioActualService) {

        this.productoRepository = productoRepository;
        this.auditoriaRepository = auditoriaRepository;
        this.usuarioActualService = usuarioActualService;
    }

    @Transactional(readOnly = true)
    public List<ProductoResponse> listar() {
        return productoRepository.findByActivoTrueOrderByCategoriaAscNombreAsc()
            .stream()
            .map(ProductoMapper::aResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public ProductoResponse obtenerPorId(Long id) {
        return ProductoMapper.aResponse(buscarOExcepcion(id));
    }

    @Transactional
    public ProductoResponse crear(ProductoRequest request) {

        validar(request);

        UsuarioAutenticado usuarioActual = usuarioActualService.obtener();

        Producto producto = new Producto(
            null,
            request.nombre(),
            request.categoria(),
            request.tipoCantidad(),
            request.unidadBase(),
            request.unidadCompra(),
            request.cantidadUnidadCompra(),
            request.precioUnidadCompra(),
            request.capacidadReferencia(),
            request.esBasico(),
            true,
            usuarioActual.id()
        );

        Producto guardado = productoRepository.save(producto);

        registrarAuditoria(
            AccionAuditoria.CREAR,
            guardado.getId(),
            null,
            ProductoMapper.aInstantanea(guardado),
            usuarioActual.id()
        );

        return ProductoMapper.aResponse(guardado);
    }

    @Transactional
    public ProductoResponse actualizar(Long id, ProductoRequest request) {

        validar(request);

        Producto producto = buscarOExcepcion(id);
        InstantaneaProducto antes = ProductoMapper.aInstantanea(producto);

        producto.setNombre(request.nombre());
        producto.setCategoria(request.categoria());
        producto.setTipoCantidad(request.tipoCantidad());
        producto.setUnidadBase(request.unidadBase());
        producto.setUnidadCompra(request.unidadCompra());
        producto.setCantidadUnidadCompra(request.cantidadUnidadCompra());
        producto.setPrecioUnidadCompra(request.precioUnidadCompra());
        producto.setCapacidadReferencia(request.capacidadReferencia());
        producto.setEsBasico(request.esBasico());

        InstantaneaProducto despues = ProductoMapper.aInstantanea(producto);

        // Sin cambios reales no se toca el responsable ni se audita
        if (ComparadorProducto.comparar(antes, despues).isEmpty()) {
            return ProductoMapper.aResponse(producto);
        }

        UsuarioAutenticado usuarioActual = usuarioActualService.obtener();
        producto.setModificadoPor(usuarioActual.id());

        registrarAuditoria(
            AccionAuditoria.EDITAR,
            producto.getId(),
            antes,
            despues,
            usuarioActual.id()
        );

        return ProductoMapper.aResponse(producto);
    }

    /**
     * Desactiva el producto. Es idempotente: si ya estaba desactivado no hace
     * nada y no genera un registro de auditoría.
     */
    @Transactional
    public void desactivar(Long id) {

        Producto producto = buscarOExcepcion(id);

        if (!producto.isActivo()) {
            return;
        }

        UsuarioAutenticado usuarioActual = usuarioActualService.obtener();
        InstantaneaProducto antes = ProductoMapper.aInstantanea(producto);

        producto.setActivo(false);
        producto.setModificadoPor(usuarioActual.id());

        registrarAuditoria(
            AccionAuditoria.DESACTIVAR,
            producto.getId(),
            antes,
            ProductoMapper.aInstantanea(producto),
            usuarioActual.id()
        );
    }

    private Producto buscarOExcepcion(Long id) {
        return productoRepository.findById(id)
            .orElseThrow(() ->
                new RecursoNoEncontradoException(
                    "El producto solicitado no existe."
                ));
    }

    private void validar(ProductoRequest request) {
        ValidadorProducto.validar(
            request.nombre(),
            request.categoria(),
            request.tipoCantidad(),
            request.unidadBase(),
            request.unidadCompra(),
            request.cantidadUnidadCompra(),
            request.precioUnidadCompra(),
            request.capacidadReferencia()
        );
    }

    private void registrarAuditoria(
        AccionAuditoria accion,
        Long productoId,
        InstantaneaProducto antes,
        InstantaneaProducto despues,
        Long usuarioId) {

        Map<String, Object> cambios =
            new LinkedHashMap<>(ComparadorProducto.comparar(antes, despues));

        auditoriaRepository.save(
            new AuditoriaCatalogo(
                EntidadCatalogo.PRODUCTO,
                productoId,
                accion,
                usuarioId,
                cambios
            )
        );
    }
}
