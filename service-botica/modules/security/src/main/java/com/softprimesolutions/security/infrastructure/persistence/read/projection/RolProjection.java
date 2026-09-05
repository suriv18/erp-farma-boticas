package com.softprimesolutions.security.infrastructure.persistence.read.projection;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record RolProjection(
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

    public RolProjection {
        permissionCodes = Set.copyOf(permissionCodes);
    }
}
