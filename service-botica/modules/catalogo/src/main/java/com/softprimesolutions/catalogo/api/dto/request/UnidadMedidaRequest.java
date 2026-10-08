package com.softprimesolutions.catalogo.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UnidadMedidaRequest(
        @NotBlank @Size(max = 30) String codigo,
        @NotBlank @Size(min = 2, max = 150) String denominacion,
        @Size(max = 30) String simbolo,
        boolean permiteDecimal,
        @Size(max = 300) String fuente) {
}
