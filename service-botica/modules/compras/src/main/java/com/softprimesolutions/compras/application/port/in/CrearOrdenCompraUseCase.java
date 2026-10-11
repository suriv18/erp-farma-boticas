package com.softprimesolutions.compras.application.port.in;

import com.softprimesolutions.compras.application.dto.command.CrearOrdenCompraCommand;
import com.softprimesolutions.compras.application.dto.result.OrdenCompraResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

@FunctionalInterface
public interface CrearOrdenCompraUseCase {
    Result<OrdenCompraResult, ApplicationError> execute(CrearOrdenCompraCommand command);
}
