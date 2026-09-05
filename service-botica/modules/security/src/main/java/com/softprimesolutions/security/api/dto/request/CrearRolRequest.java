package com.softprimesolutions.security.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CrearRolRequest(
        @NotNull UUID tenantId,
        @NotBlank @Size(min = 3, max = 80) String code,
        @NotBlank @Size(min = 2, max = 150) String name,
        @Size(max = 500) String description,
        @NotBlank String roleType,
        Boolean systemRole) {
}
