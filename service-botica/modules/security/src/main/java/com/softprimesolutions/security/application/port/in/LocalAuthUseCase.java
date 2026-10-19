package com.softprimesolutions.security.application.port.in;

import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import com.softprimesolutions.shared.kernel.result.Unit;
import java.time.Instant;
import java.util.UUID;

public interface LocalAuthUseCase {

    Result<TokenResult, ApplicationError> login(LoginCommand command);

    Result<TokenResult, ApplicationError> refresh(String refreshToken);

    Result<Unit, ApplicationError> logout(UUID tenantId, UUID userId, UUID sessionId);

    Result<Unit, ApplicationError> requestPasswordReset(PasswordResetRequest command);

    Result<Unit, ApplicationError> resetPassword(ResetPasswordCommand command);

    Result<Unit, ApplicationError> changePassword(ChangePasswordCommand command);

    Result<Unit, ApplicationError> provisionCredential(ProvisionCredentialCommand command);

    record LoginCommand(
            String login, String password, String channel,
            String ipAddress, String userAgent, UUID deviceId) {
    }

    record PasswordResetRequest(String login) {
    }

    record ResetPasswordCommand(String resetToken, String newPassword) {
    }

    record ChangePasswordCommand(UUID tenantId, UUID userId, String currentPassword, String newPassword) {
    }

    record ProvisionCredentialCommand(
            UUID tenantId, UUID userId, String password, boolean requireChange) {
    }

    record TokenResult(
            String accessToken, String refreshToken, String tokenType,
            Instant accessExpiresAt, Instant refreshExpiresAt,
            UUID tenantId, UUID userId, UUID sessionId, boolean passwordChangeRequired) {
    }
}
