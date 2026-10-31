package com.softprimesolutions.ventas.api.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record AbrirTurnoRequest(
        @NotNull UUID terminalId,
        @NotNull @DecimalMin("0.00") BigDecimal fondoInicial) {
}
