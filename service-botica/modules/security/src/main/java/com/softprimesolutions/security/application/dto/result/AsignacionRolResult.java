package com.softprimesolutions.security.application.dto.result;

import java.time.Instant;
import java.util.UUID;

public record AsignacionRolResult(
        UUID id,
        UUID tenantId,
        UUID userId,
        UUID roleId,
        String scopeType,
        UUID companyId,
        UUID establishmentId,
        UUID warehouseId,
        UUID terminalId,
        Instant validFrom,
        Instant validUntil,
        String status,
        String createdBy,
        Instant createdAt) {
}
