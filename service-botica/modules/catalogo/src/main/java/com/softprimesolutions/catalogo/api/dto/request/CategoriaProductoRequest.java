package com.softprimesolutions.catalogo.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CategoriaProductoRequest(
        @NotNull UUID tenantId,
        UUID categoriaPadreId,
        @NotBlank @Size(min = 2, max = 50) String codigo,
        @NotBlank @Size(min = 2, max = 180) String nombre,
        @Size(max = 500) String descripcion,
        int nivel,
        int orden) {
}
