package com.softprimesolutions.ventas.application.port.in;

import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import com.softprimesolutions.ventas.application.dto.command.RegistrarVentaCommand;
import com.softprimesolutions.ventas.application.dto.result.VentaResult;

@FunctionalInterface
public interface RegistrarVentaUseCase {
    Result<VentaResult, ApplicationError> execute(RegistrarVentaCommand command);
}
