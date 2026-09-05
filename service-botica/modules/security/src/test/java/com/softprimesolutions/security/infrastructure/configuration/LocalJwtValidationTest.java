package com.softprimesolutions.security.infrastructure.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.softprimesolutions.security.application.port.out.LocalAuthStorePort;
import java.lang.reflect.Proxy;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

class LocalJwtValidationTest {

    private static final UUID TENANT_ID = UUID.fromString("8aa22ce8-d66a-46c6-b84c-941a9c86a4ec");
    private static final UUID USER_ID = UUID.fromString("74ec7cc0-a9dd-44bc-b5e5-a4f151899caf");
    private static final UUID SESSION_ID = UUID.fromString("7fd80a8c-3ec1-4e59-9352-49c161263ade");
    private static final Instant NOW = Instant.parse("2026-09-03T20:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Test
    void requiresAValidBase64SecretOfAtLeast256Bits() {
        var configuration = new SecurityModuleConfiguration();

        assertThrows(IllegalStateException.class, () -> configuration.localAuthSecretKey(null));
        assertThrows(IllegalStateException.class, () -> configuration.localAuthSecretKey(" "));
        assertThrows(IllegalStateException.class, () -> configuration.localAuthSecretKey("not-base64!"));
        assertThrows(IllegalStateException.class, () -> configuration.localAuthSecretKey(
                Base64.getEncoder().encodeToString(new byte[31])));

        var encoded = Base64.getEncoder().encodeToString(new byte[32]);
        var key = configuration.localAuthSecretKey("  " + encoded + "  ");
        assertEquals("HmacSHA256", key.getAlgorithm());
        assertTrue(key.getEncoded().length >= 32);
    }

    @Test
    void acceptsOnlyJwtWhoseLocalSessionIsStillActive() {
        var jwt = jwt(false);
        JwtDecoder delegate = token -> jwt;

        var activeDecoder = new SessionAwareJwtDecoder(delegate, store(true, Set.of()), CLOCK);
        assertSame(jwt, activeDecoder.decode("signed-jwt"));

        var revokedDecoder = new SessionAwareJwtDecoder(delegate, store(false, Set.of()), CLOCK);
        assertThrows(BadJwtException.class, () -> revokedDecoder.decode("signed-jwt"));
    }

    @Test
    void rejectsMissingOrMalformedLocalClaims() {
        var malformed = Jwt.withTokenValue("signed-jwt")
                .header("alg", "HS256")
                .claim("sub", "not-a-uuid")
                .claim("tid", TENANT_ID.toString())
                .claim("sid", SESSION_ID.toString())
                .issuedAt(NOW)
                .expiresAt(NOW.plusSeconds(300))
                .build();
        var decoder = new SessionAwareJwtDecoder(token -> malformed, store(true, Set.of()), CLOCK);

        assertThrows(BadJwtException.class, () -> decoder.decode("signed-jwt"));
    }

    @Test
    void resolvesCurrentDatabasePermissionsInsteadOfTrustingJwtAuthorities() {
        var converter = new LocalJwtAuthenticationConverter(
                store(true, Set.of("seguridad.usuarios.consultar", "seguridad.roles.gestionar")), CLOCK);

        var authentication = converter.convert(jwt(false));

        assertEquals(USER_ID.toString(), authentication.getName());
        assertEquals(Set.of("seguridad.usuarios.consultar", "seguridad.roles.gestionar"),
                authentication.getAuthorities().stream().map(value -> value.getAuthority())
                        .collect(java.util.stream.Collectors.toSet()));
    }

    @Test
    void restrictsPasswordChangeJwtToItsSingleRecoveryAuthority() {
        var converter = new LocalJwtAuthenticationConverter(
                store(true, Set.of("seguridad.usuarios.gestionar")), CLOCK);

        var authentication = converter.convert(jwt(true));

        assertEquals(1, authentication.getAuthorities().size());
        assertEquals("AUTH_PASSWORD_CHANGE", authentication.getAuthorities().iterator().next().getAuthority());
    }

    private static Jwt jwt(boolean passwordChangeRequired) {
        return Jwt.withTokenValue("signed-jwt")
                .header("alg", "HS256")
                .claim("sub", USER_ID.toString())
                .claim("tid", TENANT_ID.toString())
                .claim("sid", SESSION_ID.toString())
                .claim("pwd_change_required", passwordChangeRequired)
                .issuedAt(NOW)
                .expiresAt(NOW.plusSeconds(300))
                .build();
    }

    private static LocalAuthStorePort store(boolean sessionActive, Set<String> permissions) {
        return (LocalAuthStorePort) Proxy.newProxyInstance(
                LocalAuthStorePort.class.getClassLoader(),
                new Class<?>[]{LocalAuthStorePort.class},
                (proxy, method, arguments) -> switch (method.getName()) {
                    case "isSessionActive" -> sessionActive;
                    case "findEffectivePermissions" -> permissions;
                    case "toString" -> "LocalAuthStorePortTestDouble";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == arguments[0];
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }
}
