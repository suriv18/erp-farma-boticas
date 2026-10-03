package com.softprimesolutions.ventas.application.error;

import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.ventas.domain.exception.VentasErrorCodes;
import java.util.Set;

public final class VentasErrors {

    public static final String CONCURRENCIA = "VEN_MODIFICACION_CONCURRENTE";

    private static final Set<String> CONFLICTOS = Set.of(VentasErrorCodes.TURNO_ESTADO_INVALIDO);

    private VentasErrors() {
    }

    public static ApplicationError fromDomain(ErrorDetail error) {
        var category = CONFLICTOS.contains(error.code()) ? ErrorCategory.CONFLICT : ErrorCategory.VALIDATION;
        return new StandardApplicationError(error.code(), error.message(), category, error.metadata());
    }

    public static ApplicationError turnoNoEncontrado() {
        return notFound("VEN_TURNO_NO_ENCONTRADO", "El turno de caja indicado no existe.");
    }

    public static ApplicationError terminalNoEncontrada() {
        return notFound("VEN_TERMINAL_NO_ENCONTRADA", "La terminal indicada no existe.");
    }

    public static ApplicationError terminalNoOperable() {
        return conflict("VEN_TERMINAL_NO_OPERABLE", "La terminal debe estar activa para operar.");
    }

    public static ApplicationError turnoYaAbierto() {
        return conflict("VEN_TURNO_YA_ABIERTO", "La terminal ya tiene un turno abierto.");
    }

    public static ApplicationError modificacionConcurrente() {
        return conflict(CONCURRENCIA, "La operacion fue modificada por otra solicitud; reintenta.");
    }

    private static ApplicationError notFound(String code, String message) {
        return new StandardApplicationError(code, message, ErrorCategory.NOT_FOUND);
    }

    private static ApplicationError conflict(String code, String message) {
        return new StandardApplicationError(code, message, ErrorCategory.CONFLICT);
    }
}
