package com.softprimesolutions.catalogo.application.port.in;

import com.softprimesolutions.catalogo.application.dto.command.CrearSkuCommand;
import com.softprimesolutions.catalogo.application.dto.result.SkuResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

@FunctionalInterface
public interface CrearSkuUseCase {
    Result<SkuResult, ApplicationError> execute(CrearSkuCommand command);
}
