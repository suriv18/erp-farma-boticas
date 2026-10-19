package com.softprimesolutions.inventario.application.port.in;

import com.softprimesolutions.inventario.application.dto.command.DesbloquearLoteCommand;
import com.softprimesolutions.inventario.application.dto.result.LoteResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

@FunctionalInterface
public interface DesbloquearLoteUseCase {
    Result<LoteResult, ApplicationError> execute(DesbloquearLoteCommand command);
}
