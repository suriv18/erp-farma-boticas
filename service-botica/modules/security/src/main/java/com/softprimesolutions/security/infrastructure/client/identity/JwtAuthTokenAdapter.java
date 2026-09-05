package com.softprimesolutions.security.infrastructure.client.identity;

import com.softprimesolutions.security.application.port.out.AuthTokenPort;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;

public final class JwtAuthTokenAdapter implements AuthTokenPort {

    private final JwtEncoder encoder;
    private final String issuer;
    private final String audience;
    private final SecureRandom random = new SecureRandom();

    public JwtAuthTokenAdapter(JwtEncoder encoder, String issuer, String audience) {
        this.encoder = encoder;
        this.issuer = issuer;
        this.audience = audience;
    }

    @Override
    public String issueAccessToken(AccessTokenClaims value) {
        var claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(value.userId().toString())
                .audience(java.util.List.of(audience))
                .issuedAt(value.issuedAt())
                .notBefore(value.issuedAt())
                .expiresAt(value.expiresAt())
                .id(UUID.randomUUID().toString())
                .claim("tid", value.tenantId().toString())
                .claim("sid", value.sessionId().toString())
                .claim("auth_method", "PASSWORD")
                .claim("pwd_change_required", value.passwordChangeRequired())
                .build();
        var header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    @Override
    public String newOpaqueToken() {
        var bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    @Override
    public String digest(String rawToken) {
        try {
            var digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 no esta disponible", impossible);
        }
    }
}
