package com.softprimesolutions.catalogo.api.controller;

import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.web.error.ApplicationErrorHttpMapper;
import java.util.UUID;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

final class CatalogoControllerSupport {

    private static final String TENANT_CLAIM = "tid";

    private CatalogoControllerSupport() {
    }

    static UUID tenantOf(Jwt jwt) {
        return UUID.fromString(jwt.getClaimAsString(TENANT_CLAIM));
    }

    static String actorOf(Jwt jwt) {
        return jwt.getSubject();
    }

    static ResponseEntity<ProblemDetail> problem(ApplicationError error) {
        var problem = ApplicationErrorHttpMapper.toProblemDetail(error);
        return ResponseEntity.status(problem.getStatus()).body(problem);
    }
}
