package com.softprimesolutions.catalogo.api.controller;

import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.web.error.ApplicationErrorHttpMapper;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

final class CatalogoControllerSupport {

    private CatalogoControllerSupport() {
    }

    static ResponseEntity<ProblemDetail> problem(ApplicationError error) {
        var problem = ApplicationErrorHttpMapper.toProblemDetail(error);
        return ResponseEntity.status(problem.getStatus()).body(problem);
    }
}
