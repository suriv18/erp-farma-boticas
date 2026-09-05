package com.softprimesolutions.security.application.dto.result;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record RolResult(
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

    public RolResult {
        permissionCodes = Set.copyOf(permissionCodes);
    }
}
