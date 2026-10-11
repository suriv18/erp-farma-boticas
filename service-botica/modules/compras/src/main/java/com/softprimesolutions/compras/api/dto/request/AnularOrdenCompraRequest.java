package com.softprimesolutions.compras.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AnularOrdenCompraRequest(@NotBlank @Size(max = 300) String motivo) {
}
