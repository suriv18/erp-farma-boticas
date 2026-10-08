package com.softprimesolutions.inventario.api;

import java.math.BigDecimal;
import java.util.UUID;

public record SalidaVentaSolicitud(
        UUID tenantId,
        UUID almacenId,
        UUID skuId,
        BigDecimal cantidad,
        UUID ventaId,
        UUID ventaLineaId,
        UUID actorId,
        String idempotencyKey) {
}
