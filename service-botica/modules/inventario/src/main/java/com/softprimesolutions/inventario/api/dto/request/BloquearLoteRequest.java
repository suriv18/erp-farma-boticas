package com.softprimesolutions.inventario.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BloquearLoteRequest(@NotBlank @Size(max = 1000) String motivo) {
}
