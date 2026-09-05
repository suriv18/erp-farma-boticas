package com.softprimesolutions.security.application.port.out;

import java.time.Instant;
import java.util.UUID;

public interface AuthTokenPort {

    String issueAccessToken(AccessTokenClaims claims);

    String newOpaqueToken();

    String digest(String rawToken);

    record AccessTokenClaims(
            UUID tenantId, UUID userId, UUID sessionId, Instant issuedAt,
            Instant expiresAt, boolean passwordChangeRequired) {
    }
}
