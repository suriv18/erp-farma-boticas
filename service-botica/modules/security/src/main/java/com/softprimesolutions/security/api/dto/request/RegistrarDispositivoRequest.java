package com.softprimesolutions.security.api.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record RegistrarDispositivoRequest(
        @NotNull UUID tenantId,
        @NotNull UUID companyId,
        @NotNull UUID establishmentId,
        UUID terminalId,
        @Size(max = 300) String fingerprintHash,
        @Size(max = 300) String certificateThumbprint,
        @Size(max = 100) String agentVersion) {
}
