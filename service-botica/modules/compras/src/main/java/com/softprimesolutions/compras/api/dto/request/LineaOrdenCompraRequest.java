package com.softprimesolutions.compras.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record LineaOrdenCompraRequest(
        @NotNull UUID skuId,
        @NotNull @Positive BigDecimal cantidad,
        @NotBlank @Size(max = 30) String unidadMedidaCodigo,
        @NotNull @PositiveOrZero BigDecimal precioUnitario,
        @PositiveOrZero BigDecimal descuento,
        @PositiveOrZero BigDecimal impuesto,
        @PositiveOrZero BigDecimal toleranciaExcesoPct,
        @PositiveOrZero BigDecimal toleranciaDefectoPct) {
}
