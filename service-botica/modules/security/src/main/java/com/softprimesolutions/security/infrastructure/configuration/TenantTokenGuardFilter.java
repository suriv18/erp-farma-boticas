package com.softprimesolutions.security.infrastructure.configuration;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

public final class TenantTokenGuardFilter extends OncePerRequestFilter {

    private static final String BODY = """
            {"status":403,"title":"Forbidden","detail":"El tenant de la petición no coincide con el de la sesión.","code":"SEC_TENANT_NO_COINCIDE"}""";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var requested = request.getParameter("tenantId");
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (requested != null && authentication instanceof JwtAuthenticationToken token
                && !requested.equalsIgnoreCase(token.getToken().getClaimAsString("tid"))) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(BODY);
            return;
        }
        chain.doFilter(request, response);
    }
}
