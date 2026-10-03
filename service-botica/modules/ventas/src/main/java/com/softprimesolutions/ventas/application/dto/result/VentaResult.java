package com.softprimesolutions.ventas.application.dto.result;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record VentaResult(
        UUID id,
        String numeroOperacion,
        UUID terminalId,
        UUID turnoId,
        UUID establecimientoId,
        UUID vendedorId,
        Instant fechaVenta,
        String moneda,
        BigDecimal subtotal,
        BigDecimal descuentoTotal,
        BigDecimal impuestoTotal,
        BigDecimal total,
        String estado,
        List<LineaVentaResult> lineas,
        PagoResult pago) {
}
