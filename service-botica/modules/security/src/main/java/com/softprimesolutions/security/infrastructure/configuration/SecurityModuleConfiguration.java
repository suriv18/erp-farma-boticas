package com.softprimesolutions.security.infrastructure.configuration;

import com.softprimesolutions.security.application.port.in.AsignarRolUsuarioUseCase;
import com.softprimesolutions.security.application.port.in.CrearRolUseCase;
import com.softprimesolutions.security.application.port.in.CrearUsuarioUseCase;
import com.softprimesolutions.security.application.port.in.ListarPermisosUseCase;
import com.softprimesolutions.security.application.port.in.ListarRolesUseCase;
import com.softprimesolutions.security.application.port.in.ListarUsuariosUseCase;
import com.softprimesolutions.security.application.port.in.LocalAuthUseCase;
import com.softprimesolutions.security.application.port.in.ReemplazarPermisosRolUseCase;
import com.softprimesolutions.security.application.port.in.SecurityControlUseCase;
import com.softprimesolutions.security.application.port.out.IamReadPort;
import com.softprimesolutions.security.application.port.out.IamWritePort;
import com.softprimesolutions.security.application.port.out.AuthTokenPort;
import com.softprimesolutions.security.application.port.out.LocalAuthStorePort;
import com.softprimesolutions.security.application.port.out.PasswordHashPort;
import com.softprimesolutions.security.application.port.out.PasswordResetNotificationPort;
import com.softprimesolutions.security.application.port.out.SecurityControlPort;
import com.softprimesolutions.security.application.usecase.command.AsignarRolUsuarioHandler;
import com.softprimesolutions.security.application.usecase.command.CrearRolHandler;
import com.softprimesolutions.security.application.usecase.command.CrearUsuarioHandler;
import com.softprimesolutions.security.application.usecase.command.ReemplazarPermisosRolHandler;
import com.softprimesolutions.security.application.usecase.command.LocalAuthPolicy;
import com.softprimesolutions.security.application.usecase.command.LocalAuthService;
import com.softprimesolutions.security.infrastructure.client.identity.JwtAuthTokenAdapter;
import com.softprimesolutions.security.infrastructure.client.identity.SpringPasswordHashAdapter;
import com.softprimesolutions.security.application.usecase.command.SecurityControlService;
import com.softprimesolutions.security.application.usecase.query.ListarPermisosHandler;
import com.softprimesolutions.security.application.usecase.query.ListarRolesHandler;
import com.softprimesolutions.security.application.usecase.query.ListarUsuariosHandler;
import com.softprimesolutions.shared.application.port.ClockPort;
import com.softprimesolutions.shared.application.port.IdentifierGenerator;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity
public class SecurityModuleConfiguration {

    @Bean
    Clock iamClock() {
        return Clock.systemUTC();
    }

    @Bean
    ClockPort iamClockPort(Clock iamClock) {
        return () -> Instant.now(iamClock);
    }

    @Bean
    IdentifierGenerator iamIdentifierGenerator() {
        return UUID::randomUUID;
    }

    @Bean
    CrearUsuarioUseCase crearUsuarioUseCase(
            IamWritePort writePort, IdentifierGenerator iamIdentifierGenerator, ClockPort iamClockPort) {
        return new CrearUsuarioHandler(writePort, iamIdentifierGenerator, iamClockPort);
    }

    @Bean
    CrearRolUseCase crearRolUseCase(
            IamWritePort writePort, IdentifierGenerator iamIdentifierGenerator, ClockPort iamClockPort) {
        return new CrearRolHandler(writePort, iamIdentifierGenerator, iamClockPort);
    }

    @Bean
    ReemplazarPermisosRolUseCase reemplazarPermisosRolUseCase(IamWritePort writePort, ClockPort iamClockPort) {
        return new ReemplazarPermisosRolHandler(writePort, iamClockPort);
    }

    @Bean
    AsignarRolUsuarioUseCase asignarRolUsuarioUseCase(
            IamWritePort writePort, IdentifierGenerator iamIdentifierGenerator, ClockPort iamClockPort) {
        return new AsignarRolUsuarioHandler(writePort, iamIdentifierGenerator, iamClockPort);
    }

    @Bean
    ListarUsuariosUseCase listarUsuariosUseCase(IamReadPort readPort) {
        return new ListarUsuariosHandler(readPort);
    }

    @Bean
    ListarRolesUseCase listarRolesUseCase(IamReadPort readPort) {
        return new ListarRolesHandler(readPort);
    }

    @Bean
    ListarPermisosUseCase listarPermisosUseCase(IamReadPort readPort) {
        return new ListarPermisosHandler(readPort);
    }

    @Bean
    SecurityControlUseCase securityControlUseCase(
            SecurityControlPort controlPort,
            IamWritePort writePort,
            IdentifierGenerator iamIdentifierGenerator,
            ClockPort iamClockPort) {
        return new SecurityControlService(controlPort, writePort, iamIdentifierGenerator, iamClockPort);
    }

    @Bean
    PasswordHashPort localPasswordHashPort() {
        return new SpringPasswordHashAdapter();
    }

    @Bean
    SecretKey localAuthSecretKey(@Value("${security.local-auth.jwt-secret}") String encodedSecret) {
        if (encodedSecret == null || encodedSecret.isBlank()) {
            throw new IllegalStateException("BOTICA_JWT_SECRET es obligatorio para autenticacion local");
        }
        final byte[] secret;
        try {
            secret = Base64.getDecoder().decode(encodedSecret.trim());
        } catch (IllegalArgumentException invalidBase64) {
            throw new IllegalStateException("BOTICA_JWT_SECRET debe estar codificado en Base64", invalidBase64);
        }
        if (secret.length < 32) {
            throw new IllegalStateException("BOTICA_JWT_SECRET debe contener al menos 256 bits");
        }
        return new SecretKeySpec(secret, "HmacSHA256");
    }

    @Bean
    JwtEncoder localJwtEncoder(SecretKey localAuthSecretKey) {
        return NimbusJwtEncoder.withSecretKey(localAuthSecretKey).algorithm(MacAlgorithm.HS256).build();
    }

    @Bean
    AuthTokenPort localAuthTokenPort(
            JwtEncoder localJwtEncoder,
            @Value("${security.local-auth.issuer}") String issuer,
            @Value("${security.local-auth.audience}") String audience) {
        return new JwtAuthTokenAdapter(localJwtEncoder, issuer, audience);
    }

    @Bean
    LocalAuthPolicy localAuthPolicy(
            @Value("${security.local-auth.access-token-ttl}") Duration accessTokenTtl,
            @Value("${security.local-auth.refresh-token-ttl}") Duration refreshTokenTtl,
            @Value("${security.local-auth.password-reset-ttl}") Duration passwordResetTtl,
            @Value("${security.local-auth.max-failed-attempts}") int maxFailedAttempts,
            @Value("${security.local-auth.lock-duration}") Duration lockDuration) {
        return new LocalAuthPolicy(
                accessTokenTtl, refreshTokenTtl, passwordResetTtl, maxFailedAttempts, lockDuration);
    }

    @Bean
    LocalAuthUseCase localAuthUseCase(
            LocalAuthStorePort store,
            PasswordHashPort passwords,
            AuthTokenPort tokens,
            PasswordResetNotificationPort notifications,
            IdentifierGenerator iamIdentifierGenerator,
            ClockPort iamClockPort,
            LocalAuthPolicy policy) {
        return new LocalAuthService(
                store, passwords, tokens, notifications, iamIdentifierGenerator, iamClockPort, policy);
    }
}
