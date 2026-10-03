package com.softprimesolutions.ventas.api.controller;

import com.softprimesolutions.ventas.api.dto.request.AnularVentaRequest;
import com.softprimesolutions.ventas.api.dto.request.VentaRequest;
import com.softprimesolutions.ventas.api.mapper.VentasApiMapper;
import com.softprimesolutions.ventas.application.dto.query.ListarVentasQuery;
import com.softprimesolutions.ventas.application.dto.query.ObtenerVentaQuery;
import com.softprimesolutions.ventas.application.port.in.AnularVentaUseCase;
import com.softprimesolutions.ventas.application.port.in.ConsultarVentasUseCase;
import com.softprimesolutions.ventas.application.port.in.RegistrarVentaUseCase;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
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
@RequestMapping(VentaController.BASE_PATH)
public class VentaController {

    static final String BASE_PATH = "/api/v1/ventas/ventas";

    private final RegistrarVentaUseCase registerVenta;
    private final AnularVentaUseCase annulVenta;
    private final ConsultarVentasUseCase queryVentas;

    public VentaController(
            RegistrarVentaUseCase registerVenta, AnularVentaUseCase annulVenta, ConsultarVentasUseCase queryVentas) {
        this.registerVenta = registerVenta;
        this.annulVenta = annulVenta;
        this.queryVentas = queryVentas;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ventas.ventas.registrar')")
    public ResponseEntity<?> register(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody VentaRequest request) {
        var command = VentasApiMapper.toCommand(
                VentasControllerSupport.tenantOf(jwt), VentasControllerSupport.actorOf(jwt), idempotencyKey, request);
        return registerVenta.execute(command).fold(
                result -> ResponseEntity.status(HttpStatus.CREATED).body(VentasApiMapper.toResponse(result)),
                VentasControllerSupport::problem);
    }

    @PostMapping("/{ventaId}/anulacion")
    @PreAuthorize("hasAuthority('ventas.ventas.anular')")
    public ResponseEntity<?> annul(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID ventaId,
            @Valid @RequestBody AnularVentaRequest request) {
        var command = VentasApiMapper.toCommand(
                VentasControllerSupport.tenantOf(jwt), VentasControllerSupport.actorOf(jwt), ventaId, request);
        return annulVenta.execute(command).fold(
                result -> ResponseEntity.ok(VentasApiMapper.toResponse(result)), VentasControllerSupport::problem);
    }

    @GetMapping("/{ventaId}")
    @PreAuthorize("hasAuthority('ventas.ventas.consultar')")
    public ResponseEntity<?> getById(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID ventaId) {
        return queryVentas.obtener(new ObtenerVentaQuery(VentasControllerSupport.tenantOf(jwt), ventaId))
                .fold(result -> ResponseEntity.ok(VentasApiMapper.toResponse(result)),
                        VentasControllerSupport::problem);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ventas.ventas.consultar')")
    public ResponseEntity<?> list(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) UUID establecimientoId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant hasta,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var query = new ListarVentasQuery(
                VentasControllerSupport.tenantOf(jwt), establecimientoId, desde, hasta, page, size);
        return queryVentas.listar(query)
                .fold(result -> ResponseEntity.ok(VentasApiMapper.toResponse(result)),
                        VentasControllerSupport::problem);
    }
}
