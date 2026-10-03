package com.softprimesolutions.inventario.application.dto.command;

import java.math.BigDecimal;
import java.util.UUID;

public record RegistrarSalidaVentaCommand(
        UUID tenantId,
        UUID almacenId,
        UUID skuId,
        BigDecimal cantidad,
        UUID ventaId,
        UUID ventaLineaId,
        UUID actorId,
        String idempotencyKey) {
}
