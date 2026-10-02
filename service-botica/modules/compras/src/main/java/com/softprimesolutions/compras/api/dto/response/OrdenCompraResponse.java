package com.softprimesolutions.compras.api.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record OrdenCompraResponse(
        UUID id,
        String numero,
        UUID proveedorId,
        UUID establecimientoDestinoId,
        LocalDate fechaEmision,
        LocalDate fechaEntregaEstimada,
        String moneda,
        BigDecimal tipoCambio,
        String condicionPago,
        int diasCredito,
        BigDecimal subtotal,
        BigDecimal descuentoTotal,
        BigDecimal impuestoTotal,
        BigDecimal total,
        String estado,
        String observacion,
        Instant aprobadoAt,
        List<LineaOrdenCompraResponse> lineas) {

    public OrdenCompraResponse {
        lineas = List.copyOf(lineas);
    }
}
