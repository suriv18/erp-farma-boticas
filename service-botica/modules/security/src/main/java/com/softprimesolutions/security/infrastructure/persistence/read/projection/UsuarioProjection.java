package com.softprimesolutions.security.infrastructure.persistence.read.projection;

import java.time.Instant;
import java.util.UUID;

public record UsuarioProjection(
        UUID id,
        UUID tenantId,
        String documentType,
        String documentNumber,
        String firstNames,
        String lastNames,
        String username,
        String email,
        String displayName,
        String phone,
        boolean credentialChangeRequired,
        boolean mfaRequired,
        String status,
        Instant createdAt,
        Instant updatedAt) {
}
