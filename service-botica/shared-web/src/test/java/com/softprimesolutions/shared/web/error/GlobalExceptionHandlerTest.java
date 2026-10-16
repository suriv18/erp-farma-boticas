package com.softprimesolutions.shared.web.error;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;

import jakarta.validation.ConstraintViolationException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.MethodValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @SuppressWarnings("unused")
    void target(String value) {
    }

    private MethodArgumentNotValidException bodyValidation(FieldError... errors) throws NoSuchMethodException {
        Method method = GlobalExceptionHandlerTest.class.getDeclaredMethod("target", String.class);
        var binding = new BeanPropertyBindingResult(new Object(), "request");
        for (var error : errors) binding.addError(error);
        return new MethodArgumentNotValidException(new MethodParameter(method, 0), binding);
    }

    @Test
    void mapsBodyValidationToABadRequestListingEachFieldError() throws Exception {
        var exception = bodyValidation(
                new FieldError("request", "nombre", null, false, null, null, "El nombre es obligatorio."),
                new FieldError("request", "codigo", null, false, null, null, null));

        var response = handler.handleBodyValidation(exception);

        assertEquals(400, response.getStatusCode().value());
        assertEquals(MediaType.APPLICATION_PROBLEM_JSON, response.getHeaders().getContentType());
        var problem = response.getBody();
        assertEquals("REQUEST_VALIDATION_FAILED", problem.getProperties().get("code"));
        assertEquals(
                List.of(
                        Map.of("field", "nombre", "message", "El nombre es obligatorio."),
                        Map.of("field", "codigo", "message", "Valor inválido.")),
                problem.getProperties().get("errors"));
    }

    @Test
    void mapsMethodParameterValidationToABadRequest() {
        var exception = new HandlerMethodValidationException(mock(MethodValidationResult.class));

        assertParameterInvalid(handler.handleParameterValidation(exception).getBody());
    }

    @Test
    void mapsConstraintViolationsOfMethodValidationToABadRequest() {
        var exception = new ConstraintViolationException("list.size: must be less than or equal to 100", Set.of());

        var response = handler.handleParameterValidation(exception);

        assertEquals(400, response.getStatusCode().value());
        assertEquals(MediaType.APPLICATION_PROBLEM_JSON, response.getHeaders().getContentType());
        assertParameterInvalid(response.getBody());
    }

    @Test
    void doesNotLeakTheConstraintDetailsToTheClient() {
        var exception = new ConstraintViolationException("list.size: valor interno", Set.of());

        var problem = handler.handleParameterValidation(exception).getBody();

        assertFalse(problem.getDetail().contains("list.size"));
    }

    @Test
    void mapsAnUnreadableBodyToABadRequest() {
        var exception = new HttpMessageNotReadableException("JSON inválido", mock(HttpInputMessage.class));

        var problem = handler.handleUnreadableBody(exception).getBody();

        assertEquals(400, problem.getStatus());
        assertEquals("REQUEST_BODY_INVALID", problem.getProperties().get("code"));
    }

    private static void assertParameterInvalid(ProblemDetail problem) {
        assertEquals(400, problem.getStatus());
        assertEquals("REQUEST_PARAMETER_INVALID", problem.getProperties().get("code"));
        assertEquals("Uno o más parámetros de la solicitud no son válidos.", problem.getDetail());
    }
}
