package com.softprimesolutions.ventas.api.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record LineaVentaResponse(
        int numeroLinea,
        UUID skuId,
        String descripcion,
        String unidadVentaCodigo,
        BigDecimal cantidad,
        BigDecimal precioUnitario,
        BigDecimal totalLinea,
        List<LoteConsumidoResponse> lotes) {
}
