package com.softprimesolutions.security.application.port.out;

import java.time.Instant;
import java.util.UUID;

public interface PasswordResetNotificationPort {

    void send(PasswordResetMessage message);

    record PasswordResetMessage(
            UUID tenantId, UUID userId, String destination, String resetToken, Instant expiresAt) {
    }
}
