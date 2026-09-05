package com.softprimesolutions.shared.web.error;

import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import java.net.URI;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

/** Traduce errores de Application a RFC 9457 sin exponer detalles técnicos. */
public final class ApplicationErrorHttpMapper {

    private static final String UNEXPECTED_DETAIL = "Ocurrió un error inesperado.";

    private ApplicationErrorHttpMapper() {
    }

    public static ProblemDetail toProblemDetail(ApplicationError error) {
        Objects.requireNonNull(error, "error es obligatorio");
        var status = switch (error.category()) {
            case VALIDATION -> HttpStatus.BAD_REQUEST;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case CONFLICT -> HttpStatus.CONFLICT;
            case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
            case UNEXPECTED -> HttpStatus.INTERNAL_SERVER_ERROR;
        };

        var detail = error.category() == ErrorCategory.UNEXPECTED
                ? UNEXPECTED_DETAIL
                : error.message();
        var problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(status.getReasonPhrase());
        problem.setType(URI.create("urn:erp-botica:error:" + error.code()));
        problem.setProperty("code", error.code());
        return problem;
    }
}
