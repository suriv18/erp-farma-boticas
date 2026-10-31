package com.softprimesolutions.ventas.application.dto.command;

import java.math.BigDecimal;
import java.util.UUID;

public record CerrarTurnoCommand(
        UUID tenantId, UUID actorId, UUID turnoId, BigDecimal totalDeclarado, String observacion) {
}
