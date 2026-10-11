package com.softprimesolutions.ventas.domain.model;

import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Map;

final class Failures {

    private Failures() {
    }

    static <T> Result<T, ErrorDetail> failure(String code, String message) {
        return Result.failure(new ErrorDetail(code, message, Map.of()));
    }
}
