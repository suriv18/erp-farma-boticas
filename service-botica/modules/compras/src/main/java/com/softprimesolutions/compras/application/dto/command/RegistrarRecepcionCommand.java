package com.softprimesolutions.compras.application.dto.command;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public record RegistrarRecepcionCommand(
        UUID tenantId,
        UUID actorId,
        String idempotencyKey,
        UUID ordenCompraId,
        UUID almacenId,
        String documentoProveedorTipo,
        String documentoProveedorSerie,
        String documentoProveedorNumero,
        String guiaRemisionRemitente,
        String guiaRemisionTransportista,
        BigDecimal temperaturaRecepcionC,
        BigDecimal humedadRelativaPct,
        String observacion,
        List<ItemRecepcionInput> items) {

    public RegistrarRecepcionCommand {
        items = Optional.ofNullable(items).map(List::copyOf).orElse(List.of());
    }
}
