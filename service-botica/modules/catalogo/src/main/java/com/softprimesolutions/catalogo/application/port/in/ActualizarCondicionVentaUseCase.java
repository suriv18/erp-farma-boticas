package com.softprimesolutions.catalogo.application.port.in;

import com.softprimesolutions.catalogo.application.dto.command.ActualizarCondicionVentaCommand;
import com.softprimesolutions.catalogo.application.dto.result.CondicionVentaResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

@FunctionalInterface
public interface ActualizarCondicionVentaUseCase {
    Result<CondicionVentaResult, ApplicationError> execute(ActualizarCondicionVentaCommand command);
}
