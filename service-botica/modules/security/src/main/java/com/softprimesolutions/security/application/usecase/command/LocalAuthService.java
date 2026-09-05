package com.softprimesolutions.security.application.usecase.command;

import com.softprimesolutions.security.application.port.in.LocalAuthUseCase;
import com.softprimesolutions.security.application.port.out.AuthTokenPort;
import com.softprimesolutions.security.application.port.out.AuthTokenPort.AccessTokenClaims;
import com.softprimesolutions.security.application.port.out.LocalAuthStorePort;
import com.softprimesolutions.security.application.port.out.LocalAuthStorePort.RefreshTokenData;
import com.softprimesolutions.security.application.port.out.LocalAuthStorePort.SessionData;
import com.softprimesolutions.security.application.port.out.PasswordHashPort;
import com.softprimesolutions.security.application.port.out.PasswordResetNotificationPort;
import com.softprimesolutions.security.application.port.out.PasswordResetNotificationPort.PasswordResetMessage;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.application.port.ClockPort;
import com.softprimesolutions.shared.application.port.IdentifierGenerator;
import com.softprimesolutions.shared.kernel.result.Result;
import com.softprimesolutions.shared.kernel.result.Unit;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class LocalAuthService implements LocalAuthUseCase {

    private static final Set<String> CHANNELS = Set.of("WEB", "POS", "MOBILE", "API", "BACKOFFICE", "ECOMMERCE");

    private final LocalAuthStorePort store;
    private final PasswordHashPort passwords;
    private final AuthTokenPort tokens;
    private final PasswordResetNotificationPort notifications;
    private final IdentifierGenerator identifiers;
    private final ClockPort clock;
    private final LocalAuthPolicy policy;
    private final String dummyPasswordHash;

    public LocalAuthService(
            LocalAuthStorePort store,
            PasswordHashPort passwords,
            AuthTokenPort tokens,
            PasswordResetNotificationPort notifications,
            IdentifierGenerator identifiers,
            ClockPort clock,
            LocalAuthPolicy policy) {
        this.store = store;
        this.passwords = passwords;
        this.tokens = tokens;
        this.notifications = notifications;
        this.identifiers = identifiers;
        this.clock = clock;
        this.policy = policy;
        this.dummyPasswordHash = passwords.encode("dummy-password-that-is-never-valid");
    }

    @Override
    public Result<TokenResult, ApplicationError> login(LoginCommand command) {
        if (command == null || command.tenantId() == null || blank(command.login()) || blank(command.password())) {
            return invalid("login", "Tenant, usuario y contrasena son obligatorios.");
        }
        var channel = normalizeChannel(command.channel());
        if (!CHANNELS.contains(channel)) return invalid("channel", "El canal de autenticacion no es valido.");

        var now = clock.now();
        var account = store.findAccountByLogin(command.tenantId(), command.login().trim());
        var encoded = account.map(LocalAuthStorePort.LocalAccount::passwordHash).orElse(dummyPasswordHash);
        var passwordMatches = passwords.matches(command.password(), encoded);
        if (account.isEmpty() || !passwordMatches) {
            account.ifPresent(value -> store.recordLoginFailure(
                    value.tenantId(), value.userId(), policy.maxFailedAttempts(),
                    now.plus(policy.lockDuration()), now));
            return invalidCredentials();
        }

        var value = account.get();
        if (!"ACTIVO".equals(value.userStatus()) || !"ACTIVA".equals(value.credentialStatus())
                || value.lockedUntil() != null && value.lockedUntil().isAfter(now)
                || value.mfaRequired()) {
            return invalidCredentials();
        }
        if ("POS".equals(channel)
                && (command.deviceId() == null || !store.isTrustedDevice(value.tenantId(), command.deviceId()))) {
            return invalidCredentials();
        }

        store.recordLoginSuccess(value.tenantId(), value.userId(), now);
        var sessionId = identifiers.next();
        var familyId = identifiers.next();
        var refreshId = identifiers.next();
        var refreshRaw = tokens.newOpaqueToken();
        var accessExpiresAt = now.plus(policy.accessTokenTtl());
        var refreshExpiresAt = now.plus(policy.refreshTokenTtl());
        var session = new SessionData(value.tenantId(), value.userId(), sessionId, channel,
                trim(command.ipAddress(), 100), trim(command.userAgent(), 1000), command.deviceId(),
                now, refreshExpiresAt);
        var refresh = new RefreshTokenData(refreshId, value.tenantId(), value.userId(), sessionId,
                familyId, tokens.digest(refreshRaw), refreshExpiresAt, now);
        if (!store.createSessionAndRefresh(session, refresh)) {
            return unexpected("No fue posible crear la sesion local.");
        }
        return Result.success(tokenResult(value, sessionId, refreshRaw, now, accessExpiresAt, refreshExpiresAt));
    }

    @Override
    public Result<TokenResult, ApplicationError> refresh(String refreshToken) {
        if (blank(refreshToken)) return invalidCredentials();
        var now = clock.now();
        var hash = tokens.digest(refreshToken.trim());
        var stored = store.findRefreshToken(hash);
        if (stored.isEmpty()) return invalidCredentials();
        var current = stored.get();
        if (!current.activeAt(now)) {
            store.revokeSession(current.tenantId(), current.userId(), current.sessionId(),
                    "REUTILIZACION_O_EXPIRACION_REFRESH", now);
            return invalidCredentials();
        }
        var account = store.findAccountByUser(current.tenantId(), current.userId());
        if (account.isEmpty() || !"ACTIVO".equals(account.get().userStatus())
                || !"ACTIVA".equals(account.get().credentialStatus())
                || account.get().mfaRequired()
                || !store.isSessionActive(current.tenantId(), current.userId(), current.sessionId(), now)) {
            return invalidCredentials();
        }

        var replacementRaw = tokens.newOpaqueToken();
        var replacementId = identifiers.next();
        var refreshExpiresAt = now.plus(policy.refreshTokenTtl());
        var replacement = new RefreshTokenData(replacementId, current.tenantId(), current.userId(),
                current.sessionId(), current.familyId(), tokens.digest(replacementRaw), refreshExpiresAt, now);
        if (!store.rotateRefreshToken(hash, replacement, now)) {
            store.revokeSession(current.tenantId(), current.userId(), current.sessionId(),
                    "REUTILIZACION_REFRESH", now);
            return invalidCredentials();
        }
        var accessExpiresAt = now.plus(policy.accessTokenTtl());
        return Result.success(tokenResult(
                account.get(), current.sessionId(), replacementRaw, now, accessExpiresAt, refreshExpiresAt));
    }

    @Override
    public Result<Unit, ApplicationError> logout(UUID tenantId, UUID userId, UUID sessionId) {
        return store.revokeSession(tenantId, userId, sessionId, "LOGOUT", clock.now())
                ? Result.success(Unit.INSTANCE)
                : Result.failure(new StandardApplicationError(
                        "AUTH_SESSION_NOT_FOUND", "La sesion ya no se encuentra activa.", ErrorCategory.NOT_FOUND));
    }

    @Override
    public Result<Unit, ApplicationError> requestPasswordReset(PasswordResetRequest command) {
        if (command == null || command.tenantId() == null || blank(command.login())) {
            return invalid("login", "Tenant y usuario son obligatorios.");
        }
        passwords.matches("dummy-password-that-is-never-valid", dummyPasswordHash);
        var account = store.findAccountByLogin(command.tenantId(), command.login().trim());
        if (account.isPresent() && account.get().email() != null
                && "ACTIVO".equals(account.get().userStatus())
                && "ACTIVA".equals(account.get().credentialStatus())) {
            var now = clock.now();
            var rawToken = tokens.newOpaqueToken();
            var expiresAt = now.plus(policy.passwordResetTtl());
            var value = account.get();
            store.createPasswordReset(value.tenantId(), value.userId(), tokens.digest(rawToken), expiresAt, now);
            notifications.send(new PasswordResetMessage(
                    value.tenantId(), value.userId(), value.email(), rawToken, expiresAt));
        }
        return Result.success(Unit.INSTANCE);
    }

    @Override
    public Result<Unit, ApplicationError> resetPassword(ResetPasswordCommand command) {
        if (command == null || blank(command.resetToken())) return invalidResetToken();
        var validation = validatePassword(command.newPassword());
        if (validation != null) return validation;
        var changed = store.resetPassword(
                tokens.digest(command.resetToken().trim()), passwords.encode(command.newPassword()), clock.now());
        return changed ? Result.success(Unit.INSTANCE) : invalidResetToken();
    }

    @Override
    public Result<Unit, ApplicationError> changePassword(ChangePasswordCommand command) {
        if (command == null || command.tenantId() == null || command.userId() == null
                || blank(command.currentPassword())) {
            return invalid("password", "La contrasena actual es obligatoria.");
        }
        var validation = validatePassword(command.newPassword());
        if (validation != null) return validation;
        var account = store.findAccountByUser(command.tenantId(), command.userId());
        if (account.isEmpty() || !passwords.matches(command.currentPassword(), account.get().passwordHash())) {
            return invalidCredentials();
        }
        if (passwords.matches(command.newPassword(), account.get().passwordHash())) {
            return invalid("newPassword", "La nueva contrasena debe ser diferente de la actual.");
        }
        return store.changePassword(
                        command.tenantId(), command.userId(), passwords.encode(command.newPassword()), clock.now())
                ? Result.success(Unit.INSTANCE)
                : invalidCredentials();
    }

    @Override
    public Result<Unit, ApplicationError> provisionCredential(ProvisionCredentialCommand command) {
        if (command == null || command.tenantId() == null || command.userId() == null) {
            return invalid("userId", "Tenant y usuario son obligatorios.");
        }
        var validation = validatePassword(command.password());
        if (validation != null) return validation;
        var outcome = store.provisionCredential(command.tenantId(), command.userId(),
                passwords.encode(command.password()), command.requireChange(), clock.now());
        if (outcome == LocalAuthStorePort.ProvisionOutcome.USER_NOT_FOUND) {
            return Result.failure(new StandardApplicationError(
                    "AUTH_USER_NOT_FOUND", "El usuario no existe en el tenant indicado.", ErrorCategory.NOT_FOUND));
        }
        return Result.success(Unit.INSTANCE);
    }

    private TokenResult tokenResult(
            LocalAuthStorePort.LocalAccount account,
            UUID sessionId,
            String refreshToken,
            Instant issuedAt,
            Instant accessExpiresAt,
            Instant refreshExpiresAt) {
        var jwt = tokens.issueAccessToken(new AccessTokenClaims(
                account.tenantId(), account.userId(), sessionId, issuedAt,
                accessExpiresAt, account.passwordChangeRequired()));
        return new TokenResult(jwt, refreshToken, "Bearer", accessExpiresAt, refreshExpiresAt,
                account.tenantId(), account.userId(), sessionId, account.passwordChangeRequired());
    }

    private static Result<Unit, ApplicationError> validatePassword(String password) {
        if (password == null || password.length() < 12 || password.length() > 128
                || password.chars().noneMatch(Character::isUpperCase)
                || password.chars().noneMatch(Character::isLowerCase)
                || password.chars().noneMatch(Character::isDigit)
                || password.chars().allMatch(Character::isLetterOrDigit)
                || password.getBytes(StandardCharsets.UTF_8).length > 512) {
            return invalid("password", "La contrasena debe tener entre 12 y 128 caracteres e incluir mayuscula, "
                    + "minuscula, numero y simbolo.");
        }
        return null;
    }

    private static String normalizeChannel(String channel) {
        return blank(channel) ? "WEB" : channel.trim().toUpperCase(Locale.ROOT);
    }

    private static String trim(String value, int maximum) {
        if (value == null || value.isBlank()) return null;
        var normalized = value.trim();
        return normalized.length() <= maximum ? normalized : normalized.substring(0, maximum);
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static <T> Result<T, ApplicationError> invalid(String field, String message) {
        return Result.failure(new StandardApplicationError(
                "AUTH_REQUEST_INVALID", message, ErrorCategory.VALIDATION, Map.of("field", field)));
    }

    private static <T> Result<T, ApplicationError> invalidCredentials() {
        return Result.failure(new StandardApplicationError(
                "AUTH_INVALID_CREDENTIALS", "Las credenciales no son validas.", ErrorCategory.UNAUTHORIZED));
    }

    private static <T> Result<T, ApplicationError> invalidResetToken() {
        return Result.failure(new StandardApplicationError(
                "AUTH_RESET_TOKEN_INVALID", "El token de recuperacion no es valido o ha expirado.",
                ErrorCategory.VALIDATION));
    }

    private static <T> Result<T, ApplicationError> unexpected(String message) {
        return Result.failure(new StandardApplicationError(
                "AUTH_UNEXPECTED", message, ErrorCategory.UNEXPECTED));
    }
}
