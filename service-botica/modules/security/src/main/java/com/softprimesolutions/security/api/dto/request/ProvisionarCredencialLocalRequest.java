package com.softprimesolutions.security.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record ProvisionarCredencialLocalRequest(
        @NotNull UUID tenantId,
        @NotBlank @Size(max = 128) String password,
        Boolean requireChange) {

    public boolean requireChangeOrDefault() {
        return requireChange == null || requireChange;
    }
}
