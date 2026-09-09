package com.softprimesolutions.catalogo.application.port.in;

import com.softprimesolutions.catalogo.application.dto.query.ConsultarSkuQuery;
import com.softprimesolutions.catalogo.application.dto.result.SkuResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

@FunctionalInterface
public interface ConsultarSkuUseCase {
    Result<SkuResult, ApplicationError> execute(ConsultarSkuQuery query);
}
