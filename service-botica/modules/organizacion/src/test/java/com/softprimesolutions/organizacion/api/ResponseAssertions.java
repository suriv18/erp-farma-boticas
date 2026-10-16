package com.softprimesolutions.organizacion.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

public final class ResponseAssertions {

    private ResponseAssertions() {
    }

    public static <T> Result<T, ApplicationError> ok(T value) {
        return Result.success(value);
    }

    public static <T> Result<T, ApplicationError> conflict() {
        return Result.failure(OrganizacionApiFixtures.CONFLICT);
    }

    public static void assertCreated(ResponseEntity<?> response, String basePath, UUID id, Object body) {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getHeaders().getFirst(HttpHeaders.LOCATION)).isEqualTo(basePath + "/" + id);
        assertThat(response.getBody()).usingRecursiveComparison().isEqualTo(body);
    }

    public static void assertOk(ResponseEntity<?> response, Object body) {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).usingRecursiveComparison().isEqualTo(body);
    }

    public static void assertConflict(ResponseEntity<?> response) {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isInstanceOf(ProblemDetail.class);
    }
}
