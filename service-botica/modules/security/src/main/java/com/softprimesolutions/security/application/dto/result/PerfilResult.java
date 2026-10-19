package com.softprimesolutions.security.application.dto.result;

import java.util.List;
import java.util.UUID;

public record PerfilResult(
        UUID userId,
        UUID tenantId,
        String displayName,
        String username,
        String email,
        List<Asignacion> assignments) {

    public record Asignacion(String roleCode, String roleName, String scopeType, String scopeName) {
    }
}
