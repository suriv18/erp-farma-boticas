package com.softprimesolutions.catalogo.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FormaFarmaceuticaRequest(
        @NotBlank @Size(max = 30) String codigo,
        @NotBlank @Size(min = 2, max = 200) String denominacion,
        @Size(max = 300) String fuente) {
}
