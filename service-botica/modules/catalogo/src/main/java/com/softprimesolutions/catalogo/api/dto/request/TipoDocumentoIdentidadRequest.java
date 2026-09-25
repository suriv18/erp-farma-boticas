package com.softprimesolutions.catalogo.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record TipoDocumentoIdentidadRequest(
        @NotBlank @Size(max = 2) String codigo,
        @NotBlank @Size(max = 30) String sigla,
        @NotBlank @Size(min = 2, max = 200) String denominacion,
        @Positive Integer max,
        @Positive Integer min) {
}
