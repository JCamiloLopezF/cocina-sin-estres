package co.edu.uniquindio.cocinasinestres.despensa.controller;

import co.edu.uniquindio.cocinasinestres.despensa.dto.EdicionItemDespensaRequest;
import co.edu.uniquindio.cocinasinestres.despensa.dto.ItemDespensaRequest;
import co.edu.uniquindio.cocinasinestres.despensa.dto.ItemDespensaResponse;
import co.edu.uniquindio.cocinasinestres.despensa.dto.ResumenDespensaResponse;
import co.edu.uniquindio.cocinasinestres.despensa.service.DespensaService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * Despensa virtual del hogar de la usuaria autenticada.
 *
 * <p>No necesita {@code @PreAuthorize}: la cadena de seguridad ya exige una cuenta
 * autenticada en {@code /api/**} y la despensa se resuelve siempre desde el hogar de esa
 * cuenta, nunca desde un identificador que mande el cliente.</p>
 */
@RestController
@RequestMapping("/api/items-despensa")
public class DespensaController {

    private final DespensaService despensaService;

    public DespensaController(DespensaService despensaService) {
        this.despensaService = despensaService;
    }

    /**
     * @param filtro TODO (por defecto), POR_VENCER o BAJO_MINIMO. Se recibe como texto para
     *               que un valor desconocido responda 400 con un código estable
     * @param buscar texto a buscar en el nombre del producto
     */
    @GetMapping
    public List<ItemDespensaResponse> listar(
        @RequestParam(required = false) String filtro,
        @RequestParam(required = false) String buscar) {

        return despensaService.listar(filtro, buscar);
    }

    @GetMapping("/resumen")
    public ResumenDespensaResponse resumen() {
        return despensaService.resumen();
    }

    @GetMapping("/{id}")
    public ItemDespensaResponse obtenerPorId(@PathVariable Long id) {
        return despensaService.obtenerPorId(id);
    }

    @PostMapping
    public ResponseEntity<ItemDespensaResponse> agregar(@RequestBody ItemDespensaRequest request) {
        ItemDespensaResponse creado = despensaService.agregar(request);

        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(creado.id())
            .toUri();

        return ResponseEntity.created(ubicacion).body(creado);
    }

    @PutMapping("/{id}")
    public ItemDespensaResponse actualizar(
        @PathVariable Long id,
        @RequestBody EdicionItemDespensaRequest request) {

        return despensaService.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        despensaService.eliminar(id);

        return ResponseEntity.noContent().build();
    }
}
