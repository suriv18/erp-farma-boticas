package com.softprimesolutions.shared.web.error;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ApplicationErrorHttpMapperTest {

    @Test
    void mapsExpectedErrorWithoutRawMetadata() {
        var error = new StandardApplicationError(
                "INV_STOCK_INSUFICIENTE",
                "No existe stock vendible suficiente.",
                ErrorCategory.CONFLICT,
                Map.of("sql", "dato interno"));

        var problem = ApplicationErrorHttpMapper.toProblemDetail(error);

        assertEquals(409, problem.getStatus());
        assertEquals("No existe stock vendible suficiente.", problem.getDetail());
        assertEquals("INV_STOCK_INSUFICIENTE", problem.getProperties().get("code"));
        assertFalse(problem.getProperties().containsKey("metadata"));
    }

    @Test
    void hidesUnexpectedErrorDetails() {
        var error = new StandardApplicationError(
                "UNEXPECTED",
                "password=secreto; SQLSTATE=08006",
                ErrorCategory.UNEXPECTED,
                Map.of("stackTrace", "interno"));

        var problem = ApplicationErrorHttpMapper.toProblemDetail(error);

        assertEquals(500, problem.getStatus());
        assertEquals("Ocurrió un error inesperado.", problem.getDetail());
        assertEquals("UNEXPECTED", problem.getProperties().get("code"));
        assertFalse(problem.getProperties().containsKey("metadata"));
    }
}
