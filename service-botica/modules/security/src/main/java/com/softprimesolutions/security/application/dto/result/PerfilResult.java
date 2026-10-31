package com.softprimesolutions.security.application.dto.result;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public record PerfilResult(
        UUID userId,
        UUID tenantId,
        String displayName,
        String username,
        String email,
        List<Asignacion> assignments,
        List<String> permissions) {

    public PerfilResult conPermisos(Collection<String> permisos) {
        return new PerfilResult(
                userId, tenantId, displayName, username, email, assignments, List.copyOf(permisos));
    }

    public record Asignacion(String roleCode, String roleName, String scopeType, String scopeName) {
    }
}
