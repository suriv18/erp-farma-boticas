package com.softprimesolutions.inventario.api.controller;

import com.softprimesolutions.inventario.api.dto.request.BloquearLoteRequest;
import com.softprimesolutions.inventario.api.mapper.InventarioApiMapper;
import com.softprimesolutions.inventario.application.dto.command.DesbloquearLoteCommand;
import com.softprimesolutions.inventario.application.dto.query.ObtenerLoteQuery;
import com.softprimesolutions.inventario.application.port.in.BloquearLoteUseCase;
import com.softprimesolutions.inventario.application.port.in.DesbloquearLoteUseCase;
import com.softprimesolutions.inventario.application.port.in.ObtenerLoteUseCase;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(LoteController.BASE_PATH)
public class LoteController {

    static final String BASE_PATH = "/api/v1/inventario/lotes";

    private final ObtenerLoteUseCase getLote;
    private final BloquearLoteUseCase blockLote;
    private final DesbloquearLoteUseCase unblockLote;

    public LoteController(
            ObtenerLoteUseCase getLote, BloquearLoteUseCase blockLote, DesbloquearLoteUseCase unblockLote) {
        this.getLote = getLote;
        this.blockLote = blockLote;
        this.unblockLote = unblockLote;
    }

    @GetMapping("/{loteId}")
    @PreAuthorize("hasAuthority('inventario.lotes.consultar')")
    public ResponseEntity<?> getById(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID loteId) {
        return getLote.execute(new ObtenerLoteQuery(InventarioControllerSupport.tenantOf(jwt), loteId)).fold(
                result -> ResponseEntity.ok(InventarioApiMapper.toResponse(result)),
                InventarioControllerSupport::problem);
    }

    @PostMapping("/{loteId}/bloqueos")
    @PreAuthorize("hasAuthority('inventario.lotes.bloquear')")
    public ResponseEntity<?> block(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID loteId,
            @Valid @RequestBody BloquearLoteRequest request) {
        var command = InventarioApiMapper.toCommand(
                InventarioControllerSupport.tenantOf(jwt), loteId, InventarioControllerSupport.actorOf(jwt), request);
        return blockLote.execute(command).fold(
                result -> ResponseEntity.ok(InventarioApiMapper.toResponse(result)),
                InventarioControllerSupport::problem);
    }

    @DeleteMapping("/{loteId}/bloqueos")
    @PreAuthorize("hasAuthority('inventario.lotes.bloquear')")
    public ResponseEntity<?> unblock(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID loteId) {
        var command = new DesbloquearLoteCommand(
                InventarioControllerSupport.tenantOf(jwt), loteId, InventarioControllerSupport.actorOf(jwt));
        return unblockLote.execute(command).fold(
                result -> ResponseEntity.ok(InventarioApiMapper.toResponse(result)),
                InventarioControllerSupport::problem);
    }
}
