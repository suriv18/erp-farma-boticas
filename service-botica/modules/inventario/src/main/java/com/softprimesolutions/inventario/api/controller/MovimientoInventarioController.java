package com.softprimesolutions.inventario.api.controller;

import com.softprimesolutions.inventario.api.dto.request.RegistrarMovimientoRequest;
import com.softprimesolutions.inventario.api.mapper.InventarioApiMapper;
import com.softprimesolutions.inventario.application.port.in.RegistrarMovimientoUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(MovimientoInventarioController.BASE_PATH)
public class MovimientoInventarioController {

    static final String BASE_PATH = "/api/v1/inventario/movimientos";

    private final RegistrarMovimientoUseCase registerMovimiento;

    public MovimientoInventarioController(RegistrarMovimientoUseCase registerMovimiento) {
        this.registerMovimiento = registerMovimiento;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('inventario.movimientos.registrar')")
    public ResponseEntity<?> register(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody RegistrarMovimientoRequest request) {
        var command = InventarioApiMapper.toCommand(
                InventarioControllerSupport.tenantOf(jwt), InventarioControllerSupport.actorOf(jwt),
                idempotencyKey, request);
        return registerMovimiento.execute(command).fold(
                result -> ResponseEntity.status(HttpStatus.CREATED).body(InventarioApiMapper.toResponse(result)),
                InventarioControllerSupport::problem);
    }
}
