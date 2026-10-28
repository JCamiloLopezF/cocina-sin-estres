package co.edu.uniquindio.cocinasinestres.despensa.service;

import static co.edu.uniquindio.cocinasinestres.despensa.ErroresDePrueba.codigoDelError;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.edu.uniquindio.cocinasinestres.comun.error.CodigoError;
import co.edu.uniquindio.cocinasinestres.curaduria.domain.TipoCantidad;
import co.edu.uniquindio.cocinasinestres.curaduria.domain.UnidadBase;
import co.edu.uniquindio.cocinasinestres.despensa.domain.CatalogoProductos;
import co.edu.uniquindio.cocinasinestres.despensa.domain.CodigoDespensa;
import co.edu.uniquindio.cocinasinestres.despensa.domain.DatosItemDespensa;
import co.edu.uniquindio.cocinasinestres.despensa.domain.EstadoVencimiento;
import co.edu.uniquindio.cocinasinestres.despensa.domain.HogarDeUsuaria;
import co.edu.uniquindio.cocinasinestres.despensa.domain.NivelGranel;
import co.edu.uniquindio.cocinasinestres.despensa.domain.ProductoCatalogo;
import co.edu.uniquindio.cocinasinestres.despensa.dto.EdicionItemDespensaRequest;
import co.edu.uniquindio.cocinasinestres.despensa.dto.ItemDespensaRequest;
import co.edu.uniquindio.cocinasinestres.despensa.dto.ItemDespensaResponse;
import co.edu.uniquindio.cocinasinestres.despensa.dto.ProductoAdicionalRequest;
import co.edu.uniquindio.cocinasinestres.despensa.dto.ProductoDisponibleResponse;
import co.edu.uniquindio.cocinasinestres.despensa.dto.ResumenDespensaResponse;
import co.edu.uniquindio.cocinasinestres.despensa.model.ItemDespensa;
import co.edu.uniquindio.cocinasinestres.despensa.repository.ItemDespensaRepository;
import co.edu.uniquindio.cocinasinestres.perfil.domain.Rol;
import co.edu.uniquindio.cocinasinestres.perfil.domain.UsuarioAutenticado;
import co.edu.uniquindio.cocinasinestres.perfil.service.UsuarioActualService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.AdditionalAnswers;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Prueba de los casos de uso de la despensa con dobles: no levanta Spring ni toca MySQL.
 * El repositorio y la usuaria actual son dobles de Mockito; el catálogo y el hogar son
 * implementaciones en memoria de los puertos de {@code domain/}.
 */
@DisplayName("DespensaService")
class DespensaServiceTest {

    private static final Long USUARIA_ID = 10L;
    private static final Long HOGAR_ID = 100L;
    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");

    // 14 de octubre de 2026, 10:00 a. m. en Bogotá
    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-10-14T15:00:00Z"), BOGOTA);

    private static final ProductoCatalogo HUEVOS = new ProductoCatalogo(
        1L, "Huevos", "LACTEOS", TipoCantidad.CONTABLE, UnidadBase.unidad,
        null, "Cubeta", new BigDecimal("30"), new BigDecimal("19500"));

    private static final ProductoCatalogo ARROZ = new ProductoCatalogo(
        2L, "Arroz", "GRANOS", TipoCantidad.GRANEL, UnidadBase.g,
        new BigDecimal("1000"), "Bolsa", new BigDecimal("1000"), new BigDecimal("4500"));

    private static final ProductoCatalogo ACEITE = new ProductoCatalogo(
        3L, "Aceite vegetal", "ACEITES", TipoCantidad.GRANEL, UnidadBase.ml,
        new BigDecimal("1000"), "Botella", new BigDecimal("1000"), new BigDecimal("8000"));

    private static final ProductoCatalogo FRIJOL = new ProductoCatalogo(
        4L, "Fríjol", "GRANOS", TipoCantidad.GRANEL, UnidadBase.g,
        new BigDecimal("500"), "Bolsa", new BigDecimal("500"), new BigDecimal("5500"));

    private static final ProductoCatalogo POLLO = new ProductoCatalogo(
        5L, "Pollo", "PROTEINAS", TipoCantidad.CONTABLE, UnidadBase.g,
        null, "Bandeja", new BigDecimal("500"), new BigDecimal("12000"));

    private ItemDespensaRepository itemRepository;
    private UsuarioActualService usuarioActualService;
    private CatalogoEnMemoria catalogo;
    private HogarEnMemoria hogares;

    @BeforeEach
    void prepararDobles() {
        itemRepository = mock(ItemDespensaRepository.class);
        usuarioActualService = mock(UsuarioActualService.class);
        catalogo = new CatalogoEnMemoria(HUEVOS, ARROZ, ACEITE, FRIJOL, POLLO);
        hogares = new HogarEnMemoria();
        hogares.asignar(USUARIA_ID, HOGAR_ID);

        when(usuarioActualService.obtener()).thenReturn(
            new UsuarioAutenticado(USUARIA_ID, "uid-mariana", "mariana@ejemplo.com", Rol.USUARIA, true));
    }

    // ---------- Catálogo para agregar (pantalla 16) ----------

    @Test
    void elCatalogoSaleOrdenadoPorNombreYMarcaLoQueYaEstaEnLaDespensa() {
        ItemDespensa huevosEnCasa = item(1L, "6", null, null);
        conId(huevosEnCasa, 77L);
        when(itemRepository.findByHogarId(HOGAR_ID)).thenReturn(List.of(huevosEnCasa));

        List<ProductoDisponibleResponse> catalogoDisponible = servicio().catalogo(null);

        assertThat(catalogoDisponible).extracting(ProductoDisponibleResponse::nombre)
            .containsExactly("Aceite vegetal", "Arroz", "Fríjol", "Huevos", "Pollo");
        assertThat(catalogoDisponible).filteredOn(p -> p.id().equals(1L))
            .singleElement().extracting(ProductoDisponibleResponse::itemDespensaId).isEqualTo(77L);
        assertThat(catalogoDisponible).filteredOn(p -> p.id().equals(2L))
            .singleElement().extracting(ProductoDisponibleResponse::itemDespensaId).isNull();
    }

    @Test
    void elCatalogoBuscaSinDistinguirTildesNiMayusculas() {
        when(itemRepository.findByHogarId(HOGAR_ID)).thenReturn(List.of());

        assertThat(servicio().catalogo("FRIJOL")).extracting(ProductoDisponibleResponse::nombre)
            .containsExactly("Fríjol");
        assertThat(servicio().catalogo("ar")).extracting(ProductoDisponibleResponse::nombre)
            .containsExactly("Arroz");
    }

    @Test
    void sinHogarElCatalogoTambienPideCompletarElPerfil() {
        hogares.quitar(USUARIA_ID);

        assertThat(codigoDelError(() -> servicio().catalogo(null)))
            .isEqualTo(CodigoDespensa.HOGAR_REQUERIDO);
    }

    // ---------- Agregar ----------

    @Test
    void sinFechaElVencimientoSeEstimaConLaVidaUtilDelProducto() {
        guardarDevolviendoLoRecibido();
        catalogo.vidaUtil.put(1L, 21);

        servicio().agregar(new ItemDespensaRequest(1L, new BigDecimal("30"), null, null, null, null, null));

        // 14 de octubre + 21 días
        assertThat(capturarGuardado().getVenceEn()).isEqualTo(LocalDate.of(2026, 11, 4));
    }

    @Test
    void laFechaQueEscribeLaUsuariaGanaALaVidaUtil() {
        guardarDevolviendoLoRecibido();
        catalogo.vidaUtil.put(1L, 21);

        servicio().agregar(new ItemDespensaRequest(1L, new BigDecimal("30"), null, null, null, LocalDate.of(2026, 10, 20), null));

        assertThat(capturarGuardado().getVenceEn()).isEqualTo(LocalDate.of(2026, 10, 20));
    }

    @Test
    void agregaUnProductoContableConSuCantidadVencimientoYMinimo() {
        guardarDevolviendoLoRecibido();

        ItemDespensaResponse respuesta = servicio().agregar(new ItemDespensaRequest(
            1L, new BigDecimal("12"), null, new BigDecimal("4"), null,
            LocalDate.of(2026, 10, 20), "Nevera"));

        ItemDespensa guardado = capturarGuardado();
        assertThat(guardado.getHogarId()).isEqualTo(HOGAR_ID);
        assertThat(guardado.getProductoId()).isEqualTo(1L);
        assertThat(guardado.getCantidad()).isEqualByComparingTo("12");
        assertThat(guardado.isEsEstimada()).isFalse();
        assertThat(guardado.getCantidadMinima()).isEqualByComparingTo("4");
        assertThat(guardado.getVenceEn()).isEqualTo(LocalDate.of(2026, 10, 20));

        assertThat(respuesta.nombre()).isEqualTo("Huevos");
        assertThat(respuesta.nivel()).isNull();
        assertThat(respuesta.diasParaVencer()).isEqualTo(6);
        assertThat(respuesta.estadoVencimiento()).isEqualTo(EstadoVencimiento.POR_VENCER);
        assertThat(respuesta.bajoMinimo()).isFalse();
    }

    @Test
    void agregaUnProductoAGranelConUnaCantidadEstimadaDelNivel() {
        guardarDevolviendoLoRecibido();

        ItemDespensaResponse respuesta = servicio().agregar(new ItemDespensaRequest(
            2L, null, NivelGranel.MEDIO, null, NivelGranel.POCO, null, null));

        ItemDespensa guardado = capturarGuardado();
        assertThat(guardado.getCantidad()).isEqualByComparingTo("500");
        assertThat(guardado.isEsEstimada()).isTrue();
        assertThat(guardado.getCantidadMinima()).isEqualByComparingTo("250");

        assertThat(respuesta.nivel()).isEqualTo(NivelGranel.MEDIO);
        assertThat(respuesta.nivelMinimo()).isEqualTo(NivelGranel.POCO);
        assertThat(respuesta.esEstimada()).isTrue();
        assertThat(respuesta.bajoMinimo()).isFalse();
    }

    // ---------- Producto adicional del hogar (ADR-18) ----------

    @Test
    void registraUnProductoAdicionalYLoAgregaALaDespensa() {
        guardarDevolviendoLoRecibido();

        ItemDespensaResponse respuesta = servicio().agregarAdicional(new ProductoAdicionalRequest(
            "  Arepas   de maíz ", null, TipoCantidad.CONTABLE, null,
            new BigDecimal("10"), null, null, null, null, "Nevera"));

        ItemDespensa guardado = capturarGuardado();
        assertThat(guardado.getHogarId()).isEqualTo(HOGAR_ID);
        assertThat(guardado.getCantidad()).isEqualByComparingTo("10");
        ProductoCatalogo registrado = catalogo.productos.get(guardado.getProductoId());
        assertThat(registrado.nombre()).isEqualTo("Arepas de maíz");
        assertThat(registrado.categoria()).isEqualTo(DespensaService.CATEGORIA_ADICIONAL);
        assertThat(registrado.unidadBase()).isEqualTo(UnidadBase.unidad);
        assertThat(registrado.capacidadReferencia()).isNull();
        assertThat(respuesta.nombre()).isEqualTo("Arepas de maíz");
    }

    @Test
    void unAdicionalAGranelSeRegistraPorNivelConCapacidadDeReferencia() {
        guardarDevolviendoLoRecibido();

        servicio().agregarAdicional(new ProductoAdicionalRequest(
            "Quinua", "granos", TipoCantidad.GRANEL, UnidadBase.g,
            null, NivelGranel.LLENO, null, null, null, null));

        ItemDespensa guardado = capturarGuardado();
        assertThat(guardado.getCantidad()).isEqualByComparingTo("1000");
        assertThat(guardado.isEsEstimada()).isTrue();
        assertThat(catalogo.productos.get(guardado.getProductoId()).categoria()).isEqualTo("GRANOS");
    }

    @Test
    void noRegistraUnAdicionalConElNombreDeUnProductoQueYaExiste() {
        assertThat(codigoDelError(() -> servicio().agregarAdicional(new ProductoAdicionalRequest(
            "fríjol", null, TipoCantidad.GRANEL, null, null, NivelGranel.MEDIO, null, null, null, null))))
            .isEqualTo(CodigoDespensa.PRODUCTO_EXISTE);

        verify(itemRepository, never()).saveAndFlush(any(ItemDespensa.class));
    }

    @Test
    void validaElAdicionalAntesDeRegistrarlo() {
        assertThat(codigoDelError(() -> servicio().agregarAdicional(new ProductoAdicionalRequest(
            " ", null, TipoCantidad.CONTABLE, null, BigDecimal.ONE, null, null, null, null, null))))
            .isEqualTo(CodigoDespensa.NOMBRE_REQUERIDO);
        assertThat(codigoDelError(() -> servicio().agregarAdicional(new ProductoAdicionalRequest(
            "Tamal", null, null, null, BigDecimal.ONE, null, null, null, null, null))))
            .isEqualTo(CodigoDespensa.TIPO_REQUERIDO);
        assertThat(codigoDelError(() -> servicio().agregarAdicional(new ProductoAdicionalRequest(
            "Tamal", null, TipoCantidad.CONTABLE, UnidadBase.g, BigDecimal.ONE, null, null, null, null, null))))
            .isEqualTo(CodigoDespensa.UNIDAD_INVALIDA);
        // Contable sin cantidad: la regla de cantidad mixta se aplica antes de guardar el producto.
        assertThat(codigoDelError(() -> servicio().agregarAdicional(new ProductoAdicionalRequest(
            "Tamal", null, TipoCantidad.CONTABLE, null, null, null, null, null, null, null))))
            .isEqualTo(CodigoDespensa.CANTIDAD_REQUERIDA);

        assertThat(catalogo.productos).hasSize(5);
        verify(itemRepository, never()).saveAndFlush(any(ItemDespensa.class));
    }

    @Test
    void noAgregaUnProductoQueYaEstaEnLaDespensa() {
        when(itemRepository.existsByHogarIdAndProductoId(HOGAR_ID, 1L)).thenReturn(true);

        assertThat(codigoDelError(() -> servicio().agregar(contableValido(1L))))
            .isEqualTo(CodigoDespensa.PRODUCTO_REPETIDO);

        verify(itemRepository, never()).saveAndFlush(any(ItemDespensa.class));
    }

    @Test
    void traduceElChoqueDeLaBaseCuandoDosPeticionesAgreganElMismoProducto() {
        when(itemRepository.saveAndFlush(any(ItemDespensa.class)))
            .thenThrow(new DataIntegrityViolationException("uk_item_despensa_hogar_id_producto_id"));

        assertThat(codigoDelError(() -> servicio().agregar(contableValido(1L))))
            .isEqualTo(CodigoDespensa.PRODUCTO_REPETIDO);
    }

    @Test
    void unProductoNoDisponibleParaElHogarRespondeComoSiNoExistiera() {
        // El 99 no está en el catálogo (o es de otro hogar, o está desactivado)
        assertThat(codigoDelError(() -> servicio().agregar(contableValido(99L))))
            .isEqualTo(CodigoError.RECURSO_NO_ENCONTRADO);

        verify(itemRepository, never()).saveAndFlush(any(ItemDespensa.class));
    }

    @Test
    void exigeElProductoAlAgregar() {
        assertThat(codigoDelError(() -> servicio().agregar(contableValido(null))))
            .isEqualTo(CodigoDespensa.PRODUCTO_REQUERIDO);
    }

    @Test
    void noGuardaNadaSiLosDatosIncumplenLaReglaDeCantidadMixta() {
        // Un producto a granel no se registra con una cantidad numérica
        ItemDespensaRequest granelConCantidad = new ItemDespensaRequest(
            2L, new BigDecimal("500"), null, null, null, null, null);

        assertThat(codigoDelError(() -> servicio().agregar(granelConCantidad)))
            .isEqualTo(CodigoDespensa.CANTIDAD_NO_PERMITIDA);

        verify(itemRepository, never()).saveAndFlush(any(ItemDespensa.class));
    }

    @Test
    void unaUsuariaSinHogarDebeCompletarSuPerfilPrimero() {
        hogares.quitar(USUARIA_ID);

        assertThat(codigoDelError(() -> servicio().agregar(contableValido(1L))))
            .isEqualTo(CodigoDespensa.HOGAR_REQUERIDO);
        assertThat(codigoDelError(() -> servicio().listar(null, null)))
            .isEqualTo(CodigoDespensa.HOGAR_REQUERIDO);
    }

    // ---------- Listar ----------

    @Test
    void sinProductosDevuelveUnaListaVaciaSinConsultarElCatalogo() {
        when(itemRepository.findByHogarId(HOGAR_ID)).thenReturn(List.of());

        assertThat(servicio().listar(null, null)).isEmpty();
        assertThat(catalogo.consultasPorIds).isEqualTo(0);
    }

    @Test
    void porVencerMuestraLosVencidosYLosProximosEmpezandoPorElQueVenceAntes() {
        when(itemRepository.findByHogarId(HOGAR_ID)).thenReturn(List.of(
            item(1L, "6", null, "2026-10-16"),   // huevos: vence en 2 días
            item(5L, "500", null, "2026-10-13"), // pollo: venció ayer
            item(4L, "250", null, "2026-12-01"), // fríjol: vigente
            item(2L, "500", null, null)));       // arroz: sin fecha

        List<ItemDespensaResponse> resultado = servicio().listar("POR_VENCER", null);

        assertThat(nombres(resultado)).isEqualTo(List.of("Pollo", "Huevos"));
        assertThat(resultado.get(0).estadoVencimiento()).isEqualTo(EstadoVencimiento.VENCIDO);
        assertThat(resultado.get(0).diasParaVencer()).isEqualTo(-1);
        assertThat(resultado.get(1).estadoVencimiento()).isEqualTo(EstadoVencimiento.POR_VENCER);
    }

    @Test
    void bajoMinimoMuestraLosQueLlegaronAlMinimoOQuedaronPorDebajo() {
        when(itemRepository.findByHogarId(HOGAR_ID)).thenReturn(List.of(
            item(3L, "250", "250", null),  // aceite: Poco y el mínimo es Poco
            item(2L, "1000", "250", null), // arroz: Lleno, mínimo Poco: bien
            item(1L, "4", "4", null),      // huevos: justo en el mínimo
            item(5L, "500", "200", null),  // pollo: por encima
            item(4L, "100", null, null))); // fríjol: bajo pero sin mínimo registrado

        List<ItemDespensaResponse> resultado = servicio().listar("BAJO_MINIMO", null);

        assertThat(nombres(resultado)).isEqualTo(List.of("Aceite vegetal", "Huevos"));
    }

    @Test
    void sinFiltroListaTodoOrdenadoPorCategoriaYNombre() {
        when(itemRepository.findByHogarId(HOGAR_ID)).thenReturn(List.of(
            item(5L, "500", null, null),   // PROTEINAS
            item(4L, "250", null, null),   // GRANOS: Fríjol
            item(2L, "500", null, null),   // GRANOS: Arroz
            item(3L, "500", null, null),   // ACEITES
            item(1L, "6", null, null)));   // LACTEOS

        assertThat(nombres(servicio().listar(null, null)))
            .isEqualTo(List.of("Aceite vegetal", "Arroz", "Fríjol", "Huevos", "Pollo"));
    }

    @Test
    void elBuscadorNoDistingueTildesNiMayusculas() {
        when(itemRepository.findByHogarId(HOGAR_ID)).thenReturn(List.of(
            item(4L, "250", null, null),
            item(2L, "500", null, null)));

        assertThat(nombres(servicio().listar("TODO", "FRIJOL"))).isEqualTo(List.of("Fríjol"));
    }

    @Test
    void unFiltroDesconocidoResponde400() {
        assertThat(codigoDelError(() -> servicio().listar("PROXIMOS", null)))
            .isEqualTo(CodigoDespensa.FILTRO_INVALIDO);
    }

    @Test
    void hoyEsElDiaDeColombiaYNoElDiaEnUtc() {
        // 14 de octubre, 9 p. m. en Bogotá: en UTC ya es el 15
        Clock nocheEnBogota = Clock.fixed(Instant.parse("2026-10-15T02:00:00Z"), BOGOTA);

        when(itemRepository.findByHogarId(HOGAR_ID)).thenReturn(List.of(
            item(1L, "6", null, "2026-10-14")));

        ItemDespensaResponse huevos = servicio(nocheEnBogota).listar(null, null).get(0);

        // Vence hoy: todavía no está vencido
        assertThat(huevos.estadoVencimiento()).isEqualTo(EstadoVencimiento.POR_VENCER);
        assertThat(huevos.diasParaVencer()).isEqualTo(0);
    }

    // ---------- Resumen ----------

    @Test
    void elResumenCoincideConLasPestanasDelListado() {
        when(itemRepository.findByHogarId(HOGAR_ID)).thenReturn(List.of(
            item(1L, "4", "4", "2026-10-16"),    // por vencer y bajo mínimo
            item(5L, "500", null, "2026-10-13"), // vencido
            item(2L, "1000", "250", null),       // bien
            item(3L, "250", "250", null)));      // bajo mínimo

        ResumenDespensaResponse resumen = servicio().resumen();

        assertThat(resumen.total()).isEqualTo(4);
        assertThat(resumen.porVencer()).isEqualTo(2);
        assertThat(resumen.bajoMinimo()).isEqualTo(2);
    }

    // ---------- Consultar, editar y eliminar ----------

    @Test
    void unRegistroDeOtroHogarRespondeComoSiNoExistiera() {
        when(itemRepository.findByIdAndHogarId(7L, HOGAR_ID)).thenReturn(Optional.empty());

        assertThat(codigoDelError(() -> servicio().obtenerPorId(7L)))
            .isEqualTo(CodigoError.RECURSO_NO_ENCONTRADO);
        assertThat(codigoDelError(() -> servicio().actualizar(7L, edicionContable("5"))))
            .isEqualTo(CodigoError.RECURSO_NO_ENCONTRADO);
        assertThat(codigoDelError(() -> servicio().eliminar(7L)))
            .isEqualTo(CodigoError.RECURSO_NO_ENCONTRADO);

        verify(itemRepository, never()).delete(any(ItemDespensa.class));
    }

    @Test
    void editaLaCantidadElMinimoElVencimientoYLaUbicacion() {
        ItemDespensa existente = item(1L, "6", null, null);
        when(itemRepository.findByIdAndHogarId(7L, HOGAR_ID)).thenReturn(Optional.of(existente));

        ItemDespensaResponse respuesta = servicio().actualizar(7L, new EdicionItemDespensaRequest(
            new BigDecimal("2"), null, new BigDecimal("4"), null,
            LocalDate.of(2026, 10, 15), "Nevera"));

        assertThat(existente.getCantidad()).isEqualByComparingTo("2");
        assertThat(existente.getCantidadMinima()).isEqualByComparingTo("4");
        assertThat(existente.getVenceEn()).isEqualTo(LocalDate.of(2026, 10, 15));
        assertThat(existente.getUbicacion()).isEqualTo("Nevera");
        assertThat(respuesta.bajoMinimo()).isTrue();
        assertThat(respuesta.diasParaVencer()).isEqualTo(1);
    }

    @Test
    void editarUnProductoAGranelPasaDeNivelYActualizaLaCantidadEstimada() {
        ItemDespensa existente = item(2L, "1000", "250", null);
        when(itemRepository.findByIdAndHogarId(7L, HOGAR_ID)).thenReturn(Optional.of(existente));

        ItemDespensaResponse respuesta = servicio().actualizar(7L, new EdicionItemDespensaRequest(
            null, NivelGranel.POCO, null, NivelGranel.POCO, null, null));

        assertThat(existente.getCantidad()).isEqualByComparingTo("250");
        assertThat(respuesta.nivel()).isEqualTo(NivelGranel.POCO);
        assertThat(respuesta.bajoMinimo()).isTrue();
    }

    @Test
    void editarSinLosOpcionalesLosDejaSinValor() {
        ItemDespensa existente = item(1L, "6", "4", "2026-10-20");
        when(itemRepository.findByIdAndHogarId(7L, HOGAR_ID)).thenReturn(Optional.of(existente));

        servicio().actualizar(7L, edicionContable("6"));

        assertThat(existente.getCantidadMinima()).isNull();
        assertThat(existente.getVenceEn()).isNull();
    }

    @Test
    void editarConDatosInvalidosNoCambiaElRegistro() {
        ItemDespensa existente = item(1L, "6", null, null);
        when(itemRepository.findByIdAndHogarId(7L, HOGAR_ID)).thenReturn(Optional.of(existente));

        // Un producto contable no se edita por nivel
        EdicionItemDespensaRequest conNivel = new EdicionItemDespensaRequest(
            new BigDecimal("2"), NivelGranel.POCO, null, null, null, null);

        assertThat(codigoDelError(() -> servicio().actualizar(7L, conNivel)))
            .isEqualTo(CodigoDespensa.NIVEL_NO_PERMITIDO);
        assertThat(existente.getCantidad()).isEqualByComparingTo("6");
    }

    @Test
    void eliminaElProductoDeLaDespensaDelHogar() {
        ItemDespensa existente = item(1L, "6", null, null);
        when(itemRepository.findByIdAndHogarId(7L, HOGAR_ID)).thenReturn(Optional.of(existente));

        servicio().eliminar(7L);

        verify(itemRepository).delete(existente);
    }

    // ---------- Apoyo ----------

    private DespensaService servicio() {
        return servicio(RELOJ);
    }

    private DespensaService servicio(Clock reloj) {
        return new DespensaService(itemRepository, catalogo, hogares, usuarioActualService, reloj);
    }

    private void guardarDevolviendoLoRecibido() {
        when(itemRepository.saveAndFlush(any(ItemDespensa.class)))
            .then(AdditionalAnswers.returnsFirstArg());
    }

    private ItemDespensa capturarGuardado() {
        ArgumentCaptor<ItemDespensa> captor = ArgumentCaptor.forClass(ItemDespensa.class);
        verify(itemRepository).saveAndFlush(captor.capture());
        return captor.getValue();
    }

    private static ItemDespensaRequest contableValido(Long productoId) {
        return new ItemDespensaRequest(productoId, new BigDecimal("6"), null, null, null, null, null);
    }

    private static EdicionItemDespensaRequest edicionContable(String cantidad) {
        return new EdicionItemDespensaRequest(new BigDecimal(cantidad), null, null, null, null, null);
    }

    /** Registro ya guardado de un producto, con cantidad, mínimo y vencimiento opcionales. */
    private static ItemDespensa item(Long productoId, String cantidad, String minimo, String vence) {
        return new ItemDespensa(HOGAR_ID, productoId, new DatosItemDespensa(
            new BigDecimal(cantidad),
            false,
            minimo == null ? null : new BigDecimal(minimo),
            vence == null ? null : LocalDate.parse(vence),
            null));
    }

    /** Simula el id que pone MySQL al guardar. */
    private static void conId(ItemDespensa item, Long id) {
        ReflectionTestUtils.setField(item, "id", id);
    }

    private static List<String> nombres(List<ItemDespensaResponse> respuestas) {
        return respuestas.stream().map(ItemDespensaResponse::nombre).toList();
    }

    /** Catálogo en memoria: todos los productos que recibe están disponibles para el hogar. */
    private static final class CatalogoEnMemoria implements CatalogoProductos {

        private final Map<Long, ProductoCatalogo> productos = new HashMap<>();
        int consultasPorIds = 0;

        CatalogoEnMemoria(ProductoCatalogo... disponibles) {
            for (ProductoCatalogo producto : disponibles) {
                productos.put(producto.id(), producto);
            }
        }

        @Override
        public Optional<ProductoCatalogo> buscarDisponible(Long productoId, Long hogarId) {
            return Optional.ofNullable(productos.get(productoId));
        }

        final Map<Long, Integer> vidaUtil = new HashMap<>();

        @Override
        public Optional<Integer> vidaUtilDias(Long productoId) {
            return Optional.ofNullable(vidaUtil.get(productoId));
        }

        @Override
        public List<ProductoCatalogo> listarDisponibles(Long hogarId) {
            return List.copyOf(productos.values());
        }

        private long siguienteId = 1000L;

        @Override
        public ProductoCatalogo registrarAdicional(Long hogarId, String nombre, String categoria,
            TipoCantidad tipoCantidad, UnidadBase unidadBase, BigDecimal capacidadReferencia) {
            ProductoCatalogo nuevo = new ProductoCatalogo(siguienteId++, nombre, categoria, tipoCantidad,
                unidadBase, capacidadReferencia, null, null, null);
            productos.put(nuevo.id(), nuevo);
            return nuevo;
        }

        @Override
        public Map<Long, ProductoCatalogo> buscarPorIds(Collection<Long> productoIds) {
            consultasPorIds++;
            Map<Long, ProductoCatalogo> encontrados = new HashMap<>();
            for (Long id : productoIds) {
                if (productos.containsKey(id)) {
                    encontrados.put(id, productos.get(id));
                }
            }
            return encontrados;
        }
    }

    /** Hogares en memoria: qué usuaria tiene qué hogar. */
    private static final class HogarEnMemoria implements HogarDeUsuaria {

        private final Map<Long, Long> hogarPorUsuaria = new HashMap<>();

        void asignar(Long usuariaId, Long hogarId) {
            hogarPorUsuaria.put(usuariaId, hogarId);
        }

        void quitar(Long usuariaId) {
            hogarPorUsuaria.remove(usuariaId);
        }

        @Override
        public Optional<Long> hogarIdDe(Long usuarioId) {
            return Optional.ofNullable(hogarPorUsuaria.get(usuarioId));
        }
    }
}
