package com.softprimesolutions.ventas.api.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record LineaVentaRequest(
        @NotNull UUID skuId,
        @NotNull @DecimalMin("0.0001") BigDecimal cantidad,
        @NotNull @DecimalMin("0.00") BigDecimal precioUnitario) {
}
