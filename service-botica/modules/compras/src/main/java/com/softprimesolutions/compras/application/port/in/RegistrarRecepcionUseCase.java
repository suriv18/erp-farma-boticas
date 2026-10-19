package com.softprimesolutions.compras.application.port.in;

import com.softprimesolutions.compras.application.dto.command.RegistrarRecepcionCommand;
import com.softprimesolutions.compras.application.dto.result.RecepcionResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

@FunctionalInterface
public interface RegistrarRecepcionUseCase {
    Result<RecepcionResult, ApplicationError> execute(RegistrarRecepcionCommand command);
}
