package com.softprimesolutions.organizacion.application.usecase.command;

import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;

final class EstadoErrors {

    private EstadoErrors() {
    }

    static <T> Result<T, ApplicationError> validationFailure(ErrorDetail error) {
        return Result.failure(new StandardApplicationError(
                error.code(), error.message(), ErrorCategory.VALIDATION, error.metadata()));
    }

    static <T, E extends Enum<E>> Result<T, ApplicationError> invalidEstado(String code, Class<E> type) {
        return Result.failure(new StandardApplicationError(
                code,
                "El estado indicado no es válido. Valores permitidos: " + EnumParser.allowedValues(type) + ".",
                ErrorCategory.VALIDATION));
    }
}
