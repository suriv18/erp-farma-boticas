package com.softprimesolutions.compras.application.port.in;

import com.softprimesolutions.compras.application.dto.command.ActualizarProveedorCommand;
import com.softprimesolutions.compras.application.dto.result.ProveedorResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

@FunctionalInterface
public interface ActualizarProveedorUseCase {
    Result<ProveedorResult, ApplicationError> execute(ActualizarProveedorCommand command);
}
