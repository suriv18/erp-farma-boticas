package com.softprimesolutions.compras.api.controller;

import com.softprimesolutions.compras.api.dto.request.RecepcionRequest;
import com.softprimesolutions.compras.api.mapper.ComprasApiMapper;
import com.softprimesolutions.compras.application.dto.query.ListarRecepcionesOrdenQuery;
import com.softprimesolutions.compras.application.dto.query.ObtenerRecepcionQuery;
import com.softprimesolutions.compras.application.port.in.ConsultarRecepcionesUseCase;
import com.softprimesolutions.compras.application.port.in.RegistrarRecepcionUseCase;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(RecepcionController.BASE_PATH)
public class RecepcionController {

    static final String BASE_PATH = "/api/v1/compras/recepciones";

    private final RegistrarRecepcionUseCase registerRecepcion;
    private final ConsultarRecepcionesUseCase queryRecepciones;

    public RecepcionController(
            RegistrarRecepcionUseCase registerRecepcion, ConsultarRecepcionesUseCase queryRecepciones) {
        this.registerRecepcion = registerRecepcion;
        this.queryRecepciones = queryRecepciones;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('compras.recepciones.registrar')")
    public ResponseEntity<?> register(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody RecepcionRequest request) {
        var command = ComprasApiMapper.toCommand(
                ComprasControllerSupport.tenantOf(jwt), ComprasControllerSupport.actorOf(jwt), idempotencyKey,
                request);
        return registerRecepcion.execute(command).fold(
                result -> ResponseEntity.status(HttpStatus.CREATED).body(ComprasApiMapper.toResponse(result)),
                ComprasControllerSupport::problem);
    }

    @GetMapping("/{recepcionId}")
    @PreAuthorize("hasAuthority('compras.recepciones.consultar')")
    public ResponseEntity<?> getById(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID recepcionId) {
        return queryRecepciones.obtener(new ObtenerRecepcionQuery(ComprasControllerSupport.tenantOf(jwt), recepcionId))
                .fold(result -> ResponseEntity.ok(ComprasApiMapper.toResponse(result)),
                        ComprasControllerSupport::problem);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('compras.recepciones.consultar')")
    public ResponseEntity<?> listByOrden(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam UUID ordenCompraId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var query = new ListarRecepcionesOrdenQuery(
                ComprasControllerSupport.tenantOf(jwt), ordenCompraId, page, size);
        return queryRecepciones.listarPorOrden(query).fold(
                result -> ResponseEntity.ok(ComprasApiMapper.toRecepcionPage(result)),
                ComprasControllerSupport::problem);
    }
}
