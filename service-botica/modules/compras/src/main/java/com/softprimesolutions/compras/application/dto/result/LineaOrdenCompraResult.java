package com.softprimesolutions.compras.application.dto.result;

import java.math.BigDecimal;
import java.util.UUID;

public record LineaOrdenCompraResult(
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
