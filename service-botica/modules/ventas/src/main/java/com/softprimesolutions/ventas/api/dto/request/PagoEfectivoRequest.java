package com.softprimesolutions.ventas.api.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record PagoEfectivoRequest(@NotNull @DecimalMin("0.00") BigDecimal montoRecibido) {
}
