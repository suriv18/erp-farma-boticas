package com.softprimesolutions.security.infrastructure.configuration;

import com.softprimesolutions.security.application.port.out.LocalAuthStorePort;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

final class SessionAwareJwtDecoder implements JwtDecoder {

    private final JwtDecoder delegate;
    private final LocalAuthStorePort store;
    private final Clock clock;

    SessionAwareJwtDecoder(JwtDecoder delegate, LocalAuthStorePort store, Clock clock) {
        this.delegate = delegate;
        this.store = store;
        this.clock = clock;
    }

    @Override
    public Jwt decode(String token) {
        var jwt = delegate.decode(token);
        try {
            var tenantId = UUID.fromString(jwt.getClaimAsString("tid"));
            var userId = UUID.fromString(jwt.getSubject());
            var sessionId = UUID.fromString(jwt.getClaimAsString("sid"));
            if (!store.isSessionActive(tenantId, userId, sessionId, Instant.now(clock))) {
                throw new BadJwtException("La sesion local no se encuentra activa");
            }
            return jwt;
        } catch (IllegalArgumentException | NullPointerException invalidClaims) {
            throw new BadJwtException("Los claims locales del JWT no son validos", invalidClaims);
        }
    }
}
