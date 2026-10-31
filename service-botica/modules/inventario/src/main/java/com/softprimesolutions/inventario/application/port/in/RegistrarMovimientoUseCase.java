package com.softprimesolutions.inventario.application.port.in;

import com.softprimesolutions.inventario.application.dto.command.RegistrarMovimientoCommand;
import com.softprimesolutions.inventario.application.dto.result.MovimientoResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

@FunctionalInterface
public interface RegistrarMovimientoUseCase {
    Result<MovimientoResult, ApplicationError> execute(RegistrarMovimientoCommand command);
}
