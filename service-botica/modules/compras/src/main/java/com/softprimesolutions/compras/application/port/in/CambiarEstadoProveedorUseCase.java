package com.softprimesolutions.compras.application.port.in;

import com.softprimesolutions.compras.application.dto.command.CambiarEstadoProveedorCommand;
import com.softprimesolutions.compras.application.dto.result.ProveedorResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

@FunctionalInterface
public interface CambiarEstadoProveedorUseCase {
    Result<ProveedorResult, ApplicationError> execute(CambiarEstadoProveedorCommand command);
}
