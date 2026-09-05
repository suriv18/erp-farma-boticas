package com.softprimesolutions.security.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

public record AsignarRolUsuarioRequest(
        @NotNull UUID tenantId,
        @NotNull UUID roleId,
        @NotBlank String scopeType,
        UUID companyId,
        UUID establishmentId,
        UUID warehouseId,
        UUID terminalId,
        Instant validFrom,
        Instant validUntil) {
}
