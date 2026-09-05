package com.softprimesolutions.security.api.dto.response;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record RolResponse(
        UUID id,
        UUID tenantId,
        String code,
        String name,
        String description,
        String roleType,
        boolean systemRole,
        Set<String> permissionCodes,
        String status,
        Instant createdAt,
        Instant updatedAt) {

    public RolResponse {
        permissionCodes = Set.copyOf(permissionCodes);
    }
}
