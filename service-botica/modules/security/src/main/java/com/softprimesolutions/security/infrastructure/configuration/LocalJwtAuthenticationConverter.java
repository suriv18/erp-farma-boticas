package com.softprimesolutions.security.infrastructure.configuration;

import com.softprimesolutions.security.application.port.out.LocalAuthStorePort;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

final class LocalJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final LocalAuthStorePort store;
    private final Clock clock;

    LocalJwtAuthenticationConverter(LocalAuthStorePort store, Clock clock) {
        this.store = store;
        this.clock = clock;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        var userId = UUID.fromString(jwt.getSubject());
        if (Boolean.TRUE.equals(jwt.getClaimAsBoolean("pwd_change_required"))) {
            return new JwtAuthenticationToken(
                    jwt, List.of(new SimpleGrantedAuthority("AUTH_PASSWORD_CHANGE")), userId.toString());
        }
        var tenantId = UUID.fromString(jwt.getClaimAsString("tid"));
        var authorities = store.findEffectivePermissions(tenantId, userId, Instant.now(clock)).stream()
                .map(SimpleGrantedAuthority::new)
                .toList();
        return new JwtAuthenticationToken(jwt, authorities, userId.toString());
    }
}
