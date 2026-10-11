package com.softprimesolutions.catalogo.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record RubroComercialRequest(
        @NotBlank @Size(min = 2, max = 50) String codigo,
        @NotBlank @Size(min = 2, max = 120) String nombre,
        @Size(max = 300) String descripcion,
        boolean esFarmaceutico,
        @PositiveOrZero int orden) {
}
