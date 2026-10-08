package com.softprimesolutions.compras.api.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record LineaOrdenCompraResponse(
        int numeroLinea,
        UUID skuId,
        String descripcion,
        BigDecimal cantidad,
        String unidadMedidaCodigo,
        BigDecimal precioUnitario,
        BigDecimal descuento,
        BigDecimal impuesto,
        BigDecimal totalLinea,
        BigDecimal toleranciaExcesoPct,
        BigDecimal toleranciaDefectoPct,
        BigDecimal cantidadRecibida,
        BigDecimal cantidadPendiente) {
}
