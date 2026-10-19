package com.softprimesolutions.security.infrastructure.persistence.read.projection;

import java.util.List;
import java.util.UUID;

public record PerfilProjection(
        UUID userId,
        UUID tenantId,
        String displayName,
        String username,
        String email,
        List<Asignacion> assignments) {

    public record Asignacion(String roleCode, String roleName, String scopeType, String scopeName) {
    }
}
