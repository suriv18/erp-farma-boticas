package com.softprimesolutions.compras.application.dto.command;

import java.math.BigDecimal;
import java.util.UUID;

public record LineaOrdenCompraInput(
        UUID skuId,
        BigDecimal cantidad,
        String unidadMedidaCodigo,
        BigDecimal precioUnitario,
        BigDecimal descuento,
        BigDecimal impuesto,
        BigDecimal toleranciaExcesoPct,
        BigDecimal toleranciaDefectoPct) {
}
