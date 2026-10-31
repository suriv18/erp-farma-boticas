package com.softprimesolutions.inventario.application.port.in;

import com.softprimesolutions.inventario.application.dto.command.BloquearLoteCommand;
import com.softprimesolutions.inventario.application.dto.result.LoteResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

@FunctionalInterface
public interface BloquearLoteUseCase {
    Result<LoteResult, ApplicationError> execute(BloquearLoteCommand command);
}
