package com.softprimesolutions.security.api.controller;

import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.web.error.ApplicationErrorHttpMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

final class IamControllerSupport {

    private IamControllerSupport() {
    }

    static ResponseEntity<ProblemDetail> problem(ApplicationError error) {
        var problem = ApplicationErrorHttpMapper.toProblemDetail(error);
        return ResponseEntity.status(problem.getStatus()).body(problem);
    }

    static ResponseEntity<?> guardTenant(Jwt jwt, UUID requestedTenantId, Supplier<ResponseEntity<?>> action) {
        if (jwt == null || tenantId(jwt).equals(requestedTenantId)) {
            return action.get();
        }
        var problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN, "El tenant de la petición no coincide con el de la sesión.");
        problem.setProperty("code", "SEC_TENANT_NO_COINCIDE");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problem);
    }

    static UUID tenantId(Jwt jwt) {
        return UUID.fromString(jwt.getClaimAsString("tid"));
    }

    static UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }

    static UUID sessionId(Jwt jwt) {
        return UUID.fromString(jwt.getClaimAsString("sid"));
    }
}
