package com.softprimesolutions.security.infrastructure.client.identity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.softprimesolutions.security.application.port.out.AuthTokenPort.AccessTokenClaims;
import java.time.Instant;
import java.util.UUID;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

class LocalAuthCryptoAdapterTest {

    private static final UUID TENANT_ID = UUID.fromString("d02084fa-f25d-4b5c-96a4-b1e14775c3a8");
    private static final UUID USER_ID = UUID.fromString("5bf09010-0b4e-4496-a7e1-c742e8f8a0c2");
    private static final UUID SESSION_ID = UUID.fromString("b40fcbfa-9772-470c-b3dd-1585a67f85cb");

    @Test
    void hashesPasswordsWithSaltAndVerifiesOnlyTheCorrectSecret() {
        var adapter = new SpringPasswordHashAdapter();

        var first = adapter.encode("StrongPass!2026");
        var second = adapter.encode("StrongPass!2026");

        assertTrue(first.startsWith("{pbkdf2@SpringSecurity_v5_8}"));
        assertNotEquals(first, second);
        assertTrue(adapter.matches("StrongPass!2026", first));
        assertFalse(adapter.matches("WrongPass!2026", first));
    }

    @Test
    void issuesSignedHs256JwtWithTheRequiredLocalClaims() {
        var key = new SecretKeySpec(new byte[32], "HmacSHA256");
        var encoder = NimbusJwtEncoder.withSecretKey(key).algorithm(MacAlgorithm.HS256).build();
        var decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        var adapter = new JwtAuthTokenAdapter(encoder, "service-botica-test", "botica-api-test");
        var issuedAt = Instant.now();

        var token = adapter.issueAccessToken(new AccessTokenClaims(
                TENANT_ID, USER_ID, SESSION_ID, issuedAt, issuedAt.plusSeconds(300), true));
        var jwt = decoder.decode(token);

        assertEquals("service-botica-test", jwt.getClaimAsString("iss"));
        assertEquals(USER_ID.toString(), jwt.getSubject());
        assertEquals(java.util.List.of("botica-api-test"), jwt.getAudience());
        assertEquals(TENANT_ID.toString(), jwt.getClaimAsString("tid"));
        assertEquals(SESSION_ID.toString(), jwt.getClaimAsString("sid"));
        assertEquals("PASSWORD", jwt.getClaimAsString("auth_method"));
        assertTrue(jwt.getClaimAsBoolean("pwd_change_required"));
        assertEquals("JWT", jwt.getHeaders().get("typ"));
    }

    @Test
    void rejectsAnExpiredJwtAndGeneratesStrongOpaqueTokensAndDigests() {
        var key = new SecretKeySpec(new byte[32], "HmacSHA256");
        var encoder = NimbusJwtEncoder.withSecretKey(key).algorithm(MacAlgorithm.HS256).build();
        var decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        var adapter = new JwtAuthTokenAdapter(encoder, "service-botica-test", "botica-api-test");
        var issuedAt = Instant.now().minusSeconds(300);
        var expired = adapter.issueAccessToken(new AccessTokenClaims(
                TENANT_ID, USER_ID, SESSION_ID, issuedAt, issuedAt.plusSeconds(60), false));

        assertThrows(JwtException.class, () -> decoder.decode(expired));
        var first = adapter.newOpaqueToken();
        var second = adapter.newOpaqueToken();
        assertTrue(first.matches("[A-Za-z0-9_-]{43}"));
        assertNotEquals(first, second);
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
                adapter.digest("abc"));
    }
}
