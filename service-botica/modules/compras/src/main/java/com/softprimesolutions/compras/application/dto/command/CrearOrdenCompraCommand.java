package com.softprimesolutions.compras.application.dto.command;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public record CrearOrdenCompraCommand(
        UUID tenantId,
        UUID actorId,
        UUID proveedorId,
        UUID establecimientoDestinoId,
        LocalDate fechaEntregaEstimada,
        String moneda,
        BigDecimal tipoCambio,
        String condicionPago,
        Integer diasCredito,
        String observacion,
        List<LineaOrdenCompraInput> lineas) {

    public CrearOrdenCompraCommand {
        lineas = Optional.ofNullable(lineas).map(List::copyOf).orElse(List.of());
    }
}
