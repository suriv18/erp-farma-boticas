package com.softprimesolutions.security.infrastructure.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.servlet.FilterChain;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class TenantTokenGuardFilterTest {

    private static final String TENANT = "7c61d383-861c-4c6b-9749-9096ce4a74cf";
    private static final String OTHER_TENANT = "172e0f26-a765-46f3-841c-4a11407ccf5b";

    private final TenantTokenGuardFilter filter = new TenantTokenGuardFilter();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private static void authenticateWithTenant(String tenantId) {
        var jwt = Jwt.withTokenValue("token").header("alg", "HS256").claim("tid", tenantId).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
    }

    private static FilterChain flag(AtomicBoolean called) {
        return (request, response) -> called.set(true);
    }

    @Test
    void letsTheRequestThroughWhenTheTenantParameterMatchesTheToken() throws Exception {
        authenticateWithTenant(TENANT);
        var request = new MockHttpServletRequest();
        request.setParameter("tenantId", TENANT.toUpperCase());
        var called = new AtomicBoolean();

        filter.doFilter(request, new MockHttpServletResponse(), flag(called));

        assertTrue(called.get());
    }

    @Test
    void rejectsTheRequestWhenTheTenantParameterDiffersFromTheToken() throws Exception {
        authenticateWithTenant(TENANT);
        var request = new MockHttpServletRequest();
        request.setParameter("tenantId", OTHER_TENANT);
        var response = new MockHttpServletResponse();
        var called = new AtomicBoolean();

        filter.doFilter(request, response, flag(called));

        assertEquals(403, response.getStatus());
        assertTrue(response.getContentType().startsWith("application/problem+json"));
        assertTrue(response.getContentAsString().contains("SEC_TENANT_NO_COINCIDE"));
        assertFalse(called.get());
    }

    @Test
    void letsTheRequestThroughWhenThereIsNoTenantParameter() throws Exception {
        authenticateWithTenant(TENANT);
        var called = new AtomicBoolean();

        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), flag(called));

        assertTrue(called.get());
    }

    @Test
    void letsTheRequestThroughWhenTheCallerIsNotAuthenticatedWithAJwt() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.unauthenticated("anonimo", "n/a"));
        var request = new MockHttpServletRequest();
        request.setParameter("tenantId", OTHER_TENANT);
        var called = new AtomicBoolean();

        filter.doFilter(request, new MockHttpServletResponse(), flag(called));

        assertTrue(called.get());
    }
}
