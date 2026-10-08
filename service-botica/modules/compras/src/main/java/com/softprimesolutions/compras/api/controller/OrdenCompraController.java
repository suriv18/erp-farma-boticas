package com.softprimesolutions.compras.api.controller;

import com.softprimesolutions.compras.api.dto.request.AnularOrdenCompraRequest;
import com.softprimesolutions.compras.api.dto.request.OrdenCompraRequest;
import com.softprimesolutions.compras.api.mapper.ComprasApiMapper;
import com.softprimesolutions.compras.application.dto.command.TransicionOrden;
import com.softprimesolutions.compras.application.dto.command.TransicionarOrdenCompraCommand;
import com.softprimesolutions.compras.application.dto.query.ListarOrdenesCompraQuery;
import com.softprimesolutions.compras.application.dto.query.ObtenerOrdenCompraQuery;
import com.softprimesolutions.compras.application.port.in.ConsultarOrdenesCompraUseCase;
import com.softprimesolutions.compras.application.port.in.CrearOrdenCompraUseCase;
import com.softprimesolutions.compras.application.port.in.TransicionarOrdenCompraUseCase;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(OrdenCompraController.BASE_PATH)
public class OrdenCompraController {

    static final String BASE_PATH = "/api/v1/compras/ordenes";

    private final CrearOrdenCompraUseCase createOrden;
    private final TransicionarOrdenCompraUseCase transitionOrden;
    private final ConsultarOrdenesCompraUseCase queryOrdenes;

    public OrdenCompraController(
            CrearOrdenCompraUseCase createOrden, TransicionarOrdenCompraUseCase transitionOrden,
            ConsultarOrdenesCompraUseCase queryOrdenes) {
        this.createOrden = createOrden;
        this.transitionOrden = transitionOrden;
        this.queryOrdenes = queryOrdenes;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('compras.ordenes.crear')")
    public ResponseEntity<?> create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody OrdenCompraRequest request) {
        var command = ComprasApiMapper.toCommand(
                ComprasControllerSupport.tenantOf(jwt), ComprasControllerSupport.actorOf(jwt), request);
        return createOrden.execute(command).fold(
                result -> ResponseEntity.status(HttpStatus.CREATED).body(ComprasApiMapper.toResponse(result)),
                ComprasControllerSupport::problem);
    }

    @GetMapping("/{ordenId}")
    @PreAuthorize("hasAuthority('compras.ordenes.consultar')")
    public ResponseEntity<?> getById(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID ordenId) {
        return queryOrdenes.obtener(new ObtenerOrdenCompraQuery(ComprasControllerSupport.tenantOf(jwt), ordenId))
                .fold(result -> ResponseEntity.ok(ComprasApiMapper.toResponse(result)),
                        ComprasControllerSupport::problem);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('compras.ordenes.consultar')")
    public ResponseEntity<?> list(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) UUID proveedorId,
            @RequestParam(required = false) String estado,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var query = new ListarOrdenesCompraQuery(
                ComprasControllerSupport.tenantOf(jwt), proveedorId, estado, page, size);
        return queryOrdenes.listar(query).fold(
                result -> ResponseEntity.ok(ComprasApiMapper.toOrdenPage(result)), ComprasControllerSupport::problem);
    }

    @PostMapping("/{ordenId}/aprobacion")
    @PreAuthorize("hasAuthority('compras.ordenes.aprobar')")
    public ResponseEntity<?> approve(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID ordenId) {
        return transition(jwt, ordenId, TransicionOrden.APROBAR, null);
    }

    @PostMapping("/{ordenId}/emision")
    @PreAuthorize("hasAuthority('compras.ordenes.aprobar')")
    public ResponseEntity<?> issue(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID ordenId) {
        return transition(jwt, ordenId, TransicionOrden.EMITIR, null);
    }

    @PostMapping("/{ordenId}/anulacion")
    @PreAuthorize("hasAuthority('compras.ordenes.anular')")
    public ResponseEntity<?> cancel(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID ordenId,
            @Valid @RequestBody AnularOrdenCompraRequest request) {
        return transition(jwt, ordenId, TransicionOrden.ANULAR, request.motivo());
    }

    private ResponseEntity<?> transition(Jwt jwt, UUID ordenId, TransicionOrden transicion, String motivo) {
        var command = new TransicionarOrdenCompraCommand(
                ComprasControllerSupport.tenantOf(jwt), ordenId, transicion, motivo,
                ComprasControllerSupport.actorOf(jwt));
        return transitionOrden.execute(command).fold(
                result -> ResponseEntity.ok(ComprasApiMapper.toResponse(result)), ComprasControllerSupport::problem);
    }
}
