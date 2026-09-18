package com.softprimesolutions.catalogo.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CambiarEstadoTenantRequest(@NotNull UUID tenantId, @NotBlank String status) {
}
