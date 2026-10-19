package co.edu.uniquindio.cocinasinestres.admin.controller;

import co.edu.uniquindio.cocinasinestres.admin.dto.CambioCuentaRequest;
import co.edu.uniquindio.cocinasinestres.admin.dto.CuentaResponse;
import co.edu.uniquindio.cocinasinestres.comun.error.DatosInvalidosException;
import co.edu.uniquindio.cocinasinestres.perfil.service.CuentasService;
import co.edu.uniquindio.cocinasinestres.perfil.service.UsuarioActualService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Gestión de cuentas y roles (proceso 1, ADR-16). Solo el rol ADMIN: lo exige la
 * regla de {@code /api/admin/**} y, por si acaso, también esta anotación (RNF-23).
 */
@RestController
@RequestMapping("/api/admin/cuentas")
@PreAuthorize("hasRole('ADMIN')")
public class CuentasController {

    private final CuentasService cuentasService;
    private final UsuarioActualService usuarioActualService;

    public CuentasController(CuentasService cuentasService, UsuarioActualService usuarioActualService) {
        this.cuentasService = cuentasService;
        this.usuarioActualService = usuarioActualService;
    }

    @GetMapping
    public List<CuentaResponse> listar() {
        return cuentasService.listar().stream().map(CuentaResponse::de).toList();
    }

    @PutMapping("/{id}")
    public CuentaResponse cambiar(@PathVariable Long id, @RequestBody(required = false) CambioCuentaRequest cambio) {
        if (cambio == null) {
            throw new DatosInvalidosException(CuentasService.CODIGO_SIN_CAMBIOS,
                    "Indica el nuevo rol o si la cuenta queda activa.");
        }
        Long adminId = usuarioActualService.obtener().id();
        return CuentaResponse.de(cuentasService.cambiar(id, cambio.rol(), cambio.activo(), adminId));
    }
}
