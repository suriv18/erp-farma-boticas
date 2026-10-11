package com.softprimesolutions.catalogo.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MarcaRequest(
        @NotBlank @Size(min = 2, max = 50) String codigo,
        @NotBlank @Size(min = 2, max = 180) String nombre,
        @Size(max = 500) String descripcion) {
}
