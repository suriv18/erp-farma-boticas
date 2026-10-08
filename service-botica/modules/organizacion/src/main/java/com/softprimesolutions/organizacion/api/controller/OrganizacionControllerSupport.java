package com.softprimesolutions.organizacion.api.controller;

import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.web.error.ApplicationErrorHttpMapper;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

final class OrganizacionControllerSupport {

    private static final String TENANT_CLAIM = "tid";

    private OrganizacionControllerSupport() {
    }

    static ResponseEntity<ProblemDetail> problem(ApplicationError error) {
        var problem = ApplicationErrorHttpMapper.toProblemDetail(error);
        return ResponseEntity.status(problem.getStatus()).body(problem);
    }

    static ResponseEntity<Object> created(String basePath, UUID id, Object body) {
        return ResponseEntity.created(URI.create(basePath + "/" + id)).body(body);
    }

    static UUID tenantOf(Jwt jwt) {
        return UUID.fromString(jwt.getClaimAsString(TENANT_CLAIM));
    }
}
