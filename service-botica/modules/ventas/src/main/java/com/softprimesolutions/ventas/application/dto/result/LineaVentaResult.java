package com.softprimesolutions.ventas.application.dto.result;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record LineaVentaResult(
        int numeroLinea,
        UUID skuId,
        String descripcion,
        String unidadVentaCodigo,
        BigDecimal cantidad,
        BigDecimal precioUnitario,
        BigDecimal totalLinea,
        List<LoteConsumidoResult> lotes) {
}
