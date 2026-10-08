package com.softprimesolutions.security.api.dto.response;

import java.time.Instant;
import java.util.UUID;

public record UsuarioResponse(
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
