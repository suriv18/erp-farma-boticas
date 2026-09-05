package com.softprimesolutions.security.api.dto.response;

import java.time.Instant;
import java.util.UUID;

public record AuthTokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        Instant accessExpiresAt,
        Instant refreshExpiresAt,
        UUID tenantId,
        UUID userId,
        UUID sessionId,
        boolean passwordChangeRequired) {
}
