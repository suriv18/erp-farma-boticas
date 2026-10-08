package com.softprimesolutions.compras.application.dto.result;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record OrdenCompraResumenResult(
        UUID id,
        String numero,
        UUID proveedorId,
        String proveedorRazonSocial,
        UUID establecimientoDestinoId,
        LocalDate fechaEmision,
        LocalDate fechaEntregaEstimada,
        String moneda,
        BigDecimal total,
        String estado) {
}
