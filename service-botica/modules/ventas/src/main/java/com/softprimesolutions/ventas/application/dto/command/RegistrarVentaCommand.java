package com.softprimesolutions.ventas.application.dto.command;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record RegistrarVentaCommand(
        UUID tenantId,
        UUID actorId,
        String idempotencyKey,
        UUID terminalId,
        UUID almacenId,
        List<LineaVentaInput> lineas,
        BigDecimal montoRecibido) {
}
