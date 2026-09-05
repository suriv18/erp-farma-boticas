package com.softprimesolutions.security.api.dto.response;

import java.time.Instant;
import java.util.UUID;

public record AsignacionRolResponse(
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
