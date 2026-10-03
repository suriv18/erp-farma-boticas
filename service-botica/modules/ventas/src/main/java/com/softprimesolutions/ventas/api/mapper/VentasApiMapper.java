package com.softprimesolutions.ventas.api.mapper;

import com.softprimesolutions.ventas.api.dto.request.AbrirTurnoRequest;
import com.softprimesolutions.ventas.api.dto.request.CerrarTurnoRequest;
import com.softprimesolutions.ventas.api.dto.response.TurnoResponse;
import com.softprimesolutions.ventas.application.dto.command.AbrirTurnoCommand;
import com.softprimesolutions.ventas.application.dto.command.CerrarTurnoCommand;
import com.softprimesolutions.ventas.application.dto.result.TurnoResult;
import java.util.UUID;

public final class VentasApiMapper {

    private VentasApiMapper() {
    }

    public static AbrirTurnoCommand toCommand(UUID tenantId, UUID actorId, AbrirTurnoRequest request) {
        return new AbrirTurnoCommand(tenantId, actorId, request.terminalId(), request.fondoInicial());
    }

    public static CerrarTurnoCommand toCommand(
            UUID tenantId, UUID actorId, UUID turnoId, CerrarTurnoRequest request) {
        return new CerrarTurnoCommand(
                tenantId, actorId, turnoId, request.totalDeclarado(), request.observacion());
    }

    public static TurnoResponse toResponse(TurnoResult result) {
        return new TurnoResponse(
                result.id(), result.terminalId(), result.establecimientoId(), result.cajeroId(),
                result.aperturaAt(), result.fondoInicial(), result.estado(), result.cierreAt(),
                result.totalVentasSistema(), result.totalSistema(), result.totalDeclarado(), result.diferencia(),
                result.observacionCierre());
    }
}
