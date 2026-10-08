package com.softprimesolutions.catalogo.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PrincipioActivoRequest(
        @Size(max = 80) String codigoFuente,
        @NotBlank @Size(min = 2, max = 300) String denominacion,
        @Size(max = 300) String nombreNormalizado,
        @Size(max = 300) String fuente) {
}
