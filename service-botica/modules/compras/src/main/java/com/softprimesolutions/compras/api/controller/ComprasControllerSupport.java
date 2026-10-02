package com.softprimesolutions.compras.api.controller;

import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.web.error.ApplicationErrorHttpMapper;
import java.util.UUID;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

final class ComprasControllerSupport {

    private static final String TENANT_CLAIM = "tid";

    private ComprasControllerSupport() {
    }

    static ResponseEntity<ProblemDetail> problem(ApplicationError error) {
        var problem = ApplicationErrorHttpMapper.toProblemDetail(error);
        return ResponseEntity.status(problem.getStatus()).body(problem);
    }

    static UUID tenantOf(Jwt jwt) {
        return UUID.fromString(jwt.getClaimAsString(TENANT_CLAIM));
    }

    static UUID actorOf(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
