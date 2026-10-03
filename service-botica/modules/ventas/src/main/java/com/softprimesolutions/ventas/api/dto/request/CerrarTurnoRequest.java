package com.softprimesolutions.ventas.api.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CerrarTurnoRequest(
        @NotNull @DecimalMin("0.00") BigDecimal totalDeclarado,
        @Size(max = 1000) String observacion) {
}
