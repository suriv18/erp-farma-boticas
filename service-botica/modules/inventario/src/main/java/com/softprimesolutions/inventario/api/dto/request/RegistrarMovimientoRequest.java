package com.softprimesolutions.inventario.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record RegistrarMovimientoRequest(
        @NotNull UUID almacenId,
        @NotNull UUID skuId,
        UUID loteId,
        @Size(max = 120) String numeroLote,
        LocalDate fechaVencimiento,
        @NotBlank String tipo,
        @NotNull @Positive BigDecimal cantidad,
        @NotBlank @Size(max = 1000) String motivo) {
}
