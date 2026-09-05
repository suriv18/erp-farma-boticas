package com.softprimesolutions.security.application.usecase.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.softprimesolutions.security.application.port.in.LocalAuthUseCase.ChangePasswordCommand;
import com.softprimesolutions.security.application.port.in.LocalAuthUseCase.LoginCommand;
import com.softprimesolutions.security.application.port.in.LocalAuthUseCase.PasswordResetRequest;
import com.softprimesolutions.security.application.port.in.LocalAuthUseCase.ProvisionCredentialCommand;
import com.softprimesolutions.security.application.port.in.LocalAuthUseCase.ResetPasswordCommand;
import com.softprimesolutions.security.application.port.out.AuthTokenPort;
import com.softprimesolutions.security.application.port.out.LocalAuthStorePort;
import com.softprimesolutions.security.application.port.out.LocalAuthStorePort.LocalAccount;
import com.softprimesolutions.security.application.port.out.LocalAuthStorePort.ProvisionOutcome;
import com.softprimesolutions.security.application.port.out.LocalAuthStorePort.RefreshTokenData;
import com.softprimesolutions.security.application.port.out.LocalAuthStorePort.SessionData;
import com.softprimesolutions.security.application.port.out.LocalAuthStorePort.StoredRefreshToken;
import com.softprimesolutions.security.application.port.out.PasswordHashPort;
import com.softprimesolutions.security.application.port.out.PasswordResetNotificationPort;
import com.softprimesolutions.security.application.port.out.PasswordResetNotificationPort.PasswordResetMessage;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.port.IdentifierGenerator;
import com.softprimesolutions.shared.kernel.result.Result;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LocalAuthServiceTest {

    private static final UUID TENANT_ID = UUID.fromString("172e0f26-a765-46f3-841c-4a11407ccf5b");
    private static final UUID USER_ID = UUID.fromString("851ef2da-faea-4224-8207-eb822a7b3574");
    private static final UUID SESSION_ID = UUID.fromString("7c30e255-462c-41c7-b667-387d2d1f1d8d");
    private static final UUID FAMILY_ID = UUID.fromString("7160d3fb-9c7d-450c-96d1-a10eaa10aec0");
    private static final UUID DEVICE_ID = UUID.fromString("64a46e89-6938-464a-84f1-fc12d47ac847");
    private static final Instant NOW = Instant.parse("2026-09-03T20:00:00Z");
    private static final String CURRENT_PASSWORD = "CurrentPass!2026";

    private FakeStore store;
    private FakePasswords passwords;
    private FakeTokens tokens;
    private FakeNotifications notifications;
    private LocalAuthService service;

    @BeforeEach
    void setUp() {
        store = new FakeStore();
        passwords = new FakePasswords();
        tokens = new FakeTokens();
        notifications = new FakeNotifications();
        service = new LocalAuthService(store, passwords, tokens, notifications,
                new SequentialIds(), () -> NOW,
                new LocalAuthPolicy(Duration.ofMinutes(10), Duration.ofDays(7),
                        Duration.ofMinutes(30), 5, Duration.ofMinutes(15)));
    }

    @Test
    void logsInAndPersistsARefreshBackedSession() {
        store.accountByLogin = Optional.of(account("ACTIVO", "ACTIVA", null, true, false));
        var longIp = "1".repeat(120);
        var longAgent = "agent".repeat(250);

        var result = service.login(new LoginCommand(
                TENANT_ID, " local.admin ", CURRENT_PASSWORD, " web ", longIp, longAgent, null));

        assertTrue(result.isSuccess());
        var token = result.getOrElse(error -> null);
        assertEquals("jwt-1", token.accessToken());
        assertEquals("opaque-1", token.refreshToken());
        assertEquals("Bearer", token.tokenType());
        assertEquals(NOW.plus(Duration.ofMinutes(10)), token.accessExpiresAt());
        assertEquals(NOW.plus(Duration.ofDays(7)), token.refreshExpiresAt());
        assertTrue(token.passwordChangeRequired());
        assertEquals(1, store.loginSuccesses);
        assertEquals("WEB", store.createdSession.channel());
        assertEquals(100, store.createdSession.ipAddress().length());
        assertEquals(1000, store.createdSession.userAgent().length());
        assertEquals("digest:opaque-1", store.createdRefresh.tokenHash());
        assertEquals(store.createdSession.sessionId(), tokens.lastClaims.sessionId());
        assertTrue(tokens.lastClaims.passwordChangeRequired());
    }

    @Test
    void returnsTheSameGenericErrorForUnknownAndIncorrectCredentials() {
        var unknown = service.login(new LoginCommand(
                TENANT_ID, "unknown", "WrongPass!2026", "WEB", null, null, null));
        assertFailureCode(unknown, "AUTH_INVALID_CREDENTIALS");
        assertEquals(1, passwords.matchCalls);
        assertEquals(0, store.loginFailures);

        store.accountByLogin = Optional.of(account("ACTIVO", "ACTIVA", null, false, false));
        var incorrect = service.login(new LoginCommand(
                TENANT_ID, "local.admin", "WrongPass!2026", "WEB", null, null, null));
        assertFailureCode(incorrect, "AUTH_INVALID_CREDENTIALS");
        assertEquals(1, store.loginFailures);
        assertEquals(5, store.failureMaximumAttempts);
        assertEquals(NOW.plus(Duration.ofMinutes(15)), store.failureLockedUntil);
    }

    @Test
    void rejectsMissingCommandsAndRequiredValuesWithoutCallingPersistence() {
        assertFailureCode(service.login(null), "AUTH_REQUEST_INVALID");
        assertFailureCode(service.login(new LoginCommand(
                null, "user", CURRENT_PASSWORD, "WEB", null, null, null)), "AUTH_REQUEST_INVALID");
        assertFailureCode(service.refresh(" "), "AUTH_INVALID_CREDENTIALS");
        assertFailureCode(service.refresh("not-found"), "AUTH_INVALID_CREDENTIALS");
        assertFailureCode(service.requestPasswordReset(null), "AUTH_REQUEST_INVALID");
        assertFailureCode(service.resetPassword(null), "AUTH_RESET_TOKEN_INVALID");
        assertFailureCode(service.changePassword(null), "AUTH_REQUEST_INVALID");
        assertFailureCode(service.provisionCredential(null), "AUTH_REQUEST_INVALID");
        assertNull(store.createdSession);
        assertNull(store.changedPasswordHash);
    }

    @Test
    void defaultsBlankChannelToWebAndNormalizesEmptyRequestMetadata() {
        store.accountByLogin = Optional.of(account("ACTIVO", "ACTIVA", null, false, false));

        var result = service.login(new LoginCommand(
                TENANT_ID, "local.admin", CURRENT_PASSWORD, " ", " ", null, null));

        assertTrue(result.isSuccess());
        assertEquals("WEB", store.createdSession.channel());
        assertNull(store.createdSession.ipAddress());
        assertNull(store.createdSession.userAgent());
    }

    @Test
    void rejectsInvalidChannelAndFailsClosedForLockedInactiveOrMfaAccounts() {
        assertFailureCode(service.login(new LoginCommand(
                TENANT_ID, "user", CURRENT_PASSWORD, "UNKNOWN", null, null, null)), "AUTH_REQUEST_INVALID");

        store.accountByLogin = Optional.of(account(
                "ACTIVO", "ACTIVA", NOW.plusSeconds(1), false, false));
        assertFailureCode(loginWeb(), "AUTH_INVALID_CREDENTIALS");

        store.accountByLogin = Optional.of(account("SUSPENDIDO", "ACTIVA", null, false, false));
        assertFailureCode(loginWeb(), "AUTH_INVALID_CREDENTIALS");

        store.accountByLogin = Optional.of(account("ACTIVO", "ACTIVA", null, false, true));
        assertFailureCode(loginWeb(), "AUTH_INVALID_CREDENTIALS");
        assertNull(store.createdSession);
    }

    @Test
    void requiresATrustedDeviceForPosAndHandlesSessionPersistenceFailure() {
        store.accountByLogin = Optional.of(account("ACTIVO", "ACTIVA", null, false, false));
        assertFailureCode(service.login(new LoginCommand(
                TENANT_ID, "local.admin", CURRENT_PASSWORD, "POS", null, null, null)),
                "AUTH_INVALID_CREDENTIALS");

        store.trustedDevice = false;
        assertFailureCode(service.login(new LoginCommand(
                TENANT_ID, "local.admin", CURRENT_PASSWORD, "POS", null, null, DEVICE_ID)),
                "AUTH_INVALID_CREDENTIALS");

        store.trustedDevice = true;
        store.createSessionResult = false;
        assertFailureCode(service.login(new LoginCommand(
                TENANT_ID, "local.admin", CURRENT_PASSWORD, "POS", null, null, DEVICE_ID)),
                "AUTH_UNEXPECTED");
    }

    @Test
    void rotatesAnActiveRefreshTokenAndPreservesItsFamilyAndSession() {
        store.refreshToken = Optional.of(activeRefresh());
        store.accountByUser = Optional.of(account("ACTIVO", "ACTIVA", null, false, false));

        var result = service.refresh(" old-refresh ");

        assertTrue(result.isSuccess());
        var token = result.getOrElse(error -> null);
        assertEquals("opaque-1", token.refreshToken());
        assertEquals(SESSION_ID, token.sessionId());
        assertEquals(FAMILY_ID, store.rotatedRefresh.familyId());
        assertEquals("digest:old-refresh", store.rotatedFromHash);
        assertEquals("digest:opaque-1", store.rotatedRefresh.tokenHash());
        assertEquals(NOW.plus(Duration.ofDays(7)), store.rotatedRefresh.expiresAt());
    }

    @Test
    void revokesTheSessionForExpiredReusedOrRacingRefreshTokens() {
        store.refreshToken = Optional.of(new StoredRefreshToken(
                TENANT_ID, USER_ID, SESSION_ID, FAMILY_ID, NOW.minusSeconds(1), null, null));
        assertFailureCode(service.refresh("expired"), "AUTH_INVALID_CREDENTIALS");
        assertEquals("REUTILIZACION_O_EXPIRACION_REFRESH", store.revocationReason);

        store.refreshToken = Optional.of(activeRefresh());
        store.accountByUser = Optional.of(account("ACTIVO", "ACTIVA", null, false, false));
        store.rotateRefreshResult = false;
        assertFailureCode(service.refresh("racing-token"), "AUTH_INVALID_CREDENTIALS");
        assertEquals("REUTILIZACION_REFRESH", store.revocationReason);
    }

    @Test
    void refusesRefreshWhenTheAccountSessionOrMfaStateIsNoLongerValid() {
        store.refreshToken = Optional.of(activeRefresh());
        store.accountByUser = Optional.of(account("ACTIVO", "ACTIVA", null, false, true));
        assertFailureCode(service.refresh("refresh"), "AUTH_INVALID_CREDENTIALS");

        store.accountByUser = Optional.of(account("ACTIVO", "ACTIVA", null, false, false));
        store.sessionActive = false;
        assertFailureCode(service.refresh("refresh"), "AUTH_INVALID_CREDENTIALS");
        assertNull(store.rotatedRefresh);
    }

    @Test
    void createsOneTimePasswordRecoveryMaterialWithoutRevealingUnknownAccounts() {
        store.accountByLogin = Optional.of(account("ACTIVO", "ACTIVA", null, false, false));

        var existing = service.requestPasswordReset(new PasswordResetRequest(TENANT_ID, " local.admin "));

        assertTrue(existing.isSuccess());
        assertEquals("digest:opaque-1", store.resetTokenHash);
        assertEquals(NOW.plus(Duration.ofMinutes(30)), store.resetExpiresAt);
        assertEquals("local.admin@example.test", notifications.message.destination());
        assertEquals("opaque-1", notifications.message.resetToken());

        store.accountByLogin = Optional.empty();
        notifications.message = null;
        var unknown = service.requestPasswordReset(new PasswordResetRequest(TENANT_ID, "unknown"));
        assertTrue(unknown.isSuccess());
        assertNull(notifications.message);
        assertEquals(2, passwords.matchCalls);
    }

    @Test
    void resetsAPasswordOnlyWithAValidTokenAndStrongNewPassword() {
        var weak = service.resetPassword(new ResetPasswordCommand("reset", "weak"));
        assertFailureCode(weak, "AUTH_REQUEST_INVALID");
        assertNull(store.changedPasswordHash);

        store.resetPasswordResult = false;
        assertFailureCode(service.resetPassword(
                new ResetPasswordCommand("invalid-reset", "NewStrong!2026")), "AUTH_RESET_TOKEN_INVALID");

        store.resetPasswordResult = true;
        var changed = service.resetPassword(new ResetPasswordCommand(" valid-reset ", "NewStrong!2026"));
        assertTrue(changed.isSuccess());
        assertEquals("digest:valid-reset", store.consumedResetHash);
        assertEquals("encoded:NewStrong!2026", store.changedPasswordHash);
    }

    @Test
    void changesAPasswordOnlyWhenCurrentAndNewSecretsAreValidAndDifferent() {
        store.accountByUser = Optional.of(account("ACTIVO", "ACTIVA", null, false, false));

        assertFailureCode(service.changePassword(new ChangePasswordCommand(
                TENANT_ID, USER_ID, "WrongPass!2026", "NewStrong!2026")), "AUTH_INVALID_CREDENTIALS");
        assertFailureCode(service.changePassword(new ChangePasswordCommand(
                TENANT_ID, USER_ID, CURRENT_PASSWORD, CURRENT_PASSWORD)), "AUTH_REQUEST_INVALID");

        var result = service.changePassword(new ChangePasswordCommand(
                TENANT_ID, USER_ID, CURRENT_PASSWORD, "NewStrong!2026"));
        assertTrue(result.isSuccess());
        assertEquals("encoded:NewStrong!2026", store.changedPasswordHash);

        store.changePasswordResult = false;
        assertFailureCode(service.changePassword(new ChangePasswordCommand(
                TENANT_ID, USER_ID, CURRENT_PASSWORD, "AnotherPass!2026")), "AUTH_INVALID_CREDENTIALS");
    }

    @Test
    void enforcesThePasswordPolicyBeforeProvisioningCredentials() {
        var invalidPasswords = java.util.List.of(
                "Short!1A",
                "alllowercase!2026",
                "ALLUPPERCASE!2026",
                "NoDigitsHere!",
                "NoSymbols2026A",
                "A1!" + "a".repeat(126));

        invalidPasswords.forEach(password -> assertFailureCode(service.provisionCredential(
                new ProvisionCredentialCommand(TENANT_ID, USER_ID, password, true)), "AUTH_REQUEST_INVALID"));
        assertFailureCode(service.provisionCredential(
                new ProvisionCredentialCommand(TENANT_ID, USER_ID, null, true)), "AUTH_REQUEST_INVALID");
        assertEquals(0, store.provisionCalls);

        store.provisionOutcome = ProvisionOutcome.USER_NOT_FOUND;
        assertFailureCode(service.provisionCredential(new ProvisionCredentialCommand(
                TENANT_ID, USER_ID, "NewStrong!2026", true)), "AUTH_USER_NOT_FOUND");

        store.provisionOutcome = ProvisionOutcome.CREATED;
        assertTrue(service.provisionCredential(new ProvisionCredentialCommand(
                TENANT_ID, USER_ID, "NewStrong!2026", false)).isSuccess());
        assertEquals("encoded:NewStrong!2026", store.provisionedPasswordHash);
        assertFalse(store.provisionRequireChange);
    }

    @Test
    void reportsWhetherLogoutActuallyRevokedTheSession() {
        assertTrue(service.logout(TENANT_ID, USER_ID, SESSION_ID).isSuccess());
        assertEquals("LOGOUT", store.revocationReason);

        store.revokeSessionResult = false;
        assertFailureCode(service.logout(TENANT_ID, USER_ID, SESSION_ID), "AUTH_SESSION_NOT_FOUND");
    }

    private Result<?, ApplicationError> loginWeb() {
        return service.login(new LoginCommand(
                TENANT_ID, "local.admin", CURRENT_PASSWORD, "WEB", null, null, null));
    }

    private static LocalAccount account(
            String userStatus, String credentialStatus, Instant lockedUntil,
            boolean passwordChangeRequired, boolean mfaRequired) {
        return new LocalAccount(TENANT_ID, USER_ID, "local.admin", "local.admin@example.test",
                "Local Admin", userStatus, credentialStatus, "encoded:" + CURRENT_PASSWORD,
                0, lockedUntil, passwordChangeRequired, mfaRequired);
    }

    private static StoredRefreshToken activeRefresh() {
        return new StoredRefreshToken(
                TENANT_ID, USER_ID, SESSION_ID, FAMILY_ID, NOW.plusSeconds(300), null, null);
    }

    private static void assertFailureCode(Result<?, ApplicationError> result, String expectedCode) {
        assertTrue(result.isFailure());
        assertEquals(expectedCode, result.fold(value -> null, ApplicationError::code));
    }

    private static final class FakePasswords implements PasswordHashPort {
        private int matchCalls;

        @Override
        public String encode(CharSequence rawPassword) {
            return "encoded:" + rawPassword;
        }

        @Override
        public boolean matches(CharSequence rawPassword, String encodedPassword) {
            matchCalls++;
            return ("encoded:" + rawPassword).equals(encodedPassword);
        }
    }

    private static final class FakeTokens implements AuthTokenPort {
        private int jwtSequence;
        private int opaqueSequence;
        private AccessTokenClaims lastClaims;

        @Override
        public String issueAccessToken(AccessTokenClaims claims) {
            lastClaims = claims;
            return "jwt-" + ++jwtSequence;
        }

        @Override
        public String newOpaqueToken() {
            return "opaque-" + ++opaqueSequence;
        }

        @Override
        public String digest(String rawToken) {
            return "digest:" + rawToken;
        }
    }

    private static final class FakeNotifications implements PasswordResetNotificationPort {
        private PasswordResetMessage message;

        @Override
        public void send(PasswordResetMessage value) {
            message = value;
        }
    }

    private static final class SequentialIds implements IdentifierGenerator {
        private long sequence;

        @Override
        public UUID next() {
            return new UUID(0, ++sequence);
        }
    }

    private static final class FakeStore implements LocalAuthStorePort {
        private Optional<LocalAccount> accountByLogin = Optional.empty();
        private Optional<LocalAccount> accountByUser = Optional.empty();
        private Optional<StoredRefreshToken> refreshToken = Optional.empty();
        private ProvisionOutcome provisionOutcome = ProvisionOutcome.CREATED;
        private boolean createSessionResult = true;
        private boolean rotateRefreshResult = true;
        private boolean revokeSessionResult = true;
        private boolean resetPasswordResult = true;
        private boolean changePasswordResult = true;
        private boolean sessionActive = true;
        private boolean trustedDevice;
        private int loginFailures;
        private int loginSuccesses;
        private int failureMaximumAttempts;
        private int provisionCalls;
        private Instant failureLockedUntil;
        private SessionData createdSession;
        private RefreshTokenData createdRefresh;
        private RefreshTokenData rotatedRefresh;
        private String rotatedFromHash;
        private String revocationReason;
        private String resetTokenHash;
        private Instant resetExpiresAt;
        private String consumedResetHash;
        private String changedPasswordHash;
        private String provisionedPasswordHash;
        private boolean provisionRequireChange;

        @Override
        public Optional<LocalAccount> findAccountByLogin(UUID tenantId, String login) {
            return accountByLogin;
        }

        @Override
        public Optional<LocalAccount> findAccountByUser(UUID tenantId, UUID userId) {
            return accountByUser;
        }

        @Override
        public ProvisionOutcome provisionCredential(
                UUID tenantId, UUID userId, String passwordHash, boolean requireChange, Instant at) {
            provisionCalls++;
            provisionedPasswordHash = passwordHash;
            provisionRequireChange = requireChange;
            return provisionOutcome;
        }

        @Override
        public void recordLoginFailure(
                UUID tenantId, UUID userId, int maximumAttempts, Instant lockedUntil, Instant at) {
            loginFailures++;
            failureMaximumAttempts = maximumAttempts;
            failureLockedUntil = lockedUntil;
        }

        @Override
        public void recordLoginSuccess(UUID tenantId, UUID userId, Instant at) {
            loginSuccesses++;
        }

        @Override
        public boolean createSessionAndRefresh(SessionData session, RefreshTokenData refresh) {
            createdSession = session;
            createdRefresh = refresh;
            return createSessionResult;
        }

        @Override
        public Optional<StoredRefreshToken> findRefreshToken(String tokenHash) {
            return refreshToken;
        }

        @Override
        public boolean rotateRefreshToken(String currentHash, RefreshTokenData replacement, Instant at) {
            rotatedFromHash = currentHash;
            rotatedRefresh = replacement;
            return rotateRefreshResult;
        }

        @Override
        public boolean revokeSession(UUID tenantId, UUID userId, UUID sessionId, String reason, Instant at) {
            revocationReason = reason;
            return revokeSessionResult;
        }

        @Override
        public void createPasswordReset(
                UUID tenantId, UUID userId, String tokenHash, Instant expiresAt, Instant at) {
            resetTokenHash = tokenHash;
            resetExpiresAt = expiresAt;
        }

        @Override
        public boolean resetPassword(String tokenHash, String newPasswordHash, Instant at) {
            consumedResetHash = tokenHash;
            changedPasswordHash = newPasswordHash;
            return resetPasswordResult;
        }

        @Override
        public boolean changePassword(UUID tenantId, UUID userId, String newPasswordHash, Instant at) {
            changedPasswordHash = newPasswordHash;
            return changePasswordResult;
        }

        @Override
        public boolean isSessionActive(UUID tenantId, UUID userId, UUID sessionId, Instant at) {
            return sessionActive;
        }

        @Override
        public boolean isTrustedDevice(UUID tenantId, UUID deviceId) {
            return trustedDevice;
        }

        @Override
        public Set<String> findEffectivePermissions(UUID tenantId, UUID userId, Instant at) {
            return Set.of();
        }
    }
}
