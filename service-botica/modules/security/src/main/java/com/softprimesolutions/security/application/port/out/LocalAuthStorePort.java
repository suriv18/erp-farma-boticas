package com.softprimesolutions.security.application.port.out;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface LocalAuthStorePort {

    Optional<LocalAccount> findAccountByLogin(String login);

    Optional<LocalAccount> findAccountByUser(UUID tenantId, UUID userId);

    ProvisionOutcome provisionCredential(
            UUID tenantId, UUID userId, String passwordHash, boolean requireChange, Instant at);

    void recordLoginFailure(UUID tenantId, UUID userId, int maximumAttempts, Instant lockedUntil, Instant at);

    void recordLoginSuccess(UUID tenantId, UUID userId, Instant at);

    boolean createSessionAndRefresh(SessionData session, RefreshTokenData refresh);

    Optional<StoredRefreshToken> findRefreshToken(String tokenHash);

    boolean rotateRefreshToken(String currentHash, RefreshTokenData replacement, Instant at);

    boolean revokeSession(UUID tenantId, UUID userId, UUID sessionId, String reason, Instant at);

    void createPasswordReset(UUID tenantId, UUID userId, String tokenHash, Instant expiresAt, Instant at);

    boolean resetPassword(String tokenHash, String newPasswordHash, Instant at);

    boolean changePassword(UUID tenantId, UUID userId, String newPasswordHash, Instant at);

    boolean isSessionActive(UUID tenantId, UUID userId, UUID sessionId, Instant at);

    boolean isTrustedDevice(UUID tenantId, UUID deviceId);

    Set<String> findEffectivePermissions(UUID tenantId, UUID userId, Instant at);

    enum ProvisionOutcome { CREATED, UPDATED, USER_NOT_FOUND }

    record LocalAccount(
            UUID tenantId, UUID userId, String username, String email, String displayName,
            String userStatus, String credentialStatus, String passwordHash,
            int failedAttempts, Instant lockedUntil, boolean passwordChangeRequired,
            boolean mfaRequired) {
    }

    record SessionData(
            UUID tenantId, UUID userId, UUID sessionId, String channel, String ipAddress,
            String userAgent, UUID deviceId, Instant loginAt, Instant expiresAt) {
    }

    record RefreshTokenData(
            UUID id, UUID tenantId, UUID userId, UUID sessionId, UUID familyId,
            String tokenHash, Instant expiresAt, Instant createdAt) {
    }

    record StoredRefreshToken(
            UUID tenantId, UUID userId, UUID sessionId, UUID familyId,
            Instant expiresAt, Instant usedAt, Instant revokedAt) {
        public boolean activeAt(Instant at) {
            return usedAt == null && revokedAt == null && expiresAt.isAfter(at);
        }
    }
}
