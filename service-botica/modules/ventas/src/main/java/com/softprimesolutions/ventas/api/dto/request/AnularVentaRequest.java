package com.softprimesolutions.ventas.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AnularVentaRequest(@NotBlank @Size(max = 500) String motivo) {
}
