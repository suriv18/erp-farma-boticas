package com.softprimesolutions.security.api;

import java.time.Instant;
import java.util.UUID;

/** Evento confidencial para entregar un token de recuperacion por un canal verificado. */
public record PasswordResetRequested(
        UUID tenantId, UUID userId, String destination, String resetToken, Instant expiresAt) {
}
