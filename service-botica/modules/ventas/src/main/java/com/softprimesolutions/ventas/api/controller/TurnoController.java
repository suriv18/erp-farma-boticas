package com.softprimesolutions.ventas.api.controller;

import com.softprimesolutions.ventas.api.dto.request.AbrirTurnoRequest;
import com.softprimesolutions.ventas.api.dto.request.CerrarTurnoRequest;
import com.softprimesolutions.ventas.api.mapper.VentasApiMapper;
import com.softprimesolutions.ventas.application.dto.query.ObtenerTurnoQuery;
import com.softprimesolutions.ventas.application.dto.query.TurnoActualQuery;
import com.softprimesolutions.ventas.application.port.in.AbrirTurnoUseCase;
import com.softprimesolutions.ventas.application.port.in.CerrarTurnoUseCase;
import com.softprimesolutions.ventas.application.port.in.ConsultarTurnosUseCase;
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
@RequestMapping(TurnoController.BASE_PATH)
public class TurnoController {

    static final String BASE_PATH = "/api/v1/ventas/turnos";

    private final AbrirTurnoUseCase openTurno;
    private final CerrarTurnoUseCase closeTurno;
    private final ConsultarTurnosUseCase queryTurnos;

    public TurnoController(
            AbrirTurnoUseCase openTurno, CerrarTurnoUseCase closeTurno, ConsultarTurnosUseCase queryTurnos) {
        this.openTurno = openTurno;
        this.closeTurno = closeTurno;
        this.queryTurnos = queryTurnos;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ventas.turnos.abrir')")
    public ResponseEntity<?> open(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody AbrirTurnoRequest request) {
        var command = VentasApiMapper.toCommand(
                VentasControllerSupport.tenantOf(jwt), VentasControllerSupport.actorOf(jwt), request);
        return openTurno.execute(command).fold(
                result -> ResponseEntity.status(HttpStatus.CREATED).body(VentasApiMapper.toResponse(result)),
                VentasControllerSupport::problem);
    }

    @GetMapping("/actual")
    @PreAuthorize("hasAuthority('ventas.turnos.consultar')")
    public ResponseEntity<?> current(@AuthenticationPrincipal Jwt jwt, @RequestParam UUID terminalId) {
        return queryTurnos.actual(new TurnoActualQuery(VentasControllerSupport.tenantOf(jwt), terminalId))
                .fold(result -> ResponseEntity.ok(VentasApiMapper.toResponse(result)),
                        VentasControllerSupport::problem);
    }

    @GetMapping("/{turnoId}")
    @PreAuthorize("hasAuthority('ventas.turnos.consultar')")
    public ResponseEntity<?> getById(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID turnoId) {
        return queryTurnos.obtener(new ObtenerTurnoQuery(VentasControllerSupport.tenantOf(jwt), turnoId))
                .fold(result -> ResponseEntity.ok(VentasApiMapper.toResponse(result)),
                        VentasControllerSupport::problem);
    }

    @PostMapping("/{turnoId}/cierre")
    @PreAuthorize("hasAuthority('ventas.turnos.cerrar')")
    public ResponseEntity<?> close(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID turnoId,
            @Valid @RequestBody CerrarTurnoRequest request) {
        var command = VentasApiMapper.toCommand(
                VentasControllerSupport.tenantOf(jwt), VentasControllerSupport.actorOf(jwt), turnoId, request);
        return closeTurno.execute(command).fold(
                result -> ResponseEntity.ok(VentasApiMapper.toResponse(result)), VentasControllerSupport::problem);
    }
}
