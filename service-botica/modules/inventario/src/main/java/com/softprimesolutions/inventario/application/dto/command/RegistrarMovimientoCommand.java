package com.softprimesolutions.inventario.application.dto.command;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record RegistrarMovimientoCommand(
        UUID tenantId,
        UUID almacenId,
        UUID skuId,
        UUID loteId,
        String numeroLote,
        LocalDate fechaVencimiento,
        String tipo,
        BigDecimal cantidad,
        String motivo,
        UUID actorId,
        String idempotencyKey,
        DocumentoOrigen origen) {

    public RegistrarMovimientoCommand(
            UUID tenantId, UUID almacenId, UUID skuId, UUID loteId, String numeroLote,
            LocalDate fechaVencimiento, String tipo, BigDecimal cantidad, String motivo, UUID actorId,
            String idempotencyKey) {
        this(tenantId, almacenId, skuId, loteId, numeroLote, fechaVencimiento, tipo, cantidad, motivo, actorId,
                idempotencyKey, null);
    }
}
