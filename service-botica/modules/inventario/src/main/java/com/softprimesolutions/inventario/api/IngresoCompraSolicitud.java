package com.softprimesolutions.inventario.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record IngresoCompraSolicitud(
        UUID tenantId,
        UUID almacenId,
        UUID skuId,
        String numeroLote,
        LocalDate fechaVencimiento,
        BigDecimal cantidad,
        UUID recepcionId,
        UUID recepcionLineaId,
        UUID proveedorId,
        UUID actorId,
        String idempotencyKey) {
}
