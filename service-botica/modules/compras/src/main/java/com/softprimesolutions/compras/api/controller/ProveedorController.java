package com.softprimesolutions.compras.api.controller;

import com.softprimesolutions.compras.api.dto.request.CambiarEstadoProveedorRequest;
import com.softprimesolutions.compras.api.dto.request.ProveedorRequest;
import com.softprimesolutions.compras.api.mapper.ComprasApiMapper;
import com.softprimesolutions.compras.application.dto.command.ActualizarProveedorCommand;
import com.softprimesolutions.compras.application.dto.command.CambiarEstadoProveedorCommand;
import com.softprimesolutions.compras.application.dto.command.CrearProveedorCommand;
import com.softprimesolutions.compras.application.dto.query.ListarProveedoresQuery;
import com.softprimesolutions.compras.application.dto.query.ObtenerProveedorQuery;
import com.softprimesolutions.compras.application.port.in.ActualizarProveedorUseCase;
import com.softprimesolutions.compras.application.port.in.CambiarEstadoProveedorUseCase;
import com.softprimesolutions.compras.application.port.in.ConsultarProveedoresUseCase;
import com.softprimesolutions.compras.application.port.in.CrearProveedorUseCase;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ProveedorController.BASE_PATH)
public class ProveedorController {

    static final String BASE_PATH = "/api/v1/compras/proveedores";

    private final CrearProveedorUseCase createProveedor;
    private final ActualizarProveedorUseCase updateProveedor;
    private final CambiarEstadoProveedorUseCase changeEstado;
    private final ConsultarProveedoresUseCase queryProveedores;

    public ProveedorController(
            CrearProveedorUseCase createProveedor, ActualizarProveedorUseCase updateProveedor,
            CambiarEstadoProveedorUseCase changeEstado, ConsultarProveedoresUseCase queryProveedores) {
        this.createProveedor = createProveedor;
        this.updateProveedor = updateProveedor;
        this.changeEstado = changeEstado;
        this.queryProveedores = queryProveedores;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('compras.proveedores.gestionar')")
    public ResponseEntity<?> create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ProveedorRequest request) {
        var command = new CrearProveedorCommand(
                ComprasControllerSupport.tenantOf(jwt), ComprasControllerSupport.actorOf(jwt),
                ComprasApiMapper.toInput(request));
        return createProveedor.execute(command).fold(
                result -> ResponseEntity.status(HttpStatus.CREATED).body(ComprasApiMapper.toResponse(result)),
                ComprasControllerSupport::problem);
    }

    @PutMapping("/{proveedorId}")
    @PreAuthorize("hasAuthority('compras.proveedores.gestionar')")
    public ResponseEntity<?> update(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID proveedorId,
            @Valid @RequestBody ProveedorRequest request) {
        var command = new ActualizarProveedorCommand(
                ComprasControllerSupport.tenantOf(jwt), proveedorId, ComprasControllerSupport.actorOf(jwt),
                ComprasApiMapper.toInput(request));
        return updateProveedor.execute(command).fold(
                result -> ResponseEntity.ok(ComprasApiMapper.toResponse(result)), ComprasControllerSupport::problem);
    }

    @PatchMapping("/{proveedorId}/estado")
    @PreAuthorize("hasAuthority('compras.proveedores.gestionar')")
    public ResponseEntity<?> changeEstado(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID proveedorId,
            @Valid @RequestBody CambiarEstadoProveedorRequest request) {
        var command = new CambiarEstadoProveedorCommand(
                ComprasControllerSupport.tenantOf(jwt), proveedorId, request.estado(),
                ComprasControllerSupport.actorOf(jwt));
        return changeEstado.execute(command).fold(
                result -> ResponseEntity.ok(ComprasApiMapper.toResponse(result)), ComprasControllerSupport::problem);
    }

    @GetMapping("/{proveedorId}")
    @PreAuthorize("hasAuthority('compras.proveedores.consultar')")
    public ResponseEntity<?> getById(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID proveedorId) {
        return queryProveedores.obtener(new ObtenerProveedorQuery(ComprasControllerSupport.tenantOf(jwt), proveedorId))
                .fold(result -> ResponseEntity.ok(ComprasApiMapper.toResponse(result)),
                        ComprasControllerSupport::problem);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('compras.proveedores.consultar')")
    public ResponseEntity<?> list(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String texto,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var query = new ListarProveedoresQuery(ComprasControllerSupport.tenantOf(jwt), estado, texto, page, size);
        return queryProveedores.listar(query).fold(
                result -> ResponseEntity.ok(ComprasApiMapper.toProveedorPage(result)),
                ComprasControllerSupport::problem);
    }
}
