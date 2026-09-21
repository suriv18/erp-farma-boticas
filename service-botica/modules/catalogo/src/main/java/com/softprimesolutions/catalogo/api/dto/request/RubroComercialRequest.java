package com.softprimesolutions.catalogo.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record RubroComercialRequest(
        @NotNull UUID tenantId,
        @NotBlank @Size(min = 2, max = 50) String codigo,
        @NotBlank @Size(min = 2, max = 120) String nombre,
        @Size(max = 300) String descripcion,
        boolean esFarmaceutico,
        @PositiveOrZero int orden) {
}
