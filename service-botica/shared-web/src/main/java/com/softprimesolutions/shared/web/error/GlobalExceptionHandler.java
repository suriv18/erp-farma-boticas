package com.softprimesolutions.shared.web.error;

import java.net.URI;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

/** Traduce fallos del protocolo HTTP; los errores esperables de aplicación conservan su Result. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleBodyValidation(MethodArgumentNotValidException exception) {
        var errors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> Map.of(
                        "field", error.getField(),
                        "message", error.getDefaultMessage() == null ? "Valor inválido." : error.getDefaultMessage()))
                .toList();
        var problem = badRequest(
                "REQUEST_VALIDATION_FAILED", "La solicitud contiene datos inválidos.");
        problem.setProperty("errors", errors);
        return response(problem);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ProblemDetail> handleParameterValidation(HandlerMethodValidationException exception) {
        return response(badRequest(
                "REQUEST_PARAMETER_INVALID", "Uno o más parámetros de la solicitud no son válidos."));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleUnreadableBody(HttpMessageNotReadableException exception) {
        return response(badRequest(
                "REQUEST_BODY_INVALID", "El cuerpo de la solicitud no contiene JSON válido."));
    }

    private static ProblemDetail badRequest(String code, String detail) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problem.setTitle(HttpStatus.BAD_REQUEST.getReasonPhrase());
        problem.setType(URI.create("urn:erp-botica:error:" + code));
        problem.setProperty("code", code);
        return problem;
    }

    private static ResponseEntity<ProblemDetail> response(ProblemDetail problem) {
        return ResponseEntity.status(problem.getStatus())
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }
}
