package com.softprimesolutions.security.api.controller;

import com.softprimesolutions.security.api.dto.request.CambiarPasswordRequest;
import com.softprimesolutions.security.api.dto.request.LoginRequest;
import com.softprimesolutions.security.api.dto.request.ProvisionarCredencialLocalRequest;
import com.softprimesolutions.security.api.dto.request.RefreshTokenRequest;
import com.softprimesolutions.security.api.dto.request.RestablecerPasswordRequest;
import com.softprimesolutions.security.api.dto.request.SolicitarRecuperacionPasswordRequest;
import com.softprimesolutions.security.api.dto.response.AuthTokenResponse;
import com.softprimesolutions.security.application.port.in.LocalAuthUseCase;
import com.softprimesolutions.security.application.port.in.LocalAuthUseCase.ChangePasswordCommand;
import com.softprimesolutions.security.application.port.in.LocalAuthUseCase.LoginCommand;
import com.softprimesolutions.security.application.port.in.LocalAuthUseCase.PasswordResetRequest;
import com.softprimesolutions.security.application.port.in.LocalAuthUseCase.ProvisionCredentialCommand;
import com.softprimesolutions.security.application.port.in.LocalAuthUseCase.ResetPasswordCommand;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1")
public class LocalAuthController {

    private final LocalAuthUseCase auth;

    public LocalAuthController(LocalAuthUseCase auth) {
        this.auth = auth;
    }

    @PostMapping("/auth/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        var command = new LoginCommand(
                request.login(), request.password(), request.channel(),
                httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent"), request.deviceId());
        return auth.login(command).fold(LocalAuthController::tokenResponse, IamControllerSupport::problem);
    }

    @PostMapping("/auth/refresh")
    public ResponseEntity<?> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return auth.refresh(request.refreshToken()).fold(
                LocalAuthController::tokenResponse, IamControllerSupport::problem);
    }

    @PostMapping("/auth/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> logout(@AuthenticationPrincipal Jwt jwt) {
        return auth.logout(tenantId(jwt), userId(jwt), sessionId(jwt)).fold(
                ignored -> ResponseEntity.noContent().build(), IamControllerSupport::problem);
    }

    @PostMapping("/auth/password/forgot")
    public ResponseEntity<?> requestPasswordReset(
            @Valid @RequestBody SolicitarRecuperacionPasswordRequest request) {
        return auth.requestPasswordReset(new PasswordResetRequest(request.login())).fold(
                ignored -> ResponseEntity.accepted().build(), IamControllerSupport::problem);
    }

    @PostMapping("/auth/password/reset")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody RestablecerPasswordRequest request) {
        return auth.resetPassword(new ResetPasswordCommand(request.resetToken(), request.newPassword())).fold(
                ignored -> ResponseEntity.noContent().build(), IamControllerSupport::problem);
    }

    @PostMapping("/auth/password/change")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> changePassword(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CambiarPasswordRequest request) {
        var command = new ChangePasswordCommand(
                tenantId(jwt), userId(jwt), request.currentPassword(), request.newPassword());
        return auth.changePassword(command).fold(
                ignored -> ResponseEntity.noContent().build(), IamControllerSupport::problem);
    }

    @PostMapping("/usuarios/{userId}/credencial-local")
    @PreAuthorize("hasAuthority('seguridad.credenciales.gestionar')")
    public ResponseEntity<?> provisionCredential(
            @PathVariable UUID userId,
            @Valid @RequestBody ProvisionarCredencialLocalRequest request) {
        var command = new ProvisionCredentialCommand(
                request.tenantId(), userId, request.password(), request.requireChangeOrDefault());
        return auth.provisionCredential(command).fold(
                ignored -> ResponseEntity.noContent().build(), IamControllerSupport::problem);
    }

    private static ResponseEntity<AuthTokenResponse> tokenResponse(LocalAuthUseCase.TokenResult result) {
        var body = new AuthTokenResponse(
                result.accessToken(), result.refreshToken(), result.tokenType(), result.accessExpiresAt(),
                result.refreshExpiresAt(), result.tenantId(), result.userId(), result.sessionId(),
                result.passwordChangeRequired());
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header("Pragma", "no-cache")
                .body(body);
    }

    private static UUID tenantId(Jwt jwt) {
        return UUID.fromString(jwt.getClaimAsString("tid"));
    }

    private static UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }

    private static UUID sessionId(Jwt jwt) {
        return UUID.fromString(jwt.getClaimAsString("sid"));
    }
}
